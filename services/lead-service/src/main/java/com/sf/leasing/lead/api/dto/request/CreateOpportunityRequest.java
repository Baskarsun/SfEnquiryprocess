package com.sf.leasing.lead.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public class CreateOpportunityRequest {

    @NotBlank(message = "Asset category is mandatory (PP5.1)")
    public String assetCategory;

    @NotBlank(message = "Asset class is mandatory (PP5.1)")
    public String assetClass;

    @NotBlank(message = "LoB tag is mandatory (PP5.1)")
    public String lobTag;

    /** WARN (default) or BLOCK on duplicate detection */
    public String duplicateAction = "WARN";

    public String createdBy;
}
