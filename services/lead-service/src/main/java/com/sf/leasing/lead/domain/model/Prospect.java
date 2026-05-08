package com.sf.leasing.lead.domain.model;

import com.sf.leasing.lead.domain.enums.ProspectStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "prospects")
public class Prospect extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "prospect_id", unique = true, length = 30)
    public String prospectId;           // PR-YYYY-NNNNNN — null until validation succeeds

    @Column(name = "lead_lrn", nullable = false, length = 30)
    public String leadLrn;

    @Column(name = "lead_id", nullable = false)
    public UUID leadId;

    @Column(name = "lead_type", nullable = false, length = 20)
    public String leadType;             // INDIVIDUAL | COMMERCIAL

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    public ProspectStatus status = ProspectStatus.DRAFT;

    // KYC identifiers
    @Column(name = "pan", length = 12)
    public String pan;

    @Column(name = "gstin", length = 20)
    public String gstin;

    @Column(name = "pan_validated")
    public boolean panValidated = false;

    @Column(name = "gstin_validated")
    public boolean gstinValidated = false;

    // Validated legal identity
    @Column(name = "legal_name", length = 500)
    public String legalName;

    @Column(name = "registered_address")
    public String registeredAddress;

    @Column(name = "registered_pincode", length = 10)
    public String registeredPincode;

    @Column(name = "validation_reference", length = 100)
    public String validationReference;

    @Column(name = "validation_source", length = 50)
    public String validationSource;

    @Column(name = "validation_timestamp")
    public LocalDateTime validationTimestamp;

    // Manual validation override
    @Column(name = "validation_override_reason", length = 500)
    public String validationOverrideReason;

    @Column(name = "validation_override_by", length = 50)
    public String validationOverrideBy;

    @Column(name = "validation_override_at")
    public LocalDateTime validationOverrideAt;

    // UCIC / Dedup
    @Column(name = "ucic", length = 50)
    public String ucic;

    @Column(name = "existing_customer_codes")
    public String existingCustomerCodes;

    @Column(name = "dedup_label", length = 30)
    public String dedupLabel;

    // Assignment
    @Column(name = "assigned_branch_code", length = 20)
    public String assignedBranchCode;

    @Column(name = "assigned_team", length = 50)
    public String assignedTeam;

    @Column(name = "assigned_user_id", length = 50)
    public String assignedUserId;

    @Column(name = "assignment_hierarchy_level", length = 20)
    public String assignmentHierarchyLevel;

    // Closure
    @Column(name = "closure_reason_code", length = 50)
    public String closureReasonCode;

    @Column(name = "closure_reason_text", length = 500)
    public String closureReasonText;

    @Column(name = "closed_at")
    public LocalDateTime closedAt;

    @Column(name = "closed_by", length = 50)
    public String closedBy;

    // Audit
    @Column(name = "created_by", nullable = false, length = 50)
    public String createdBy;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_by", length = 50)
    public String updatedBy;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    // Finders
    public static Prospect findByProspectId(String prospectId) {
        return find("prospectId", prospectId).firstResult();
    }

    public static Prospect findByLeadLrn(String lrn) {
        return find("leadLrn", lrn).firstResult();
    }

    public boolean isDraft()    { return status == ProspectStatus.DRAFT; }
    public boolean isValidated(){ return status == ProspectStatus.VALIDATED; }
    public boolean isActive()   { return status == ProspectStatus.ACTIVE; }
    public boolean isClosed()   { return status == ProspectStatus.CLOSED; }
}
