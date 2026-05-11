package com.sf.leasing.lead.service;

import com.sf.leasing.lead.domain.enums.ProspectStatus;
import com.sf.leasing.lead.domain.exception.BusinessException;
import com.sf.leasing.lead.domain.exception.ErrorCodes;
import com.sf.leasing.lead.domain.model.Lineage;
import com.sf.leasing.lead.domain.model.Prospect;
import com.sf.leasing.lead.domain.model.ProspectKycValidation;
import com.sf.leasing.lead.infrastructure.adapter.GstinValidationAdapter;
import com.sf.leasing.lead.infrastructure.adapter.PanValidationAdapter;
import com.sf.leasing.lead.infrastructure.locking.RedisSequenceGenerator;
import com.sf.leasing.lead.infrastructure.persistence.LineageRepository;
import com.sf.leasing.lead.infrastructure.persistence.ProspectKycValidationRepository;
import com.sf.leasing.lead.infrastructure.persistence.ProspectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Implements PP3: External KYC Validation for Prospects.
 *
 * Key business rules:
 * - Prospect.prospectId (PR-YYYY-NNNNNN) is null until this service generates it on first SUCCESS.
 * - Failure routes to the exception queue with a 24-hour SLA.
 * - Each validation run produces an immutable ProspectKycValidation record.
 * - Manual override produces an OVERRIDDEN record and follows the same ID-generation path as SUCCESS.
 * - Lineage.prospectBusinessId is updated once the Prospect ID is assigned.
 */
@Service
public class ProspectValidationService {

    private static final Logger LOG = LoggerFactory.getLogger(ProspectValidationService.class);

    @Value("${prospect.validation-failure-sla-hours:24}")
    int validationFailureSlaHours;

    private final PanValidationAdapter panAdapter;
    private final GstinValidationAdapter gstinAdapter;
    private final RedisSequenceGenerator sequenceGenerator;
    private final ExceptionQueueService exceptionQueueService;
    private final NotificationService notificationService;
    private final ProspectRepository prospectRepository;
    private final ProspectKycValidationRepository prospectKycValidationRepository;
    private final LineageRepository lineageRepository;

    public ProspectValidationService(PanValidationAdapter panAdapter,
                                     GstinValidationAdapter gstinAdapter,
                                     RedisSequenceGenerator sequenceGenerator,
                                     ExceptionQueueService exceptionQueueService,
                                     NotificationService notificationService,
                                     ProspectRepository prospectRepository,
                                     ProspectKycValidationRepository prospectKycValidationRepository,
                                     LineageRepository lineageRepository) {
        this.panAdapter = panAdapter;
        this.gstinAdapter = gstinAdapter;
        this.sequenceGenerator = sequenceGenerator;
        this.exceptionQueueService = exceptionQueueService;
        this.notificationService = notificationService;
        this.prospectRepository = prospectRepository;
        this.prospectKycValidationRepository = prospectKycValidationRepository;
        this.lineageRepository = lineageRepository;
    }

    // -------------------------------------------------------
    // PP3.1 — Trigger external KYC validation
    // -------------------------------------------------------

    @Transactional
    public Prospect triggerValidation(UUID prospectId, String rawType, String triggeredBy) {
        // Validate type before DB lookup so the caller gets the right error code
        String type = (rawType != null ? rawType : "").toUpperCase().trim();
        validateType(type);

        Prospect prospect = prospectRepository.findById(prospectId).orElseThrow(
            () -> new BusinessException(ErrorCodes.PROSPECT_NOT_FOUND, "Prospect not found: " + prospectId));
        if (prospect.isClosed()) {
            throw new BusinessException(ErrorCodes.PROSPECT_ALREADY_CLOSED, "Cannot validate a closed prospect.");
        }

        if ("PAN".equals(type)) {
            return validatePan(prospect, triggeredBy);
        } else {
            return validateGstin(prospect, triggeredBy);
        }
    }

    // -------------------------------------------------------
    // PP3.2 — Manual override (authorised officer)
    // -------------------------------------------------------

    @Transactional
    public Prospect override(UUID prospectId, String rawType, String overrideReasonCode,
                             String overrideReasonText, String overrideBy) {
        String validationType = (rawType != null ? rawType : "").toUpperCase().trim();
        validateType(validationType);

        Prospect prospect = prospectRepository.findById(prospectId).orElseThrow(
            () -> new BusinessException(ErrorCodes.PROSPECT_NOT_FOUND, "Prospect not found: " + prospectId));
        if (prospect.isClosed()) {
            throw new BusinessException(ErrorCodes.PROSPECT_ALREADY_CLOSED, "Cannot override a closed prospect.");
        }

        // Record OVERRIDDEN validation entry
        ProspectKycValidation kycRecord = new ProspectKycValidation();
        kycRecord.prospect             = prospect;
        kycRecord.validationType       = validationType;
        kycRecord.identifier           = "PAN".equals(validationType) ? prospect.pan : prospect.gstin;
        kycRecord.status               = "OVERRIDDEN";
        kycRecord.overrideReasonCode   = overrideReasonCode;
        kycRecord.overrideReasonText   = overrideReasonText;
        kycRecord.overrideBy           = overrideBy;
        kycRecord.overrideAt           = LocalDateTime.now();
        kycRecord.validatedAt          = LocalDateTime.now();
        prospectKycValidationRepository.save(kycRecord);

        // Mark the specific KYC flag as validated
        applyValidationFlag(prospect, validationType, null, null, null);

        // Generate Prospect ID if not yet assigned (PP3.1 gating)
        assignProspectIdIfAbsent(prospect);

        // Update override audit on Prospect record
        prospect.validationOverrideReason  = overrideReasonCode + ": " + overrideReasonText;
        prospect.validationOverrideBy      = overrideBy;
        prospect.validationOverrideAt      = LocalDateTime.now();
        prospect.updatedBy                 = overrideBy;
        prospect.updatedAt                 = LocalDateTime.now();
        prospectRepository.save(prospect);

        notificationService.notifyKycOverride(prospect.prospectId, validationType, overrideBy);
        LOG.info("KYC override recorded: Prospect={} type={} by={}", prospect.prospectId, validationType, overrideBy);

        return prospect;
    }

    // -------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------

    private Prospect validatePan(Prospect prospect, String triggeredBy) {
        if (prospect.pan == null || prospect.pan.isBlank()) {
            throw new BusinessException(ErrorCodes.PAN_NOT_SET, "Prospect has no PAN to validate.");
        }

        PanValidationAdapter.PanValidationResult result = panAdapter.validate(prospect.pan);

        ProspectKycValidation kycRecord = new ProspectKycValidation();
        kycRecord.prospect             = prospect;
        kycRecord.validationType       = "PAN";
        kycRecord.identifier           = prospect.pan;
        kycRecord.validationSource     = "PAN_API";
        kycRecord.validatedAt          = LocalDateTime.now();

        if (result.valid()) {
            kycRecord.status            = "SUCCESS";
            kycRecord.legalName         = result.legalName();
            kycRecord.registeredAddress = result.registeredAddress();
            prospectKycValidationRepository.save(kycRecord);

            applyValidationFlag(prospect, "PAN", result.legalName(), result.registeredAddress(), null);
            assignProspectIdIfAbsent(prospect);
            prospectRepository.save(prospect);

            notificationService.notifyKycValidationSuccess(prospect.id.toString(), prospect.prospectId);
            LOG.info("PAN validated: Prospect={} pan={}", prospect.id, prospect.pan);
        } else {
            kycRecord.status = "FAILED";
            prospectKycValidationRepository.save(kycRecord);

            routeToExceptionQueue(prospect, "PAN", result.errorMessage());
            notificationService.notifyKycValidationFailure(
                prospect.id.toString(), "PAN", result.errorMessage());
            LOG.warn("PAN validation failed: Prospect={} reason={}", prospect.id, result.errorMessage());
        }

        return prospect;
    }

    private Prospect validateGstin(Prospect prospect, String triggeredBy) {
        if (prospect.gstin == null || prospect.gstin.isBlank()) {
            throw new BusinessException(ErrorCodes.GSTIN_NOT_SET, "Prospect has no GSTIN to validate.");
        }

        GstinValidationAdapter.GstinValidationResult result = gstinAdapter.validate(prospect.gstin);

        ProspectKycValidation kycRecord = new ProspectKycValidation();
        kycRecord.prospect             = prospect;
        kycRecord.validationType       = "GSTIN";
        kycRecord.identifier           = prospect.gstin;
        kycRecord.validationSource     = "GSTIN_API";
        kycRecord.validatedAt          = LocalDateTime.now();

        if (result.valid()) {
            kycRecord.status            = "SUCCESS";
            kycRecord.legalName         = result.legalEntityName();
            kycRecord.registeredAddress = result.registeredAddress();
            kycRecord.derivedPan        = result.derivedPan();
            prospectKycValidationRepository.save(kycRecord);

            applyValidationFlag(prospect, "GSTIN", result.legalEntityName(), result.registeredAddress(), result.derivedPan());
            assignProspectIdIfAbsent(prospect);
            prospectRepository.save(prospect);

            notificationService.notifyKycValidationSuccess(prospect.id.toString(), prospect.prospectId);
            LOG.info("GSTIN validated: Prospect={} gstin={}", prospect.id, prospect.gstin);
        } else {
            kycRecord.status = "FAILED";
            prospectKycValidationRepository.save(kycRecord);

            routeToExceptionQueue(prospect, "GSTIN", result.errorMessage());
            notificationService.notifyKycValidationFailure(
                prospect.id.toString(), "GSTIN", result.errorMessage());
            LOG.warn("GSTIN validation failed: Prospect={} reason={}", prospect.id, result.errorMessage());
        }

        return prospect;
    }

    private void applyValidationFlag(Prospect prospect, String type,
                                     String legalName, String address, String derivedPan) {
        if ("PAN".equals(type)) {
            prospect.panValidated          = true;
            if (legalName != null)  prospect.legalName         = legalName;
            if (address   != null)  prospect.registeredAddress = address;
        } else {
            prospect.gstinValidated        = true;
            if (legalName != null)  prospect.legalName         = legalName;
            if (address   != null)  prospect.registeredAddress = address;
            if (derivedPan != null) prospect.pan               = derivedPan; // cross-reference PAN from GSTIN
        }

        // Transition to VALIDATED when at least one check passes
        if (prospect.status == ProspectStatus.DRAFT) {
            prospect.status             = ProspectStatus.VALIDATED;
            prospect.validationTimestamp = LocalDateTime.now();
        }

        prospect.updatedAt = LocalDateTime.now();
    }

    /** PP3.1 gating: generate PR-YYYY-NNNNNN only once. */
    private void assignProspectIdIfAbsent(Prospect prospect) {
        if (prospect.prospectId != null) return;

        String newId = sequenceGenerator.generateProspectId();
        prospect.prospectId = newId;

        // Update Lineage with the business ID
        Lineage lineage = lineageRepository.findByProspectUuid(prospect.id).orElse(null);
        if (lineage != null) {
            lineage.prospectBusinessId = newId;
            lineage.updatedAt          = LocalDateTime.now();
        }

        LOG.info("Prospect ID assigned: {} → {}", prospect.id, newId);
    }

    private void routeToExceptionQueue(Prospect prospect, String validationType, String reason) {
        exceptionQueueService.routeToExceptionQueue(
            "PROSPECT_VALIDATION",
            prospect.id.toString(),
            ErrorCodes.KYC_VALIDATION_FAILED,
            validationType + " validation failed: " + reason,
            "prospect_id",
            "{\"prospectId\":\"" + prospect.id + "\",\"type\":\"" + validationType + "\"}"
        );
    }

    private void validateType(String type) {
        if (!"PAN".equals(type) && !"GSTIN".equals(type)) {
            throw new BusinessException(ErrorCodes.INVALID_VALIDATION_TYPE,
                "validationType must be PAN or GSTIN, got: " + type);
        }
    }
}
