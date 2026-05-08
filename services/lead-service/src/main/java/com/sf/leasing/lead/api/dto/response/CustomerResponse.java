package com.sf.leasing.lead.api.dto.response;

import com.sf.leasing.lead.domain.model.Customer;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response payload for enterprise customer record.
 */
public class CustomerResponse {

    public UUID id;
    public String customerId;
    public UUID prospectUuid;
    public String prospectBusinessId;
    public String applicationId;
    public String leadType;
    public String legalName;
    public String pan;
    public String gstin;
    public String registeredAddress;
    public String registeredPincode;
    public String ucic;
    public LocalDateTime kycCompletedAt;
    public LocalDateTime camApprovedAt;
    public String sanctionId;
    public String sanctionPackage;
    public String status;
    public String createdBy;
    public LocalDateTime createdAt;

    /** Gate status for UI display when creation is blocked. */
    public GateStatus gateStatus;

    public static CustomerResponse from(Customer c) {
        CustomerResponse r = new CustomerResponse();
        r.id                  = c.id;
        r.customerId          = c.customerId;
        r.prospectUuid        = c.prospectUuid;
        r.prospectBusinessId  = c.prospectBusinessId;
        r.applicationId       = c.applicationId;
        r.leadType            = c.leadType;
        r.legalName           = c.legalName;
        r.pan                 = maskPan(c.pan);
        r.gstin               = c.gstin;
        r.registeredAddress   = c.registeredAddress;
        r.registeredPincode   = c.registeredPincode;
        r.ucic                = c.ucic;
        r.kycCompletedAt      = c.kycCompletedAt;
        r.camApprovedAt       = c.camApprovedAt;
        r.sanctionId          = c.sanctionId;
        r.sanctionPackage     = c.sanctionPackage;
        r.status              = c.status;
        r.createdBy           = c.createdBy;
        r.createdAt           = c.createdAt;
        return r;
    }

    private static String maskPan(String pan) {
        if (pan == null || pan.length() < 5) return pan;
        return "XXXXX" + pan.substring(5);
    }

    public static class GateStatus {
        public boolean kycComplete;
        public boolean camApproved;
        public String blockReason;
    }
}
