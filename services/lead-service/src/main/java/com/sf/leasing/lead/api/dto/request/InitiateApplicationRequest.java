package com.sf.leasing.lead.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public class InitiateApplicationRequest {

    @NotBlank(message = "Quote ID is mandatory (PP7.1)")
    public String quoteId;

    public String leaseType;
    public String createdBy;
}
