package com.sf.leasing.lead.api.dto.request;

import com.sf.leasing.lead.domain.enums.HierarchyLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AssignLeadRequest {

    @NotBlank(message = "Assign-to user ID is required")
    public String assignToUserId;

    public String assignToTeam;

    public String branchCode;

    @NotNull(message = "Hierarchy level is required")
    public HierarchyLevel hierarchyLevel;

    @NotBlank(message = "Reason code is mandatory for assignment")
    public String reasonCode;

    @NotBlank(message = "Remarks are mandatory for assignment")
    public String remarks;

    public boolean branchChangeApproved = false;
}
