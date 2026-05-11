package com.sf.leasing.lead.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable lineage record linking entities in the Lead-to-Customer chain.
 * Phase 3: Lead LRN → Prospect UUID + Prospect Business ID.
 * Phase 5: Extended with Opportunity, Quote, Application, Customer IDs.
 */
@Entity
@Table(name = "lineage")
public class Lineage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "lead_lrn", nullable = false, length = 30)
    public String leadLrn;

    @Column(name = "lead_id", nullable = false)
    public UUID leadId;

    @Column(name = "prospect_uuid")
    public UUID prospectUuid;

    @Column(name = "prospect_business_id", length = 30)
    public String prospectBusinessId;   // PR-YYYY-NNNNNN (set after validation)

    // Phase 5 additions — set as each stage completes
    @Column(name = "opportunity_uuid")
    public UUID opportunityUuid;

    @Column(name = "opportunity_business_id", length = 30)
    public String opportunityBusinessId; // OPP-YYYY-NNNNNN

    @Column(name = "quote_uuid")
    public UUID quoteUuid;

    @Column(name = "quote_business_id", length = 30)
    public String quoteBusinessId;      // QT-YYYY-NNNNNN

    @Column(name = "application_uuid")
    public UUID applicationUuid;

    @Column(name = "application_business_id", length = 30)
    public String applicationBusinessId; // APP-YYYY-NNNNNN

    @Column(name = "customer_uuid")
    public UUID customerUuid;

    @Column(name = "customer_id", length = 30)
    public String customerId;           // CUST-YYYY-NNNNNN

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;
}
