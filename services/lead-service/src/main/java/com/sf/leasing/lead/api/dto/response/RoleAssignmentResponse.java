package com.sf.leasing.lead.api.dto.response;

import com.sf.leasing.lead.domain.model.CustomerRoleAssignment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response payload for a customer role assignment.
 */
public class RoleAssignmentResponse {

    public UUID id;
    public String customerId;
    public String roleType;
    public boolean checklistComplete;
    public List<String> checklistItems;
    public String status;
    public String remarks;
    public String assignedBy;
    public LocalDateTime assignedAt;

    public static RoleAssignmentResponse from(CustomerRoleAssignment ra) {
        RoleAssignmentResponse r = new RoleAssignmentResponse();
        r.id               = ra.id;
        r.customerId       = ra.customerId;
        r.roleType         = ra.roleType;
        r.checklistComplete = ra.checklistComplete;
        r.status           = ra.status;
        r.remarks          = ra.remarks;
        r.assignedBy       = ra.assignedBy;
        r.assignedAt       = ra.assignedAt;
        return r;
    }
}
