package com.sf.leasing.lead.api.dto.request;

import com.sf.leasing.lead.domain.enums.LeadType;
import com.sf.leasing.lead.domain.enums.SourceCategory;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class CreateLeadRequest {

    @NotNull(message = "Lead type is required")
    public LeadType leadType;

    @NotNull(message = "Source category is required")
    public SourceCategory sourceCategory;

    public String sourceName;
    public String companyKnownAs;    // Commercial: mandatory
    public String contactPerson;     // Commercial: mandatory
    public List<ApplicantRequest> applicants;

    // Asset details (optional at Lead stage)
    public String assetCategory;
    public String assetClass;
    public Double assetCost;
    public Double leaseAmount;
    public String assetModelYear;

    // GPS (populated server-side from header for MOBILE channel)
    public Double latitude;
    public Double longitude;
}
