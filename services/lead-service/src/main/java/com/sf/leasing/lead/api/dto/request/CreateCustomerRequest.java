package com.sf.leasing.lead.api.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * PP8.1: Trigger enterprise customer creation from a sanctioned application.
 * The gate check (KYC = Complete AND CAM = Approved) is enforced by the service layer.
 */
public class CreateCustomerRequest {

    @JsonProperty("applicationId")
    public String applicationId;   // APP-YYYY-NNNNNN (required)

    @JsonProperty("createdBy")
    public String createdBy;       // Operator user ID (required)
}
