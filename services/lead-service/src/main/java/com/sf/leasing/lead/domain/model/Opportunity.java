package com.sf.leasing.lead.domain.model;

import com.sf.leasing.lead.domain.enums.OpportunityStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "opportunities")
public class Opportunity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "opportunity_id", unique = true, nullable = false, length = 30)
    public String opportunityId;            // OPP-YYYY-NNNNNN

    @Column(name = "prospect_uuid", nullable = false)
    public UUID prospectUuid;

    @Column(name = "prospect_business_id", length = 30)
    public String prospectBusinessId;

    @Column(name = "asset_category", nullable = false, length = 100)
    public String assetCategory;

    @Column(name = "asset_class", nullable = false, length = 100)
    public String assetClass;

    @Column(name = "lob_tag", nullable = false, length = 50)
    public String lobTag;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    public OpportunityStatus status = OpportunityStatus.OPEN;

    @Column(name = "created_by", nullable = false, length = 50)
    public String createdBy;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_by", length = 50)
    public String updatedBy;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

}
