package com.sf.leasing.lead.domain.enums;

public enum ProspectStatus {
    DRAFT,           // Created from lead promotion; pending PAN/GSTIN validation
    VALIDATED,       // PAN/GSTIN validated; Prospect ID assigned
    ACTIVE,          // Assigned to field officer; interactions underway
    IN_APPRAISAL,    // Opportunity created (Phase 4)
    CUSTOMER_CREATED, // Customer gate satisfied (Phase 5)
    CLOSED
}
