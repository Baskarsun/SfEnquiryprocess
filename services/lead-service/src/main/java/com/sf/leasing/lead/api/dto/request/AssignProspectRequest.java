package com.sf.leasing.lead.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public class AssignProspectRequest {

    @NotBlank(message = "assignedToUserId is required.")
    public String assignedToUserId;

    public String assignedToTeam;

    public String assignedToBranchCode;

    @NotBlank(message = "hierarchyLevel is required.")
    public String hierarchyLevel;     // CENTRAL | BRANCH | ASSOCIATE

    @NotBlank(message = "reasonCode is required.")
    public String reasonCode;

    @NotBlank(message = "remarks are required.")
    public String remarks;

    @NotBlank(message = "assignedBy is required.")
    public String assignedBy;

    public LocalDateTime slaDeadline;
}
