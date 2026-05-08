package com.sf.leasing.lead.domain.enums;

public enum QuoteStatus {
    DRAFT,
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    SHARED,
    LOCKED,
    UNLOCK_REQUESTED,
    SUPERSEDED   // earlier version replaced by a new version
}
