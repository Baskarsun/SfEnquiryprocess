package com.sf.leasing.lead.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public class PromoteToProspectRequest {

    @NotBlank(message = "promotedBy (userId) is required.")
    public String promotedBy;
}
