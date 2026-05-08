package com.sf.leasing.lead.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public class CreateQuoteRequest {

    /** RACK_RATE or CUSTOMISED */
    @NotBlank(message = "Quote type is mandatory (PP6.1)")
    public String quoteType;

    @NotNull(message = "Asset cost is mandatory")
    @Positive(message = "Asset cost must be positive")
    public BigDecimal assetCost;

    @Positive(message = "Finance amount must be positive")
    public BigDecimal financeAmount;

    public String leaseType;
    public String assetMake;
    public String assetModel;
    public Integer assetYear;

    public String appraisalCategory;

    public String createdBy;
}
