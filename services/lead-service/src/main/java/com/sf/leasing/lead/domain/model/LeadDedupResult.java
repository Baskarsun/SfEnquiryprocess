package com.sf.leasing.lead.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lead_dedup_results")
public class LeadDedupResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", nullable = false)
    public Lead lead;

    @Column(name = "applicant_id")
    public UUID applicantId;

    @Column(name = "check_type", nullable = false, length = 30)
    public String checkType;  // INTERNAL | EXTERNAL_PAN | UCIC | CAUTION_LIST

    @Column(name = "result_code", nullable = false, length = 20)
    public String resultCode;  // COM110 | COM111 | COM66 | ALLOWED | UNKNOWN

    @Column(name = "matched_entity_id", length = 50)
    public String matchedEntityId;

    @Column(name = "matched_entity_type", length = 30)
    public String matchedEntityType;  // LEAD | APPLICANT | EXTERNAL

    @Column(name = "checked_at", nullable = false)
    public LocalDateTime checkedAt = LocalDateTime.now();


}
