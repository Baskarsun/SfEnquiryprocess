package com.sf.leasing.lead.domain.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "prospect_kyc_validations")
public class ProspectKycValidation extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prospect_id", nullable = false)
    public Prospect prospect;

    @Column(name = "validation_type", nullable = false, length = 10)
    public String validationType;     // PAN | GSTIN

    @Column(name = "identifier", nullable = false, length = 20)
    public String identifier;         // the PAN or GSTIN value that was validated

    @Column(name = "status", nullable = false, length = 20)
    public String status;             // SUCCESS | FAILED | OVERRIDDEN

    @Column(name = "legal_name", length = 500)
    public String legalName;

    @Column(name = "registered_address")
    public String registeredAddress;

    @Column(name = "derived_pan", length = 12)
    public String derivedPan;         // for GSTIN validation (chars 3-12 of GSTIN)

    @Column(name = "validation_reference", length = 100)
    public String validationReference;

    @Column(name = "validation_source", length = 50)
    public String validationSource;

    @Column(name = "validated_at", nullable = false)
    public LocalDateTime validatedAt = LocalDateTime.now();

    @Column(name = "override_reason_code", length = 50)
    public String overrideReasonCode;

    @Column(name = "override_reason_text", length = 500)
    public String overrideReasonText;

    @Column(name = "override_by", length = 50)
    public String overrideBy;

    @Column(name = "override_at")
    public LocalDateTime overrideAt;

    public static List<ProspectKycValidation> findByProspectId(UUID prospectId) {
        return find("prospect.id", prospectId).list();
    }
}
