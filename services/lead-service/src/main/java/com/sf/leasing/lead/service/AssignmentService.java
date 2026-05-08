package com.sf.leasing.lead.service;

import com.sf.leasing.lead.api.dto.request.AssignLeadRequest;
import com.sf.leasing.lead.domain.enums.HierarchyLevel;
import com.sf.leasing.lead.domain.enums.LeadStatus;
import com.sf.leasing.lead.domain.exception.BusinessException;
import com.sf.leasing.lead.domain.exception.ErrorCodes;
import com.sf.leasing.lead.domain.model.Lead;
import com.sf.leasing.lead.domain.model.LeadAssignment;
import com.sf.leasing.lead.infrastructure.messaging.LeadEventProducer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implements LP5: Lead Assignment & Re-assignment (4-level hierarchy).
 */
@ApplicationScoped
public class AssignmentService {

    private static final Logger LOG = Logger.getLogger(AssignmentService.class);

    @Inject
    EntityManager em;

    @Inject
    LeadEventProducer eventProducer;

    @Inject
    NotificationService notificationService;

    @Transactional
    public void assignLead(String lrn, AssignLeadRequest req, String assignedBy) {

        Lead lead = Lead.findByLrn(lrn);
        if (lead == null) {
            throw new BusinessException("LEAD_NOT_FOUND", "Lead not found: " + lrn);
        }

        // Closed leads cannot be re-assigned
        if (lead.isClosed()) {
            throw new BusinessException("LEAD_CLOSED", "Assignment not permitted on a closed lead.");
        }

        // Rule LP5.2: Remarks and reason code are mandatory
        if (isBlank(req.reasonCode)) {
            throw new BusinessException("REMARKS_REQUIRED", "Reason code is mandatory for assignment.");
        }
        if (isBlank(req.remarks)) {
            throw new BusinessException("REMARKS_REQUIRED", "Remarks are mandatory for assignment.");
        }

        // Rule LP5.3: Branch change after initial assignment requires exception flow
        if (!isBlank(lead.assignedBranchCode)
            && !isBlank(req.branchCode)
            && !lead.assignedBranchCode.equals(req.branchCode)) {
            if (!req.branchChangeApproved) {
                throw new BusinessException("BRANCH_CHANGE_EXCEPTION",
                    "Branch change requires exception workflow approval. Set branchChangeApproved=true after obtaining approval.");
            }
        }

        // Rule LP5.5: If assigning to an FO who is on leave — auto-escalate to branch manager
        String effectiveUserId = req.assignToUserId;
        HierarchyLevel effectiveLevel = req.hierarchyLevel;

        if (req.hierarchyLevel == HierarchyLevel.FIELD_OFFICER && isEmployeeOnLeave(req.assignToUserId)) {
            LOG.warnf("FO %s is on leave — escalating lead %s to Branch Manager", req.assignToUserId, lrn);
            effectiveUserId = findBranchManager(req.branchCode);
            effectiveLevel  = HierarchyLevel.BRANCH_MANAGER;
            notificationService.notifyFoAbsenceEscalation(lrn, req.assignToUserId, effectiveUserId);
        }

        // Compute SLA deadline (configurable per level)
        LocalDateTime slaDeadline = computeSlaDeadline(effectiveLevel);

        // Create immutable audit record
        LeadAssignment assignment = new LeadAssignment();
        assignment.lead                 = lead;
        assignment.assignedToUserId     = effectiveUserId;
        assignment.assignedToTeam       = req.assignToTeam;
        assignment.assignedToBranchCode = req.branchCode;
        assignment.hierarchyLevel       = effectiveLevel;
        assignment.previousUserId       = lead.assignedUserId;
        assignment.previousTeam         = lead.assignedTeam;
        assignment.reasonCode           = req.reasonCode;
        assignment.remarks              = req.remarks;
        assignment.assignedBy           = assignedBy;
        assignment.assignedAt           = LocalDateTime.now();
        assignment.slaDeadline          = slaDeadline;
        assignment.persist();

        // Update lead's current assignment
        lead.assignedUserId           = effectiveUserId;
        lead.assignedTeam             = req.assignToTeam;
        lead.assignedBranchCode       = req.branchCode;
        lead.assignmentHierarchyLevel = effectiveLevel;
        lead.updatedBy                = assignedBy;
        lead.updatedAt                = LocalDateTime.now();

        // Transition status from NEW to ASSIGNED if still new
        if (lead.status == LeadStatus.NEW) {
            lead.status = LeadStatus.ASSIGNED;
        }

        eventProducer.publishLeadAssigned(lrn, effectiveUserId, effectiveLevel.name());
        LOG.infof("Lead %s assigned to %s (%s) by %s; SLA deadline: %s",
            lrn, effectiveUserId, effectiveLevel, assignedBy, slaDeadline);
    }

    /**
     * Checks open SLA timers and marks breaches; escalates to next-level manager.
     * Rule LP5.4: Called by scheduled job hourly.
     */
    @Transactional
    public void checkAndMarkSlaBreaches() {
        List<Object[]> breached = em.createNativeQuery(
            "SELECT la.id, la.lead_id, la.assigned_to_user_id, la.hierarchy_level " +
            "FROM lead_assignments la " +
            "JOIN leads l ON l.id = la.lead_id " +
            "WHERE la.sla_breached = FALSE " +
            "  AND la.sla_deadline < CURRENT_TIMESTAMP " +
            "  AND l.status NOT IN ('CLOSED', 'PROMOTED')"
        ).getResultList();

        for (Object[] row : breached) {
            em.createNativeQuery(
                "UPDATE lead_assignments SET sla_breached = TRUE, sla_breached_at = CURRENT_TIMESTAMP WHERE id = ?1"
            ).setParameter(1, row[0]).executeUpdate();

            String lrn = findLrnForLead(row[1].toString());
            notificationService.notifySlaBreached(lrn, row[2].toString(), row[3].toString());
            LOG.infof("SLA breached for lead %s, assigned to %s at level %s", lrn, row[2], row[3]);
        }
    }

    // -------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------

    private boolean isEmployeeOnLeave(String userId) {
        List<Object[]> rows = em.createNativeQuery(
            "SELECT status FROM employees WHERE employee_id = ?1"
        ).setParameter(1, userId).getResultList();
        return !rows.isEmpty() && "ON_LEAVE".equals(rows.get(0)[0]);
    }

    private String findBranchManager(String branchCode) {
        List<Object[]> rows = em.createNativeQuery(
            "SELECT employee_id FROM employees WHERE branch_code = ?1 AND role = 'BRANCH_MANAGER' AND status = 'ACTIVE' LIMIT 1"
        ).setParameter(1, branchCode).getResultList();
        if (rows.isEmpty()) {
            throw new BusinessException("BRANCH_MANAGER_NOT_FOUND",
                "No active Branch Manager found for branch: " + branchCode);
        }
        return (String) rows.get(0)[0];
    }

    private LocalDateTime computeSlaDeadline(HierarchyLevel level) {
        List<Object[]> rows = em.createNativeQuery(
            "SELECT threshold_hours FROM sla_config WHERE hierarchy_level = ?1"
        ).setParameter(1, level.name()).getResultList();
        int hours = rows.isEmpty() ? 48 : ((Number) rows.get(0)[0]).intValue();
        return LocalDateTime.now().plusHours(hours);
    }

    private String findLrnForLead(String leadId) {
        List<Object[]> rows = em.createNativeQuery(
            "SELECT lrn FROM leads WHERE id = ?1::uuid"
        ).setParameter(1, leadId).getResultList();
        return rows.isEmpty() ? leadId : (String) rows.get(0)[0];
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
