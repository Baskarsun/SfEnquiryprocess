package com.sf.leasing.lead.domain.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Enterprise Customer master record.
 * PP8.1: Created only when KYC = Complete AND CAM = Approved simultaneously.
 * Customer ID format: CUST-YYYY-NNNNNN
 */
@Entity
@Table(name = "customers")
public class Customer extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "customer_id", unique = true, nullable = false, length = 30)
    public String customerId;               // CUST-YYYY-NNNNNN

    @Column(name = "prospect_uuid", nullable = false)
    public UUID prospectUuid;

    @Column(name = "prospect_business_id", length = 30)
    public String prospectBusinessId;

    @Column(name = "application_uuid", nullable = false)
    public UUID applicationUuid;

    @Column(name = "application_id", length = 30)
    public String applicationId;

    // Legal identity sourced from validated Prospect
    @Column(name = "lead_type", nullable = false, length = 20)
    public String leadType;                 // INDIVIDUAL | COMMERCIAL

    @Column(name = "legal_name", nullable = false, length = 500)
    public String legalName;

    @Column(name = "pan", length = 20)
    public String pan;

    @Column(name = "gstin", length = 30)
    public String gstin;

    @Column(name = "registered_address")
    public String registeredAddress;

    @Column(name = "registered_pincode", length = 10)
    public String registeredPincode;

    @Column(name = "ucic", length = 50)
    public String ucic;

    // Gate condition metadata
    @Column(name = "kyc_completed_at")
    public LocalDateTime kycCompletedAt;

    @Column(name = "cam_approved_at")
    public LocalDateTime camApprovedAt;

    @Column(name = "sanction_id", length = 100)
    public String sanctionId;

    @Column(name = "sanction_package", length = 200)
    public String sanctionPackage;

    @Column(name = "status", nullable = false, length = 30)
    public String status = "ACTIVE";        // ACTIVE | INACTIVE | SUSPENDED

    // Audit
    @Column(name = "created_by", nullable = false, length = 50)
    public String createdBy;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_by", length = 50)
    public String updatedBy;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    public static Customer findByCustomerId(String customerId) {
        return find("customerId", customerId).firstResult();
    }

    public static Customer findByProspectUuid(UUID prospectUuid) {
        return find("prospectUuid", prospectUuid).firstResult();
    }

    public static Customer findByApplicationUuid(UUID applicationUuid) {
        return find("applicationUuid", applicationUuid).firstResult();
    }
}
