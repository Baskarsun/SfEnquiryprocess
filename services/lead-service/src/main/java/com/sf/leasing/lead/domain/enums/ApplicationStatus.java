package com.sf.leasing.lead.domain.enums;

public enum ApplicationStatus {
    INITIATED,
    FRAUD_SCREENING,
    PENDING,                  // non-clear fraud result; manual review
    ELIGIBLE_FOR_APPLICATION, // fraud clear (or non-individual auto-advance)
    IN_APPRAISAL,             // CAM submitted
    SANCTIONED,               // CAM approved, sanction ID issued
    CLOSED
}
