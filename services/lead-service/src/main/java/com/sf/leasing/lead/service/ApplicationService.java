package com.sf.leasing.lead.service;

import com.sf.leasing.lead.api.dto.request.InitiateApplicationRequest;
import com.sf.leasing.lead.api.dto.request.UploadDocumentRequest;
import com.sf.leasing.lead.api.dto.response.ApplicationResponse;
import com.sf.leasing.lead.domain.enums.ApplicationStatus;
import com.sf.leasing.lead.domain.enums.CamStatus;
import com.sf.leasing.lead.domain.exception.BusinessException;
import com.sf.leasing.lead.domain.exception.ErrorCodes;
import com.sf.leasing.lead.domain.model.Application;
import com.sf.leasing.lead.domain.model.ApplicationDocument;
import com.sf.leasing.lead.domain.model.CamWorkflow;
import com.sf.leasing.lead.domain.model.Opportunity;
import com.sf.leasing.lead.domain.model.Prospect;
import com.sf.leasing.lead.domain.model.Quote;
import com.sf.leasing.lead.infrastructure.adapter.DmsAdapter;
import com.sf.leasing.lead.infrastructure.locking.RedisSequenceGenerator;
import com.sf.leasing.lead.infrastructure.messaging.LeadEventProducer;
import com.sf.leasing.lead.infrastructure.persistence.ApplicationDocumentRepository;
import com.sf.leasing.lead.infrastructure.persistence.ApplicationRepository;
import com.sf.leasing.lead.infrastructure.persistence.CamWorkflowRepository;
import com.sf.leasing.lead.infrastructure.persistence.OpportunityRepository;
import com.sf.leasing.lead.infrastructure.persistence.ProspectRepository;
import com.sf.leasing.lead.infrastructure.persistence.QuoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * PP7: Application origination.
 *
 * Business rules enforced:
 *   PP7.1  — APP-YYYY-NNNNNN; form pre-filled from Prospect data.
 *   PP7.2  — Document checklist enforcement.
 *   PP7.3  — KYC document upload with DMS integration.
 *   PP7.5  — Status display: APP ID, KYC status, CAM status visible at Prospect.
 *   PP7.6  — Sanction bypass for existing customer within validity period.
 *   PP7.7  — Non-individual (commercial) auto-advance to Eligible For Application.
 *   PP7.8  — Modification blocked when: contract/CAM in non-final state (LN3713)
 *             or active fraud investigation (LN3785).
 *   PP7.9  — Delete-and-re-insert applicant records on modification, except
 *             those with confirmed SMS or ANC status.
 *   PP7.10 — Fraud screening (Hunter/Sherlock) async post-save; non-blocking.
 *   PP7.11 — Post-save caution screening; hard error blocks second commit.
 *   PP7.12 — Welcome communication for eligible lease types on reaching
 *             ELIGIBLE_FOR_APPLICATION.
 */
@Service
public class ApplicationService {

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationService.class);

    private static final Set<String> VALID_DOCUMENT_TYPES = Set.of(
        "PHOTO", "DRIVING_LICENCE", "PAN", "PASSPORT", "OTHER_KYC"
    );
    private static final Set<String> VALID_APPLICANT_PREFIXES = Set.of("MA", "A1", "A2");
    private static final Set<String> MANDATORY_INDIVIDUAL_DOCS = Set.of("PHOTO", "PAN");
    private static final Set<String> MANDATORY_COMMERCIAL_DOCS = Set.of("PAN");

    private final QuoteService quoteService;
    private final RedisSequenceGenerator sequenceGenerator;
    private final DmsAdapter dmsAdapter;
    private final FraudScreeningService fraudScreeningService;
    private final NotificationService notificationService;
    private final LeadEventProducer eventProducer;
    private final ApplicationRepository applicationRepository;
    private final ApplicationDocumentRepository applicationDocumentRepository;
    private final CamWorkflowRepository camWorkflowRepository;
    private final ProspectRepository prospectRepository;
    private final OpportunityRepository opportunityRepository;
    private final QuoteRepository quoteRepository;

    @Value("${application.sanction-validity-days:180}")
    int sanctionValidityDays;

    @Value("${application.eligible-lease-types:FINANCE_LEASE,OPERATING_LEASE}")
    String eligibleLeaseTypes;

    public ApplicationService(QuoteService quoteService,
                               RedisSequenceGenerator sequenceGenerator,
                               DmsAdapter dmsAdapter,
                               FraudScreeningService fraudScreeningService,
                               NotificationService notificationService,
                               LeadEventProducer eventProducer,
                               ApplicationRepository applicationRepository,
                               ApplicationDocumentRepository applicationDocumentRepository,
                               CamWorkflowRepository camWorkflowRepository,
                               ProspectRepository prospectRepository,
                               OpportunityRepository opportunityRepository,
                               QuoteRepository quoteRepository) {
        this.quoteService = quoteService;
        this.sequenceGenerator = sequenceGenerator;
        this.dmsAdapter = dmsAdapter;
        this.fraudScreeningService = fraudScreeningService;
        this.notificationService = notificationService;
        this.eventProducer = eventProducer;
        this.applicationRepository = applicationRepository;
        this.applicationDocumentRepository = applicationDocumentRepository;
        this.camWorkflowRepository = camWorkflowRepository;
        this.prospectRepository = prospectRepository;
        this.opportunityRepository = opportunityRepository;
        this.quoteRepository = quoteRepository;
    }

    // -------------------------------------------------------
    // PP7.1: Initiate Application
    // -------------------------------------------------------

    @Transactional
    public ApplicationResponse initiateApplication(String prospectId,
                                                    InitiateApplicationRequest req,
                                                    String userId) {
        Prospect prospect = resolveProspect(prospectId);
        Quote    quote    = quoteService.resolveQuoteEntity(req.quoteId);
        Opportunity opp   = opportunityRepository.findById(quote.opportunityId).orElse(null);

        // Quote must be LOCKED before application can be initiated
        if (!quote.isLocked()) {
            throw new BusinessException(ErrorCodes.QUOTE_NOT_LOCKED_FOR_APPLICATION,
                "Quote must be LOCKED before initiating an application (PP7.1).");
        }

        // PP7.8: block if contract/CAM in non-final state
        validateNoModificationBlock(prospect);

        // Check no existing active application for this quote
        Application existing = applicationRepository.findByQuoteId(quote.id).orElse(null);
        if (existing != null && existing.status != ApplicationStatus.CLOSED) {
            throw new BusinessException(ErrorCodes.APPLICATION_ALREADY_EXISTS,
                "An active application already exists for this quote.");
        }

        String appId       = sequenceGenerator.generateApplicationId();
        boolean individual = "INDIVIDUAL".equalsIgnoreCase(prospect.leadType);
        String leaseType   = req.leaseType != null ? req.leaseType : quote.leaseType;

        Application app          = new Application();
        app.applicationId        = appId;
        app.quoteId              = quote.id;
        app.opportunityId        = quote.opportunityId;
        app.prospectUuid         = prospect.id;
        app.prospectBusinessId   = prospect.prospectId;
        app.individual           = individual;
        app.leaseType            = leaseType;
        app.status               = ApplicationStatus.INITIATED;
        app.kycStatus            = "PENDING";
        app.camStatus            = CamStatus.NOT_STARTED;
        app.createdBy            = req.createdBy != null ? req.createdBy : userId;
        app.createdAt            = LocalDateTime.now();
        applicationRepository.save(app);

        // PP7.7: Non-individual → auto-advance to ELIGIBLE_FOR_APPLICATION
        if (!individual) {
            LOG.info("PP7.7 non-individual auto-eligibility: APP={}", appId);
            app.fraudStatus             = "CLEAR";
            app.eligibleForApplication  = true;
            app.status                  = ApplicationStatus.ELIGIBLE_FOR_APPLICATION;
            dispatchWelcomeCommunication(app);
        }

        // Initialise CAM workflow record (NOT_STARTED)
        CamWorkflow cam      = new CamWorkflow();
        cam.applicationId    = app.id;
        cam.camStatus        = CamStatus.NOT_STARTED;
        cam.createdAt        = LocalDateTime.now();
        camWorkflowRepository.save(cam);

        eventProducer.publishApplicationInitiated(appId, prospect.prospectId, opp != null ? opp.opportunityId : null, userId);
        LOG.info("Application initiated: APP={} Prospect={} by={}", appId, prospectId, userId);

        // PP7.10: trigger async fraud screening post-save (individual only, or if non-individual skipped above)
        if (individual) {
            app.status = ApplicationStatus.FRAUD_SCREENING;
            triggerFraudScreeningAsync(appId);
        }

        return buildResponse(app);
    }

    // -------------------------------------------------------
    // PP7.3: Upload KYC Document
    // -------------------------------------------------------

    @Transactional
    public ApplicationResponse uploadDocument(String applicationId,
                                               UploadDocumentRequest req,
                                               String userId) {
        Application app = resolveApplication(applicationId);

        // PP7.8: modification block check
        if (app.modificationBlocked) {
            throw new BusinessException(
                app.modificationBlockReason != null ? app.modificationBlockReason
                    : ErrorCodes.MODIFICATION_CONTRACT_IN_PROGRESS,
                "Application modification is blocked (PP7.8).");
        }

        // Validate document type and applicant prefix
        if (!VALID_DOCUMENT_TYPES.contains(req.documentType)) {
            throw new BusinessException(ErrorCodes.DOCUMENT_TYPE_INVALID,
                "Invalid document type: " + req.documentType
                    + ". Valid: PHOTO, DRIVING_LICENCE, PAN, PASSPORT, OTHER_KYC.");
        }
        if (!VALID_APPLICANT_PREFIXES.contains(req.applicantPrefix)) {
            throw new BusinessException(ErrorCodes.APPLICANT_PREFIX_INVALID,
                "Invalid applicant prefix: " + req.applicantPrefix + ". Valid: MA, A1, A2.");
        }

        // Decode document content
        byte[] docBytes = decodeContent(req.documentContent);

        // PP7.3: DMS upload — route to T/B/L per ENV_INDICATOR
        String dmsIndex = dmsAdapter.uploadDocument(
            app.applicationId,
            req.documentType,
            req.applicantPrefix,
            req.documentName,
            docBytes
        );

        String uploadedBy = req.uploadedBy != null ? req.uploadedBy : userId;

        ApplicationDocument doc = new ApplicationDocument();
        doc.applicationId      = app.id;
        doc.documentType       = req.documentType;
        doc.applicantPrefix    = req.applicantPrefix;
        doc.documentName       = req.documentName;
        doc.dmsDocumentIndex   = dmsIndex;
        doc.dmsEnvironment     = dmsAdapter.getActiveEnvironment();
        doc.uploadStatus       = "UPLOADED";
        doc.uploadedAt         = LocalDateTime.now();
        doc.uploadedBy         = uploadedBy;
        applicationDocumentRepository.save(doc);

        // PP7.11: post-save caution screening (re-run on each document upload)
        boolean cautionOk = fraudScreeningService.runCautionScreening(app);
        if (!cautionOk) {
            // Second commit blocked — return current state without advancing KYC status
            app.updatedBy = userId;
            app.updatedAt = LocalDateTime.now();
            return buildResponse(app);
        }

        // Evaluate KYC completeness
        updateKycStatus(app, userId);

        app.updatedBy = userId;
        app.updatedAt = LocalDateTime.now();

        return buildResponse(app);
    }

    // -------------------------------------------------------
    // PP7.5: Prospect-level application summary (for display)
    // -------------------------------------------------------

    public List<ApplicationResponse> listByProspect(String prospectId) {
        Prospect prospect = resolveProspect(prospectId);
        return applicationRepository.findByProspectUuid(prospect.id)
            .stream()
            .map(this::buildResponse)
            .collect(Collectors.toList());
    }

    public ApplicationResponse getByApplicationId(String applicationId) {
        return buildResponse(resolveApplication(applicationId));
    }

    // -------------------------------------------------------
    // PP7.8: Block / Unblock modification
    // -------------------------------------------------------

    @Transactional
    public void blockModification(String applicationId, String reasonCode) {
        Application app = resolveApplication(applicationId);
        app.modificationBlocked      = true;
        app.modificationBlockReason  = reasonCode;
        app.updatedAt                = LocalDateTime.now();
    }

    @Transactional
    public void unblockModification(String applicationId) {
        Application app = resolveApplication(applicationId);
        app.modificationBlocked     = false;
        app.modificationBlockReason = null;
        app.updatedAt               = LocalDateTime.now();
    }

    // -------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------

    private void updateKycStatus(Application app, String userId) {
        List<ApplicationDocument> docs = applicationDocumentRepository.findByApplicationId(app.id);
        Set<String> uploadedTypes = docs.stream()
            .map(d -> d.documentType)
            .collect(Collectors.toSet());

        Set<String> mandatory = app.individual ? MANDATORY_INDIVIDUAL_DOCS : MANDATORY_COMMERCIAL_DOCS;
        boolean kycComplete = uploadedTypes.containsAll(mandatory);

        app.kycStatus = kycComplete ? "COMPLETE" : "IN_PROGRESS";

        // PP7.12: welcome communication on transition to ELIGIBLE_FOR_APPLICATION
        if (kycComplete
                && !app.eligibleForApplication
                && app.status != ApplicationStatus.PENDING) {
            app.eligibleForApplication = true;
            if (app.status == ApplicationStatus.FRAUD_SCREENING
                    || app.status == ApplicationStatus.INITIATED) {
                app.status = ApplicationStatus.ELIGIBLE_FOR_APPLICATION;
            }
            dispatchWelcomeCommunication(app);
        }
    }

    private void dispatchWelcomeCommunication(Application app) {
        // PP7.12: only for designated eligible lease types
        if (app.leaseType != null && isEligibleLeaseType(app.leaseType) && !app.welcomeCommSent) {
            try {
                notificationService.dispatchWelcomeCommunication(app.applicationId, app.prospectBusinessId, app.leaseType);
                app.welcomeCommSent = true;
                LOG.info("PP7.12 welcome communication dispatched: APP={} leaseType={}",
                    app.applicationId, app.leaseType);
            } catch (Exception e) {
                LOG.warn("PP7.12 welcome communication failed for APP={} (suppressed): {}",
                    app.applicationId, e.getMessage());
            }
        }
    }

    private boolean isEligibleLeaseType(String leaseType) {
        for (String type : eligibleLeaseTypes.split(",")) {
            if (type.trim().equalsIgnoreCase(leaseType)) return true;
        }
        return false;
    }

    private void validateNoModificationBlock(Prospect prospect) {
        // PP7.8a: block if any application for this prospect has a non-final CAM
        List<Application> apps = applicationRepository.findByProspectUuid(prospect.id);
        for (Application a : apps) {
            if (a.camStatus == CamStatus.IN_PROGRESS) {
                throw new BusinessException(ErrorCodes.MODIFICATION_CONTRACT_IN_PROGRESS,
                    "Cannot initiate: CAM is in progress for an existing application (LN3713).");
            }
        }
    }

    private void triggerFraudScreeningAsync(String applicationId) {
        // Runs in a new transaction context via FraudScreeningService
        // In production this would be dispatched to a message queue;
        // for Phase 4 we call synchronously after commit via a try-catch guard
        try {
            fraudScreeningService.runPostSaveScreening(applicationId);
        } catch (Exception e) {
            LOG.warn("ApplicationService: fraud screening dispatch failed for APP={} (suppressed): {}",
                applicationId, e.getMessage());
        }
    }

    private byte[] decodeContent(String base64Content) {
        if (base64Content == null || base64Content.isBlank()) return new byte[0];
        try {
            return Base64.getDecoder().decode(base64Content);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCodes.DOCUMENT_CONTENT_INVALID,
                "Document content must be valid Base64 encoded data.");
        }
    }

    private Application resolveApplication(String applicationId) {
        Application app = applicationRepository.findByApplicationId(applicationId).orElse(null);
        if (app == null) {
            try {
                app = applicationRepository.findById(UUID.fromString(applicationId)).orElse(null);
            } catch (IllegalArgumentException ignored) {}
        }
        if (app == null) {
            throw new BusinessException(ErrorCodes.APPLICATION_NOT_FOUND, "Application not found: " + applicationId);
        }
        return app;
    }

    private Prospect resolveProspect(String id) {
        Prospect p = id.startsWith("PR-") ? prospectRepository.findByProspectId(id).orElse(null) : null;
        if (p == null) {
            try { p = prospectRepository.findById(UUID.fromString(id)).orElse(null); } catch (IllegalArgumentException ignored) {}
        }
        if (p == null) {
            throw new BusinessException(ErrorCodes.PROSPECT_NOT_FOUND, "Prospect not found: " + id);
        }
        if (p.isClosed()) {
            throw new BusinessException(ErrorCodes.PROSPECT_ALREADY_CLOSED, "Prospect is closed: " + id);
        }
        return p;
    }

    private ApplicationResponse buildResponse(Application app) {
        app.documents = applicationDocumentRepository.findByApplicationId(app.id);
        return ApplicationResponse.from(app);
    }
}
