package com.sf.leasing.lead.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public class TriggerKycValidationRequest {

    @NotBlank(message = "validationType is required (PAN or GSTIN).")
    public String validationType;   // PAN | GSTIN

    @NotBlank(message = "triggeredBy is required.")
    public String triggeredBy;
}
