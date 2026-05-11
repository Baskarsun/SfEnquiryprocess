package com.sf.leasing.lead.service;

import com.sf.leasing.lead.api.dto.request.ApproveQuoteRequest;
import com.sf.leasing.lead.api.dto.request.CreateQuoteRequest;
import com.sf.leasing.lead.api.dto.request.LockUnlockQuoteRequest;
import com.sf.leasing.lead.api.dto.response.QuoteResponse;
import com.sf.leasing.lead.domain.enums.OpportunityStatus;
import com.sf.leasing.lead.domain.enums.QuoteStatus;
import com.sf.leasing.lead.domain.exception.BusinessException;
import com.sf.leasing.lead.domain.exception.ErrorCodes;
import com.sf.leasing.lead.domain.model.Opportunity;
import com.sf.leasing.lead.domain.model.Quote;
import com.sf.leasing.lead.infrastructure.adapter.ProductPriceAdapter;
import com.sf.leasing.lead.infrastructure.locking.RedisSequenceGenerator;
import com.sf.leasing.lead.infrastructure.messaging.LeadEventProducer;
import com.sf.leasing.lead.infrastructure.persistence.OpportunityRepository;
import com.sf.leasing.lead.infrastructure.persistence.QuoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * PP6: Quote lifecycle management.
 *
 * Business rules enforced:
 *   PP6.1 — Product Model Price Service; NDLP != submitted cost → override + advisory.
 *   PP6.2 — Rack rate quote; no approval required.
 *   PP6.3 — Customised quote; deviations from rack rate trigger approval workflow.
 *   PP6.4 — Unapproved customised quotes cannot be shared.
 *   PP6.5 — Multiple quote versions per Opportunity; prior version superseded.
 *   PP6.6 — Quote lock (Draft → Shared → Locked); post-lock edits blocked without unlock.
 *   PP6.7 — Appraisal category validation on lock.
 */
@Service
public class QuoteService {

    private static final Logger LOG = LoggerFactory.getLogger(QuoteService.class);

    private final OpportunityService opportunityService;
    private final RedisSequenceGenerator sequenceGenerator;
    private final ProductPriceAdapter productPriceAdapter;
    private final LeadEventProducer eventProducer;
    private final QuoteRepository quoteRepository;
    private final OpportunityRepository opportunityRepository;

    public QuoteService(OpportunityService opportunityService,
                        RedisSequenceGenerator sequenceGenerator,
                        ProductPriceAdapter productPriceAdapter,
                        LeadEventProducer eventProducer,
                        QuoteRepository quoteRepository,
                        OpportunityRepository opportunityRepository) {
        this.opportunityService = opportunityService;
        this.sequenceGenerator = sequenceGenerator;
        this.productPriceAdapter = productPriceAdapter;
        this.eventProducer = eventProducer;
        this.quoteRepository = quoteRepository;
        this.opportunityRepository = opportunityRepository;
    }

    // -------------------------------------------------------
    // PP6.1–PP6.5: Create Quote
    // -------------------------------------------------------

    @Transactional
    public QuoteResponse createQuote(String opportunityId, CreateQuoteRequest req, String userId) {
        Opportunity opp = opportunityService.resolveOpportunityEntity(opportunityId);

        boolean isCustomised = "CUSTOMISED".equalsIgnoreCase(req.quoteType);

        // PP6.1: NDLP check — if NDLP differs from submitted cost, override + advisory
        Optional<BigDecimal> ndlp = productPriceAdapter.fetchNdlp(req.assetMake, req.assetModel, req.assetYear);
        BigDecimal effectiveCost = req.assetCost;
        String advisoryMessage   = null;

        if (ndlp.isPresent() && ndlp.get().compareTo(req.assetCost) != 0) {
            effectiveCost  = ndlp.get();
            advisoryMessage = String.format(
                "Asset cost overridden from %s to NDLP %s per Product Price Service (PP6.1).",
                req.assetCost, ndlp.get());
            LOG.info("PP6.1 NDLP override: OPP={} submitted={} ndlp={}", opportunityId, req.assetCost, ndlp.get());
        }

        // PP6.5: supersede prior DRAFT/APPROVED versions; bump version number
        int newVersion = quoteRepository.maxVersionForOpportunity(opp.id) + 1;
        if (newVersion > 1) {
            supersedePriorVersions(opp.id, userId);
        }

        // PP6.3: pricing deviation detection (customised vs rack rate)
        boolean pricingDeviation = isCustomised && (req.financeAmount != null
            && req.assetCost != null
            && req.financeAmount.compareTo(req.assetCost) > 0);

        boolean approvalRequired = isCustomised && pricingDeviation;

        String quoteId = sequenceGenerator.generateQuoteId();
        Quote quote           = new Quote();
        quote.quoteId         = quoteId;
        quote.opportunityId   = opp.id;
        quote.opportunityBusinessId = opp.opportunityId;
        quote.quoteType       = req.quoteType.toUpperCase();
        quote.version         = newVersion;
        quote.status          = approvalRequired ? QuoteStatus.PENDING_APPROVAL : QuoteStatus.DRAFT;
        quote.assetCost       = effectiveCost;
        quote.productModelPrice = ndlp.orElse(null);
        quote.financeAmount   = req.financeAmount;
        quote.leaseType       = req.leaseType;
        quote.assetMake       = req.assetMake;
        quote.assetModel      = req.assetModel;
        quote.assetYear       = req.assetYear;
        quote.pricingDeviation = pricingDeviation;
        quote.advisoryMessage  = advisoryMessage;
        quote.approvalRequired = approvalRequired;
        quote.appraisalCategory = req.appraisalCategory;
        quote.createdBy       = req.createdBy != null ? req.createdBy : userId;
        quote.createdAt       = LocalDateTime.now();
        quoteRepository.save(quote);

        // Advance Opportunity to QUOTED
        if (opp.status == OpportunityStatus.OPEN) {
            opp.status     = OpportunityStatus.QUOTED;
            opp.updatedBy  = userId;
            opp.updatedAt  = LocalDateTime.now();
        }

        LOG.info("Quote created: QT={} OPP={} version={} type={} approvalRequired={}",
            quoteId, opportunityId, newVersion, req.quoteType, approvalRequired);
        return QuoteResponse.from(quote);
    }

    // -------------------------------------------------------
    // PP6.4: Approve / Reject customised quote
    // -------------------------------------------------------

    @Transactional
    public QuoteResponse processApproval(String quoteId, ApproveQuoteRequest req, String userId) {
        Quote quote = resolveQuoteEntity(quoteId);

        if (quote.status != QuoteStatus.PENDING_APPROVAL) {
            throw new BusinessException(ErrorCodes.QUOTE_NOT_PENDING_APPROVAL,
                "Quote is not awaiting approval: " + quoteId);
        }

        String decidedBy = req.approvedBy != null ? req.approvedBy : userId;
        LocalDateTime now = LocalDateTime.now();

        if ("APPROVE".equalsIgnoreCase(req.decision)) {
            quote.status         = QuoteStatus.APPROVED;
            quote.approvedBy     = decidedBy;
            quote.approvedAt     = now;
            quote.approvalRemarks = req.remarks;
        } else if ("REJECT".equalsIgnoreCase(req.decision)) {
            quote.status          = QuoteStatus.REJECTED;
            quote.rejectionReason = req.rejectionReason;
        } else {
            throw new BusinessException(ErrorCodes.QUOTE_INVALID_DECISION,
                "Decision must be APPROVE or REJECT.");
        }

        quote.updatedBy  = userId;
        quote.updatedAt  = now;

        LOG.info("Quote approval: QT={} decision={} by={}", quoteId, req.decision, decidedBy);
        return QuoteResponse.from(quote);
    }

    // -------------------------------------------------------
    // PP6.6: Share quote (Draft/Approved → Shared)
    // -------------------------------------------------------

    @Transactional
    public QuoteResponse shareQuote(String quoteId, String sharedBy) {
        Quote quote = resolveQuoteEntity(quoteId);

        // PP6.4: unapproved customised quotes cannot be shared
        if (quote.approvalRequired && quote.status != QuoteStatus.APPROVED) {
            throw new BusinessException(ErrorCodes.QUOTE_REQUIRES_APPROVAL,
                "This customised quote requires approval before it can be shared (PP6.4).");
        }
        if (quote.status != QuoteStatus.DRAFT && quote.status != QuoteStatus.APPROVED) {
            throw new BusinessException(ErrorCodes.QUOTE_INVALID_TRANSITION,
                "Quote can only be shared from DRAFT or APPROVED state.");
        }

        quote.status   = QuoteStatus.SHARED;
        quote.sharedAt = LocalDateTime.now();
        quote.sharedBy = sharedBy;
        quote.updatedBy = sharedBy;
        quote.updatedAt = LocalDateTime.now();

        LOG.info("Quote shared: QT={} by={}", quoteId, sharedBy);
        return QuoteResponse.from(quote);
    }

    // -------------------------------------------------------
    // PP6.6: Lock / Unlock quote
    // -------------------------------------------------------

    @Transactional
    public QuoteResponse processLockAction(String quoteId, LockUnlockQuoteRequest req, String userId) {
        Quote quote = resolveQuoteEntity(quoteId);
        LocalDateTime now = LocalDateTime.now();

        switch (req.action == null ? "" : req.action.toUpperCase()) {

            case "LOCK" -> {
                if (quote.status != QuoteStatus.SHARED) {
                    throw new BusinessException(ErrorCodes.QUOTE_INVALID_TRANSITION,
                        "Quote must be SHARED before it can be locked (PP6.6).");
                }
                // PP6.7: appraisal category must be set before lock
                if (quote.appraisalCategory == null || quote.appraisalCategory.isBlank()) {
                    throw new BusinessException(ErrorCodes.QUOTE_APPRAISAL_REQUIRED,
                        "Appraisal category must be set before locking the quote (PP6.7).");
                }
                quote.status   = QuoteStatus.LOCKED;
                quote.lockedAt = now;
                quote.lockedBy = req.requestedBy != null ? req.requestedBy : userId;

                Opportunity opp = opportunityRepository.findById(quote.opportunityId).orElse(null);
                if (opp != null) {
                    opp.status    = OpportunityStatus.QUOTE_LOCKED;
                    opp.updatedBy = userId;
                    opp.updatedAt = now;
                }
                eventProducer.publishQuoteLocked(quote.quoteId, quote.opportunityBusinessId, userId);
                LOG.info("Quote locked: QT={} by={}", quoteId, quote.lockedBy);
            }

            case "REQUEST_UNLOCK" -> {
                if (quote.status != QuoteStatus.LOCKED) {
                    throw new BusinessException(ErrorCodes.QUOTE_NOT_LOCKED,
                        "Quote is not locked; unlock request not applicable.");
                }
                quote.status              = QuoteStatus.UNLOCK_REQUESTED;
                quote.unlockRequestedBy   = req.requestedBy != null ? req.requestedBy : userId;
                quote.unlockRequestedAt   = now;
                LOG.info("Quote unlock requested: QT={} by={}", quoteId, quote.unlockRequestedBy);
            }

            case "APPROVE_UNLOCK" -> {
                if (quote.status != QuoteStatus.UNLOCK_REQUESTED) {
                    throw new BusinessException(ErrorCodes.QUOTE_UNLOCK_NOT_REQUESTED,
                        "No unlock request is pending for this quote.");
                }
                quote.status          = QuoteStatus.SHARED;  // revert to SHARED for editing
                quote.unlockApprovedBy  = req.approvedBy != null ? req.approvedBy : userId;
                quote.unlockApprovedAt  = now;
                quote.lockedAt          = null;
                quote.lockedBy          = null;
                LOG.info("Quote unlocked: QT={} approved by={}", quoteId, quote.unlockApprovedBy);
            }

            default -> throw new BusinessException(ErrorCodes.QUOTE_INVALID_LOCK_ACTION,
                "action must be LOCK, REQUEST_UNLOCK, or APPROVE_UNLOCK.");
        }

        quote.updatedBy  = userId;
        quote.updatedAt  = now;
        return QuoteResponse.from(quote);
    }

    // -------------------------------------------------------
    // Queries
    // -------------------------------------------------------

    public List<QuoteResponse> listByOpportunity(String opportunityId) {
        Opportunity opp = opportunityService.resolveOpportunityEntity(opportunityId);
        return quoteRepository.findByOpportunityIdOrderByVersionAsc(opp.id)
            .stream()
            .map(QuoteResponse::from)
            .collect(Collectors.toList());
    }

    public QuoteResponse getByQuoteId(String quoteId) {
        return QuoteResponse.from(resolveQuoteEntity(quoteId));
    }

    // -------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------

    Quote resolveQuoteEntity(String quoteId) {
        Quote q = quoteRepository.findByQuoteId(quoteId).orElse(null);
        if (q == null) {
            try {
                q = quoteRepository.findById(UUID.fromString(quoteId)).orElse(null);
            } catch (IllegalArgumentException ignored) {}
        }
        if (q == null) {
            throw new BusinessException(ErrorCodes.QUOTE_NOT_FOUND, "Quote not found: " + quoteId);
        }
        return q;
    }

    private void supersedePriorVersions(UUID opportunityId, String updatedBy) {
        List<Quote> prior = quoteRepository.findByOpportunityIdOrderByVersionAsc(opportunityId);
        LocalDateTime now = LocalDateTime.now();
        for (Quote q : prior) {
            if (q.status == QuoteStatus.DRAFT || q.status == QuoteStatus.APPROVED) {
                q.status    = QuoteStatus.SUPERSEDED;
                q.updatedBy = updatedBy;
                q.updatedAt = now;
            }
        }
    }
}
