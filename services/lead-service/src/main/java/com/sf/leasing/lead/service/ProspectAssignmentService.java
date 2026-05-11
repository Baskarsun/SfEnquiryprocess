package com.sf.leasing.lead.service;

import com.sf.leasing.lead.api.dto.request.AssignProspectRequest;
import com.sf.leasing.lead.domain.exception.BusinessException;
import com.sf.leasing.lead.domain.exception.ErrorCodes;
import com.sf.leasing.lead.domain.model.Prospect;
import com.sf.leasing.lead.domain.model.ProspectAssignment;
import com.sf.leasing.lead.infrastructure.persistence.ProspectAssignmentRepository;
import com.sf.leasing.lead.infrastructure.persistence.ProspectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
@Service
public class ProspectAssignmentService {

    private static final Logger LOG = LoggerFactory.getLogger(ProspectAssignmentService.class);

    private final NotificationService notificationService;
    private final ProspectRepository prospectRepository;
    private final ProspectAssignmentRepository prospectAssignmentRepository;

    public ProspectAssignmentService(NotificationService notificationService,
                                     ProspectRepository prospectRepository,
                                     ProspectAssignmentRepository prospectAssignmentRepository) {
        this.notificationService = notificationService;
        this.prospectRepository = prospectRepository;
        this.prospectAssignmentRepository = prospectAssignmentRepository;
    }

    @Transactional
    public ProspectAssignment assign(UUID prospectId, AssignProspectRequest req) {
        Prospect prospect = prospectRepository.findById(prospectId).orElseThrow(
            () -> new BusinessException(ErrorCodes.PROSPECT_NOT_FOUND, "Prospect not found: " + prospectId));
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
        prospectAssignmentRepository.save(assignment);

        // Update Prospect's current assignment snapshot
        prospect.assignedUserId          = req.assignedToUserId;
        prospect.assignedTeam            = req.assignedToTeam;
        prospect.assignedBranchCode      = req.assignedToBranchCode;
        prospect.assignmentHierarchyLevel = req.hierarchyLevel;
        prospect.updatedBy               = req.assignedBy;
        prospect.updatedAt               = LocalDateTime.now();
        prospectRepository.save(prospect);

        notificationService.notifyProspectAssigned(
            prospect.prospectId != null ? prospect.prospectId : prospect.id.toString(),
            req.assignedToUserId, req.assignedBy);

        LOG.info("Prospect assigned: Prospect={} to={} at level={} by={}",
            prospectId, req.assignedToUserId, req.hierarchyLevel, req.assignedBy);

        return assignment;
    }

    public List<ProspectAssignment> getAssignmentHistory(UUID prospectId) {
        prospectRepository.findById(prospectId).orElseThrow(
            () -> new BusinessException(ErrorCodes.PROSPECT_NOT_FOUND, "Prospect not found: " + prospectId));
        return prospectAssignmentRepository.findByProspectId(prospectId);
    }
}
