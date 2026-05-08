package com.sf.leasing.lead.api.dto.response;

import com.sf.leasing.lead.domain.model.Opportunity;

import java.time.LocalDateTime;
import java.util.UUID;

public class OpportunityResponse {

    public UUID    id;
    public String  opportunityId;
    public String  prospectBusinessId;
    public String  assetCategory;
    public String  assetClass;
    public String  lobTag;
    public String  status;
    public String  createdBy;
    public LocalDateTime createdAt;
    public String  updatedBy;
    public LocalDateTime updatedAt;

    public static OpportunityResponse from(Opportunity o) {
        OpportunityResponse r = new OpportunityResponse();
        r.id                 = o.id;
        r.opportunityId      = o.opportunityId;
        r.prospectBusinessId = o.prospectBusinessId;
        r.assetCategory      = o.assetCategory;
        r.assetClass         = o.assetClass;
        r.lobTag             = o.lobTag;
        r.status             = o.status != null ? o.status.name() : null;
        r.createdBy          = o.createdBy;
        r.createdAt          = o.createdAt;
        r.updatedBy          = o.updatedBy;
        r.updatedAt          = o.updatedAt;
        return r;
    }
}
