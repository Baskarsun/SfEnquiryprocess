package com.sf.leasing.lead.api.dto.response;

import com.sf.leasing.lead.domain.model.Lineage;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Full immutable lineage chain response.
 * PP8.2: Lead LRN → Prospect ID → Opportunity ID → Quote ID → Application ID → Customer ID.
 */
public class LineageResponse {

    public UUID lineageId;

    // Lead
    public String leadLrn;
    public UUID leadId;

    // Prospect
    public UUID prospectUuid;
    public String prospectBusinessId;

    // Opportunity
    public UUID opportunityUuid;
    public String opportunityBusinessId;

    // Quote
    public UUID quoteUuid;
    public String quoteBusinessId;

    // Application
    public UUID applicationUuid;
    public String applicationBusinessId;

    // Customer
    public UUID customerUuid;
    public String customerId;

    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;

    public static LineageResponse from(Lineage l) {
        LineageResponse r = new LineageResponse();
        r.lineageId              = l.id;
        r.leadLrn                = l.leadLrn;
        r.leadId                 = l.leadId;
        r.prospectUuid           = l.prospectUuid;
        r.prospectBusinessId     = l.prospectBusinessId;
        r.opportunityUuid        = l.opportunityUuid;
        r.opportunityBusinessId  = l.opportunityBusinessId;
        r.quoteUuid              = l.quoteUuid;
        r.quoteBusinessId        = l.quoteBusinessId;
        r.applicationUuid        = l.applicationUuid;
        r.applicationBusinessId  = l.applicationBusinessId;
        r.customerUuid           = l.customerUuid;
        r.customerId             = l.customerId;
        r.createdAt              = l.createdAt;
        r.updatedAt              = l.updatedAt;
        return r;
    }
}
