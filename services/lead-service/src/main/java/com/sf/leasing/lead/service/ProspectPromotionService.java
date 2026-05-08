package com.sf.leasing.lead.service;

import com.sf.leasing.lead.api.dto.response.ProspectPromotionResponse;
import com.sf.leasing.lead.domain.enums.LeadStatus;
import com.sf.leasing.lead.domain.enums.ProspectStatus;
import com.sf.leasing.lead.domain.exception.BusinessException;
import com.sf.leasing.lead.domain.exception.ErrorCodes;
import com.sf.leasing.lead.domain.model.Lead;
import com.sf.leasing.lead.domain.model.Lineage;
import com.sf.leasing.lead.domain.model.Prospect;
import com.sf.leasing.lead.infrastructure.messaging.LeadEventProducer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implements the Lead-to-Prospect promotion flow (LP8 → PP1).
 *
 * Flow:
 *   1. Run full 17-point qualification checklist via LeadQualificationService.
 *   2. Create Prospect entity in DRAFT status.
 *   3. Create Lineage record linking Lead → Prospect.
 *   4. Update Lead status to PROMOTED.
 *   5. Publish LeadPromoted Kafka event.
 *   6. Notify sourcing marketing employee.
 *
 * The Prospect.prospectId (PR-YYYY-NNNNNN) is NOT set here — it is gated on
 * successful external KYC validation by ProspectValidationService (PP3).
 */
@ApplicationScoped
public class ProspectPromotionService {

    private static final Logger LOG = Logger.getLogger(ProspectPromotionService.class);

    @Inject
    LeadQualificationService qualificationService;

    @Inject
    LeadEventProducer eventProducer;

    @Inject
    NotificationService notificationService;

    @Transactional
    public ProspectPromotionResponse promote(String lrn, String promotedBy) {
        // Load and guard Lead
        Lead lead = Lead.findByLrn(lrn);
        if (lead == null) {
            throw new BusinessException(ErrorCodes.LEAD_NOT_FOUND, "Lead not found: " + lrn);
        }
        if (lead.isPromoted()) {
            throw new BusinessException(ErrorCodes.LEAD_ALREADY_PROMOTED, "Lead is already promoted: " + lrn);
        }
        if (lead.isClosed()) {
            throw new BusinessException(ErrorCodes.LEAD_NOT_PROMOTABLE, "Closed leads cannot be promoted: " + lrn);
        }

        // Run full 17-point qualification checklist — throws BusinessException on hard failures
        List<String> warnings = qualificationService.validateForPromotion(lrn);

        // Create Prospect in DRAFT status
        Prospect prospect = new Prospect();
        prospect.prospectId          = null;        // set only after PP3 KYC validation
        prospect.leadLrn             = lead.lrn;
        prospect.leadId              = lead.id;
        prospect.leadType            = lead.leadType != null ? lead.leadType.name() : null;
        prospect.status              = ProspectStatus.DRAFT;
        prospect.pan                 = extractPan(lead);
        prospect.gstin               = extractGstin(lead);
        prospect.dedupLabel          = lead.dedupLabel != null ? lead.dedupLabel.name() : null;
        prospect.ucic                = lead.ucic;
        prospect.existingCustomerCodes = lead.existingCustomerCodes;
        prospect.assignedBranchCode  = lead.assignedBranchCode;
        prospect.assignedTeam        = lead.assignedTeam;
        prospect.assignedUserId      = lead.assignedUserId;
        prospect.assignmentHierarchyLevel = lead.assignmentHierarchyLevel != null
            ? lead.assignmentHierarchyLevel.name() : null;
        prospect.createdBy           = promotedBy;
        prospect.createdAt           = LocalDateTime.now();
        prospect.persist();

        // Create immutable Lineage record
        Lineage lineage = new Lineage();
        lineage.leadLrn       = lead.lrn;
        lineage.leadId        = lead.id;
        lineage.prospectUuid  = prospect.id;
        lineage.createdAt     = LocalDateTime.now();
        lineage.persist();

        // Transition Lead → PROMOTED
        lead.status      = LeadStatus.PROMOTED;
        lead.promotedAt  = LocalDateTime.now();
        lead.prospectId  = prospect.id.toString();
        lead.updatedBy   = promotedBy;
        lead.updatedAt   = LocalDateTime.now();

        // Publish event and notify
        eventProducer.publishLeadPromoted(lrn, prospect.id.toString(), promotedBy);
        notificationService.notifyLeadPromoted(lrn, prospect.id.toString(), promotedBy);

        LOG.infof("Lead promoted to Prospect: LRN=%s prospectUuid=%s by=%s", lrn, prospect.id, promotedBy);
        return ProspectPromotionResponse.of(prospect.id, lrn, warnings);
    }

    private String extractPan(Lead lead) {
        if (lead.applicants == null || lead.applicants.isEmpty()) return null;
        return lead.applicants.get(0).pan;
    }

    private String extractGstin(Lead lead) {
        if (lead.applicants == null || lead.applicants.isEmpty()) return null;
        return lead.applicants.get(0).gstin;
    }
}
