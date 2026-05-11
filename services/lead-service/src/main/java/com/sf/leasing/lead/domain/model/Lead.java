package com.sf.leasing.lead.domain.model;

import com.sf.leasing.lead.domain.enums.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "leads")
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "lrn", unique = true, nullable = false, length = 30)
    public String lrn;

    @Column(name = "temp_customer_number", unique = true, nullable = false, length = 25)
    public String tempCustomerNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "lead_type", nullable = false, length = 20)
    public LeadType leadType;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_category", nullable = false, length = 30)
    public SourceCategory sourceCategory;

    @Column(name = "source_name", nullable = false, length = 200)
    public String sourceName;

    @Column(name = "company_known_as", length = 200)
    public String companyKnownAs;

    @Column(name = "contact_person", length = 200)
    public String contactPerson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    public LeadStatus status = LeadStatus.NEW;

    @Enumerated(EnumType.STRING)
    @Column(name = "temperature", length = 10)
    public LeadTemperature temperature = LeadTemperature.COLD;

    @Column(name = "temperature_suggested_by", length = 10)
    public String temperatureSuggestedBy = "SYSTEM";

    @Column(name = "temperature_reason_code", length = 50)
    public String temperatureReasonCode;

    @Column(name = "temperature_reason_text", length = 500)
    public String temperatureReasonText;

    @Enumerated(EnumType.STRING)
    @Column(name = "dedup_label", length = 30)
    public DedupLabel dedupLabel = DedupLabel.UNKNOWN;

    @Column(name = "ucic", length = 50)
    public String ucic;

    @Column(name = "existing_customer_codes")
    public String existingCustomerCodes;

    @Column(name = "assigned_branch_code", length = 20)
    public String assignedBranchCode;

    @Column(name = "assigned_team", length = 50)
    public String assignedTeam;

    @Column(name = "assigned_user_id", length = 50)
    public String assignedUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_hierarchy_level", length = 20)
    public HierarchyLevel assignmentHierarchyLevel = HierarchyLevel.CENTRAL;

    @Column(name = "call_attempts")
    public int callAttempts = 0;

    @Column(name = "message_attempts")
    public int messageAttempts = 0;

    @Column(name = "email_attempts")
    public int emailAttempts = 0;

    @Column(name = "closure_reason_code", length = 50)
    public String closureReasonCode;

    @Column(name = "closure_reason_text", length = 500)
    public String closureReasonText;

    @Column(name = "closure_date")
    public LocalDateTime closureDate;

    @Column(name = "closure_operator", length = 50)
    public String closureOperator;

    @Column(name = "latitude", precision = 10, scale = 8)
    public Double latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    public Double longitude;

    @Column(name = "geotag_captured_at")
    public LocalDateTime geotagCapturedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    public Channel channel;

    @Column(name = "sms_indicator", length = 1)
    public String smsIndicator = "N";

    @Column(name = "promoted_at")
    public LocalDateTime promotedAt;

    @Column(name = "prospect_id", length = 30)
    public String prospectId;

    @Column(name = "created_by", nullable = false, length = 50)
    public String createdBy;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_by", length = 50)
    public String updatedBy;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    @OneToMany(mappedBy = "lead", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("created_at ASC")
    public List<Applicant> applicants;

    @OneToMany(mappedBy = "lead", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("assigned_at ASC")
    public List<LeadAssignment> assignments;

    @OneToMany(mappedBy = "lead", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("interaction_timestamp ASC")
    public List<Interaction> interactions;

    public boolean isClosed() {
        return status == LeadStatus.CLOSED;
    }

    public boolean isPromoted() {
        return status == LeadStatus.PROMOTED;
    }

    public boolean isInProgress() {
        return status == LeadStatus.IN_PROGRESS;
    }
}
