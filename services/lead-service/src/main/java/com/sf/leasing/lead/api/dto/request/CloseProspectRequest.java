package com.sf.leasing.lead.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public class CloseProspectRequest {

    @NotBlank(message = "closureReasonCode is required.")
    public String closureReasonCode;

    public String closureReasonText;

    @NotBlank(message = "closedBy is required.")
    public String closedBy;
}
