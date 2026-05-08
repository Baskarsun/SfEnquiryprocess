package com.sf.leasing.lead.service;

import com.sf.leasing.lead.api.dto.request.AssignProspectRequest;
import com.sf.leasing.lead.domain.exception.BusinessException;
import com.sf.leasing.lead.domain.exception.ErrorCodes;
import com.sf.leasing.lead.domain.model.Prospect;
import com.sf.leasing.lead.domain.model.ProspectAssignment;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Implements PP2: Prospect Assignment Workflow.
 *
 * Assignment records are append-only — existing entries are never modified.
 * The Prospect entity's current assignment fields are updated to reflect the latest assignment.
 * Assignment chain: CPU → Branch Manager → Associate FO.
 */
@ApplicationScoped
public class ProspectAssignmentService {

    private static final Logger LOG = Logger.getLogger(ProspectAssignmentService.class);

    @Inject
    NotificationService notificationService;

    @Transactional
    public ProspectAssignment assign(UUID prospectId, AssignProspectRequest req) {
        Prospect prospect = Prospect.findById(prospectId);
        if (prospect == null) {
            throw new BusinessException(ErrorCodes.PROSPECT_NOT_FOUND, "Prospect not found: " + prospectId);
        }
        if (prospect.isClosed()) {
            throw new BusinessException(ErrorCodes.PROSPECT_ALREADY_CLOSED,
                "Cannot reassign a closed prospect.");
        }

        // Capture current assignment as previous before overwriting
        String prevUserId = prospect.assignedUserId;
        String prevTeam   = prospect.assignedTeam;

        // Create immutable assignment audit record
        ProspectAssignment assignment = new ProspectAssignment();
        assignment.prospect            = prospect;
        assignment.assignedToUserId    = req.assignedToUserId;
        assignment.assignedToTeam      = req.assignedToTeam;
        assignment.assignedToBranchCode = req.assignedToBranchCode;
        assignment.hierarchyLevel      = req.hierarchyLevel;
        assignment.previousUserId      = prevUserId;
        assignment.previousTeam        = prevTeam;
        assignment.reasonCode          = req.reasonCode;
        assignment.remarks             = req.remarks;
        assignment.assignedBy          = req.assignedBy;
        assignment.assignedAt          = LocalDateTime.now();
        assignment.slaDeadline         = req.slaDeadline;
        assignment.persist();

        // Update Prospect's current assignment snapshot
        prospect.assignedUserId          = req.assignedToUserId;
        prospect.assignedTeam            = req.assignedToTeam;
        prospect.assignedBranchCode      = req.assignedToBranchCode;
        prospect.assignmentHierarchyLevel = req.hierarchyLevel;
        prospect.updatedBy               = req.assignedBy;
        prospect.updatedAt               = LocalDateTime.now();

        notificationService.notifyProspectAssigned(
            prospect.prospectId != null ? prospect.prospectId : prospect.id.toString(),
            req.assignedToUserId, req.assignedBy);

        LOG.infof("Prospect assigned: Prospect=%s to=%s at level=%s by=%s",
            prospectId, req.assignedToUserId, req.hierarchyLevel, req.assignedBy);

        return assignment;
    }

    public List<ProspectAssignment> getAssignmentHistory(UUID prospectId) {
        Prospect prospect = Prospect.findById(prospectId);
        if (prospect == null) {
            throw new BusinessException(ErrorCodes.PROSPECT_NOT_FOUND, "Prospect not found: " + prospectId);
        }
        return ProspectAssignment.findByProspectId(prospectId);
    }
}
