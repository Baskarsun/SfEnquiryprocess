package com.sf.leasing.lead.api.dto.request;

public class LockUnlockQuoteRequest {

    /** LOCK, REQUEST_UNLOCK, APPROVE_UNLOCK */
    public String action;

    public String requestedBy;
    public String approvedBy;
    public String remarks;
}
