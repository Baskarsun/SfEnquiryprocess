package com.sf.leasing.lead.api.dto.response;

import com.sf.leasing.lead.domain.model.Prospect;

import java.time.LocalDateTime;
import java.util.UUID;

public class ProspectResponse {

    public UUID    id;
    public String  prospectId;        // null until KYC validated
    public String  leadLrn;
    public String  leadType;
    public String  status;

    // PAN: show first 2 + XXXXXX + last 2 (10 chars total)
    public String  panMasked;
    // GSTIN: show first 2 + XXXXXXXXXXX + last 2 (15 chars total)
    public String  gstinMasked;

    public boolean panValidated;
    public boolean gstinValidated;

    public String  legalName;
    public String  registeredAddress;
    public String  registeredPincode;

    public String  ucic;
    public String  existingCustomerCodes;
    public String  dedupLabel;

    public String  assignedBranchCode;
    public String  assignedTeam;
    public String  assignedUserId;
    public String  assignmentHierarchyLevel;

    public String  closureReasonCode;
    public String  closureReasonText;
    public LocalDateTime closedAt;

    public String  createdBy;
    public LocalDateTime createdAt;
    public String  updatedBy;
    public LocalDateTime updatedAt;

    public static ProspectResponse from(Prospect p) {
        ProspectResponse r = new ProspectResponse();
        r.id                      = p.id;
        r.prospectId              = p.prospectId;
        r.leadLrn                 = p.leadLrn;
        r.leadType                = p.leadType;
        r.status                  = p.status != null ? p.status.name() : null;
        r.panMasked               = maskPan(p.pan);
        r.gstinMasked             = maskGstin(p.gstin);
        r.panValidated            = p.panValidated;
        r.gstinValidated          = p.gstinValidated;
        r.legalName               = p.legalName;
        r.registeredAddress       = p.registeredAddress;
        r.registeredPincode       = p.registeredPincode;
        r.ucic                    = p.ucic;
        r.existingCustomerCodes   = p.existingCustomerCodes;
        r.dedupLabel              = p.dedupLabel;
        r.assignedBranchCode      = p.assignedBranchCode;
        r.assignedTeam            = p.assignedTeam;
        r.assignedUserId          = p.assignedUserId;
        r.assignmentHierarchyLevel = p.assignmentHierarchyLevel;
        r.closureReasonCode       = p.closureReasonCode;
        r.closureReasonText       = p.closureReasonText;
        r.closedAt                = p.closedAt;
        r.createdBy               = p.createdBy;
        r.createdAt               = p.createdAt;
        r.updatedBy               = p.updatedBy;
        r.updatedAt               = p.updatedAt;
        return r;
    }

    private static String maskPan(String pan) {
        if (pan == null || pan.length() < 4) return pan;
        return pan.substring(0, 2) + "X".repeat(pan.length() - 4) + pan.substring(pan.length() - 2);
    }

    private static String maskGstin(String gstin) {
        if (gstin == null || gstin.length() < 4) return gstin;
        return gstin.substring(0, 2) + "X".repeat(gstin.length() - 4) + gstin.substring(gstin.length() - 2);
    }
}
