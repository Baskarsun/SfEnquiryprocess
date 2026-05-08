package com.sf.leasing.lead.api.dto.response;

import java.util.List;
import java.util.UUID;

public class ProspectPromotionResponse {

    public UUID   prospectUuid;
    public String prospectId;       // null until PP3 KYC validation succeeds
    public String leadLrn;
    public String status;           // DRAFT immediately after promotion

    public List<String> promotionWarnings;

    public static ProspectPromotionResponse of(UUID prospectUuid, String leadLrn, List<String> warnings) {
        ProspectPromotionResponse r = new ProspectPromotionResponse();
        r.prospectUuid      = prospectUuid;
        r.prospectId        = null;  // always null at promotion time — PP3 sets it later
        r.leadLrn           = leadLrn;
        r.status            = "DRAFT";
        r.promotionWarnings = warnings;
        return r;
    }
}
