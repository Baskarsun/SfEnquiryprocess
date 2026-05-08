package com.sf.leasing.lead.api.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * PP8.3: Assign a role to a customer and record checklist completion status.
 */
public class AssignRoleRequest {

    /** One of: LESSEE_INDIVIDUAL | LESSEE_CORPORATE | DEALER | VENDOR | DEPOSITOR */
    @JsonProperty("roleType")
    public String roleType;

    @JsonProperty("checklistItems")
    public List<String> checklistItems;   // Completed checklist item codes

    @JsonProperty("checklistComplete")
    public boolean checklistComplete;

    @JsonProperty("remarks")
    public String remarks;

    @JsonProperty("assignedBy")
    public String assignedBy;
}
