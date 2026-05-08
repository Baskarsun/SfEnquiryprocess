package com.sf.leasing.lead.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public class OverrideKycValidationRequest {

    @NotBlank(message = "validationType is required (PAN or GSTIN).")
    public String validationType;       // PAN | GSTIN

    @NotBlank(message = "overrideReasonCode is required.")
    public String overrideReasonCode;

    @NotBlank(message = "overrideReasonText is required.")
    public String overrideReasonText;

    @NotBlank(message = "overrideBy is required.")
    public String overrideBy;
}
