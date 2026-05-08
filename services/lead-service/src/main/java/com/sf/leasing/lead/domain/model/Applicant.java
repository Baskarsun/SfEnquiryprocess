package com.sf.leasing.lead.domain.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "applicants")
public class Applicant extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", nullable = false)
    public Lead lead;

    @Column(name = "applicant_label", nullable = false, length = 50)
    public String applicantLabel;  // MAIN APPLICANT | ADDL APPLICANT - 1 | ...

    @Column(name = "applicant_name", length = 200)
    public String applicantName;

    @Column(name = "gender", length = 1)
    public String gender;  // M | F | null (Non-Individual)

    @Column(name = "date_of_birth")
    public LocalDate dateOfBirth;

    @Column(name = "constitution_type", length = 30)
    public String constitutionType;  // INDIVIDUAL | NON_INDIVIDUAL

    @Column(name = "residential_type", length = 20)
    public String residentialType = "RESIDENT";

    @Column(name = "mobile", length = 15)
    public String mobile;

    @Column(name = "email", length = 200)
    public String email;

    @Column(name = "pan", length = 12)
    public String pan;

    @Column(name = "gstin", length = 20)
    public String gstin;

    @Column(name = "aadhaar_encrypted", length = 500)
    public String aadhaarEncrypted;

    @Column(name = "passport_number", length = 20)
    public String passportNumber;

    @Column(name = "passport_validity_date")
    public LocalDate passportValidityDate;

    @Column(name = "voter_id", length = 30)
    public String voterId;

    @Column(name = "driving_licence", length = 30)
    public String drivingLicence;

    @Column(name = "dan", length = 50)
    public String dan;

    @Column(name = "pan_exemption_flag", length = 1)
    public String panExemptionFlag = "N";

    @Column(name = "occupation", length = 100)
    public String occupation;

    @Column(name = "address_line1", length = 200)
    public String addressLine1;

    @Column(name = "address_line2", length = 200)
    public String addressLine2;

    @Column(name = "pincode", length = 10)
    public String pincode;

    @Column(name = "location_name", length = 200)
    public String locationName;

    @Column(name = "city", length = 100)
    public String city;

    @Column(name = "state", length = 100)
    public String state;

    @Column(name = "ucic_mapped", length = 50)
    public String ucicMapped;

    @Column(name = "dedup_result_code", length = 10)
    public String dedupResultCode;

    @Column(name = "risk_category", length = 30)
    public String riskCategory;

    @Column(name = "is_deceased")
    public boolean isDeceased = false;

    @Column(name = "sms_sent")
    public boolean smsSent = false;

    @Column(name = "applicant_status", length = 30)
    public String applicantStatus = "ACTIVE";

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    public boolean isMainApplicant() {
        return "MAIN APPLICANT".equals(applicantLabel);
    }
}
