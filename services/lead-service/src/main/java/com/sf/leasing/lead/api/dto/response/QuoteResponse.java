package com.sf.leasing.lead.api.dto.response;

import com.sf.leasing.lead.domain.model.Quote;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class QuoteResponse {

    public UUID       id;
    public String     quoteId;
    public String     opportunityBusinessId;
    public String     quoteType;
    public int        version;
    public String     status;
    public BigDecimal assetCost;
    public BigDecimal productModelPrice;
    public BigDecimal financeAmount;
    public BigDecimal rackRatePercent;
    public boolean    pricingDeviation;
    public String     advisoryMessage;
    public boolean    approvalRequired;
    public String     approvedBy;
    public LocalDateTime approvedAt;
    public String     approvalRemarks;
    public String     rejectionReason;
    public LocalDateTime sharedAt;
    public LocalDateTime lockedAt;
    public String     appraisalCategory;
    public String     leaseType;
    public String     assetMake;
    public String     assetModel;
    public Integer    assetYear;
    public String     createdBy;
    public LocalDateTime createdAt;
    public String     updatedBy;
    public LocalDateTime updatedAt;

    public static QuoteResponse from(Quote q) {
        QuoteResponse r = new QuoteResponse();
        r.id                   = q.id;
        r.quoteId              = q.quoteId;
        r.opportunityBusinessId = q.opportunityBusinessId;
        r.quoteType            = q.quoteType;
        r.version              = q.version;
        r.status               = q.status != null ? q.status.name() : null;
        r.assetCost            = q.assetCost;
        r.productModelPrice    = q.productModelPrice;
        r.financeAmount        = q.financeAmount;
        r.rackRatePercent      = q.rackRatePercent;
        r.pricingDeviation     = q.pricingDeviation;
        r.advisoryMessage      = q.advisoryMessage;
        r.approvalRequired     = q.approvalRequired;
        r.approvedBy           = q.approvedBy;
        r.approvedAt           = q.approvedAt;
        r.approvalRemarks      = q.approvalRemarks;
        r.rejectionReason      = q.rejectionReason;
        r.sharedAt             = q.sharedAt;
        r.lockedAt             = q.lockedAt;
        r.appraisalCategory    = q.appraisalCategory;
        r.leaseType            = q.leaseType;
        r.assetMake            = q.assetMake;
        r.assetModel           = q.assetModel;
        r.assetYear            = q.assetYear;
        r.createdBy            = q.createdBy;
        r.createdAt            = q.createdAt;
        r.updatedBy            = q.updatedBy;
        r.updatedAt            = q.updatedAt;
        return r;
    }
}
