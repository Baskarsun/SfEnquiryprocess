package com.sf.leasing.lead.api.dto.request;

import com.sf.leasing.lead.domain.enums.LeadType;
import com.sf.leasing.lead.domain.enums.SourceCategory;

import java.util.List;

/**
 * Single lead record for API bulk ingestion (LP3.2).
 * The bulk API accepts a list of these objects.
 */
public class BulkIngestRequest {

    public LeadType leadType;
    public SourceCategory sourceCategory;
    public String sourceName;
    public String companyKnownAs;
    public String contactPerson;

    public List<ApplicantRequest> applicants;
}
