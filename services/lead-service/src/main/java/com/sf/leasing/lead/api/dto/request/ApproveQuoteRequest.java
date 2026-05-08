package com.sf.leasing.lead.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public class ApproveQuoteRequest {

    /** APPROVE or REJECT */
    @NotBlank(message = "Decision is mandatory (APPROVE | REJECT)")
    public String decision;

    public String remarks;
    public String rejectionReason;
    public String approvedBy;
}
