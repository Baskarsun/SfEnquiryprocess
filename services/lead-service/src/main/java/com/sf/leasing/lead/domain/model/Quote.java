package com.sf.leasing.lead.domain.model;

import com.sf.leasing.lead.domain.enums.QuoteStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "quotes")
public class Quote extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "quote_id", unique = true, nullable = false, length = 30)
    public String quoteId;                  // QT-YYYY-NNNNNN

    @Column(name = "opportunity_id", nullable = false)
    public UUID opportunityId;

    @Column(name = "opportunity_business_id", length = 30)
    public String opportunityBusinessId;

    @Column(name = "quote_type", nullable = false, length = 20)
    public String quoteType;                // RACK_RATE | CUSTOMISED

    @Column(name = "version", nullable = false)
    public int version = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    public QuoteStatus status = QuoteStatus.DRAFT;

    // Asset & pricing
    @Column(name = "asset_cost", precision = 15, scale = 2)
    public BigDecimal assetCost;

    @Column(name = "product_model_price", precision = 15, scale = 2)
    public BigDecimal productModelPrice;

    @Column(name = "finance_amount", precision = 15, scale = 2)
    public BigDecimal financeAmount;

    @Column(name = "lease_type", length = 50)
    public String leaseType;

    @Column(name = "asset_make", length = 100)
    public String assetMake;

    @Column(name = "asset_model", length = 200)
    public String assetModel;

    @Column(name = "asset_year")
    public Integer assetYear;

    @Column(name = "rack_rate_percent", precision = 6, scale = 4)
    public BigDecimal rackRatePercent;

    @Column(name = "pricing_deviation")
    public boolean pricingDeviation = false;

    @Column(name = "advisory_message", length = 500)
    public String advisoryMessage;

    // Approval
    @Column(name = "approval_required")
    public boolean approvalRequired = false;

    @Column(name = "approved_by", length = 50)
    public String approvedBy;

    @Column(name = "approved_at")
    public LocalDateTime approvedAt;

    @Column(name = "approval_remarks", length = 500)
    public String approvalRemarks;

    @Column(name = "rejection_reason", length = 500)
    public String rejectionReason;

    // Sharing & lock
    @Column(name = "shared_at")
    public LocalDateTime sharedAt;

    @Column(name = "shared_by", length = 50)
    public String sharedBy;

    @Column(name = "locked_at")
    public LocalDateTime lockedAt;

    @Column(name = "locked_by", length = 50)
    public String lockedBy;

    // Unlock
    @Column(name = "unlock_requested_by", length = 50)
    public String unlockRequestedBy;

    @Column(name = "unlock_requested_at")
    public LocalDateTime unlockRequestedAt;

    @Column(name = "unlock_approved_by", length = 50)
    public String unlockApprovedBy;

    @Column(name = "unlock_approved_at")
    public LocalDateTime unlockApprovedAt;

    // Appraisal
    @Column(name = "appraisal_category", length = 100)
    public String appraisalCategory;

    // Audit
    @Column(name = "created_by", nullable = false, length = 50)
    public String createdBy;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_by", length = 50)
    public String updatedBy;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    public boolean isLocked()   { return status == QuoteStatus.LOCKED; }
    public boolean isDraft()    { return status == QuoteStatus.DRAFT; }
    public boolean isApproved() { return status == QuoteStatus.APPROVED; }
    public boolean isShared()   { return status == QuoteStatus.SHARED; }

    public static Quote findByQuoteId(String quoteId) {
        return find("quoteId", quoteId).firstResult();
    }

    public static List<Quote> findByOpportunity(UUID opportunityId) {
        return list("opportunityId ORDER BY version ASC", opportunityId);
    }

    public static int maxVersionForOpportunity(UUID opportunityId) {
        Long max = (Long) getEntityManager()
            .createQuery("SELECT MAX(q.version) FROM Quote q WHERE q.opportunityId = :oid")
            .setParameter("oid", opportunityId)
            .getSingleResult();
        return max == null ? 0 : max.intValue();
    }
}
