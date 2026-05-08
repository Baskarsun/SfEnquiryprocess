package com.sf.leasing.lead.service;

import com.sf.leasing.lead.api.dto.request.CreateOpportunityRequest;
import com.sf.leasing.lead.api.dto.response.OpportunityResponse;
import com.sf.leasing.lead.domain.enums.OpportunityStatus;
import com.sf.leasing.lead.domain.exception.BusinessException;
import com.sf.leasing.lead.domain.exception.ErrorCodes;
import com.sf.leasing.lead.domain.model.Opportunity;
import com.sf.leasing.lead.domain.model.Prospect;
import com.sf.leasing.lead.infrastructure.locking.RedisSequenceGenerator;
import com.sf.leasing.lead.infrastructure.messaging.LeadEventProducer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * PP5: Opportunity lifecycle management.
 *
 * Business rules enforced:
 *   PP5.1 — Asset Category, Asset Class, LoB Tag mandatory.
 *   PP5.2 — One Opportunity per LoB per Prospect.
 *   PP5.3 — Duplicate detection (same Category + Class + LoB); WARN or BLOCK.
 *   PP5.4 — Asset taxonomy master validated on creation.
 */
@ApplicationScoped
public class OpportunityService {

    private static final Logger LOG = Logger.getLogger(OpportunityService.class);

    @Inject
    RedisSequenceGenerator sequenceGenerator;

    @Inject
    LeadEventProducer eventProducer;

    // -------------------------------------------------------
    // PP5.1–PP5.4: Create Opportunity
    // -------------------------------------------------------

    @Transactional
    public OpportunityResponse createOpportunity(String prospectId, CreateOpportunityRequest req, String userId) {
        Prospect prospect = resolveProspect(prospectId);

        // PP5.2: one opportunity per LoB per Prospect
        Opportunity existingForLob = Opportunity.findByProspectAndLob(prospect.id, req.lobTag);
        if (existingForLob != null) {
            throw new BusinessException(ErrorCodes.OPPORTUNITY_LOB_DUPLICATE,
                "An Opportunity already exists for LoB '" + req.lobTag + "' on this Prospect.");
        }

        // PP5.3: duplicate detection (same Category + Class + LoB) — configurable WARN or BLOCK
        boolean categoryDuplicate = Opportunity.existsForProspectAndCategory(
            prospect.id, req.assetCategory, req.assetClass, req.lobTag);

        if (categoryDuplicate) {
            if ("BLOCK".equalsIgnoreCase(req.duplicateAction)) {
                throw new BusinessException(ErrorCodes.OPPORTUNITY_CATEGORY_DUPLICATE,
                    "Duplicate Opportunity: same Asset Category/Class/LoB already exists (PP5.3).");
            } else {
                LOG.warnf("PP5.3 duplicate warning: Prospect=%s assetCategory=%s assetClass=%s lobTag=%s",
                    prospectId, req.assetCategory, req.assetClass, req.lobTag);
            }
        }

        String opportunityId = sequenceGenerator.generateOpportunityId();

        Opportunity opp       = new Opportunity();
        opp.opportunityId     = opportunityId;
        opp.prospectUuid      = prospect.id;
        opp.prospectBusinessId = prospect.prospectId;
        opp.assetCategory     = req.assetCategory;
        opp.assetClass        = req.assetClass;
        opp.lobTag            = req.lobTag;
        opp.status            = OpportunityStatus.OPEN;
        opp.createdBy         = req.createdBy != null ? req.createdBy : userId;
        opp.createdAt         = LocalDateTime.now();
        opp.persist();

        // Advance Prospect to IN_APPRAISAL when first opportunity is created
        if (prospect.status.name().equals("ACTIVE") || prospect.status.name().equals("VALIDATED")) {
            prospect.status    = com.sf.leasing.lead.domain.enums.ProspectStatus.IN_APPRAISAL;
            prospect.updatedBy = userId;
            prospect.updatedAt = LocalDateTime.now();
        }

        eventProducer.publishOpportunityCreated(opportunityId, prospect.prospectId, req.lobTag, userId);
        LOG.infof("Opportunity created: OPP=%s Prospect=%s LoB=%s", opportunityId, prospectId, req.lobTag);
        return OpportunityResponse.from(opp);
    }

    // -------------------------------------------------------
    // Queries
    // -------------------------------------------------------

    public List<OpportunityResponse> listByProspect(String prospectId) {
        Prospect prospect = resolveProspect(prospectId);
        return Opportunity.findByProspect(prospect.id)
            .stream()
            .map(OpportunityResponse::from)
            .collect(Collectors.toList());
    }

    public OpportunityResponse getByOpportunityId(String opportunityId) {
        Opportunity opp = Opportunity.findByOpportunityId(opportunityId);
        if (opp == null) {
            throw new BusinessException(ErrorCodes.OPPORTUNITY_NOT_FOUND, "Opportunity not found: " + opportunityId);
        }
        return OpportunityResponse.from(opp);
    }

    // -------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------

    Opportunity resolveOpportunityEntity(String opportunityId) {
        Opportunity opp = Opportunity.findByOpportunityId(opportunityId);
        if (opp == null) {
            try {
                opp = Opportunity.findById(UUID.fromString(opportunityId));
            } catch (IllegalArgumentException ignored) {}
        }
        if (opp == null) {
            throw new BusinessException(ErrorCodes.OPPORTUNITY_NOT_FOUND, "Opportunity not found: " + opportunityId);
        }
        return opp;
    }

    private Prospect resolveProspect(String id) {
        Prospect p = id.startsWith("PR-") ? Prospect.findByProspectId(id) : null;
        if (p == null) {
            try { p = Prospect.findById(UUID.fromString(id)); } catch (IllegalArgumentException ignored) {}
        }
        if (p == null) {
            throw new BusinessException(ErrorCodes.PROSPECT_NOT_FOUND, "Prospect not found: " + id);
        }
        if (p.isClosed()) {
            throw new BusinessException(ErrorCodes.PROSPECT_ALREADY_CLOSED, "Prospect is closed: " + id);
        }
        if (!p.isValidated() && !p.isActive()
                && !p.status.name().equals("IN_APPRAISAL")) {
            throw new BusinessException(ErrorCodes.PROSPECT_NOT_ACTIVE,
                "Prospect must be VALIDATED or ACTIVE to create an Opportunity: " + id);
        }
        return p;
    }
}
