package com.sf.leasing.lead.domain.model;

import com.sf.leasing.lead.domain.enums.ApplicationStatus;
import com.sf.leasing.lead.domain.enums.CamStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "application_id", unique = true, nullable = false, length = 30)
    public String applicationId;            // APP-YYYY-NNNNNN

    @Column(name = "quote_id", nullable = false)
    public UUID quoteId;

    @Column(name = "opportunity_id", nullable = false)
    public UUID opportunityId;

    @Column(name = "prospect_uuid", nullable = false)
    public UUID prospectUuid;

    @Column(name = "prospect_business_id", length = 30)
    public String prospectBusinessId;

    @Column(name = "is_individual", nullable = false)
    public boolean individual = true;

    @Column(name = "lease_type", length = 50)
    public String leaseType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    public ApplicationStatus status = ApplicationStatus.INITIATED;

    @Column(name = "kyc_status", nullable = false, length = 30)
    public String kycStatus = "PENDING";    // PENDING | IN_PROGRESS | COMPLETE

    @Enumerated(EnumType.STRING)
    @Column(name = "cam_status", nullable = false, length = 30)
    public CamStatus camStatus = CamStatus.NOT_STARTED;

    @Column(name = "sanction_id", length = 100)
    public String sanctionId;

    @Column(name = "sanction_package", length = 200)
    public String sanctionPackage;

    // Fraud screening
    @Column(name = "fraud_status", nullable = false, length = 30)
    public String fraudStatus = "PENDING";  // PENDING | CLEAR | NON_CLEAR

    @Column(name = "fraud_message")
    public String fraudMessage;

    @Column(name = "fraud_screened_at")
    public LocalDateTime fraudScreenedAt;

    // CIBIL
    @Column(name = "cibil_submitted")
    public boolean cibilSubmitted = false;

    @Column(name = "cibil_reference", length = 100)
    public String cibilReference;

    @Column(name = "cibil_requested_at")
    public LocalDateTime cibilRequestedAt;

    // Caution
    @Column(name = "caution_screened")
    public boolean cautionScreened = false;

    @Column(name = "caution_blocked")
    public boolean cautionBlocked = false;

    @Column(name = "caution_screened_at")
    public LocalDateTime cautionScreenedAt;

    // Eligibility
    @Column(name = "eligible_for_application")
    public boolean eligibleForApplication = false;

    @Column(name = "sanction_bypass")
    public boolean sanctionBypass = false;

    @Column(name = "welcome_comm_sent")
    public boolean welcomeCommSent = false;

    // Modification controls
    @Column(name = "modification_blocked")
    public boolean modificationBlocked = false;

    @Column(name = "modification_block_reason", length = 10)
    public String modificationBlockReason;

    // Loaded by ApplicationService.buildResponse() — not persisted
    @Transient
    public List<ApplicationDocument> documents;

    // Audit
    @Column(name = "created_by", nullable = false, length = 50)
    public String createdBy;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_by", length = 50)
    public String updatedBy;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

}
