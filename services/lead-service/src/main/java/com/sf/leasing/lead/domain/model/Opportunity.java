package com.sf.leasing.lead.domain.model;

import com.sf.leasing.lead.domain.enums.OpportunityStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "opportunities")
public class Opportunity extends PanacheEntityBase {

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

    @OneToMany
    @JoinColumn(name = "opportunity_id", referencedColumnName = "id")
    public List<Quote> quotes;

    public static Opportunity findByOpportunityId(String opportunityId) {
        return find("opportunityId", opportunityId).firstResult();
    }

    public static List<Opportunity> findByProspect(UUID prospectUuid) {
        return list("prospectUuid", prospectUuid);
    }

    public static Opportunity findByProspectAndLob(UUID prospectUuid, String lobTag) {
        return find("prospectUuid = ?1 AND lobTag = ?2", prospectUuid, lobTag).firstResult();
    }

    public static boolean existsForProspectAndCategory(UUID prospectUuid,
                                                        String assetCategory,
                                                        String assetClass,
                                                        String lobTag) {
        return count("prospectUuid = ?1 AND assetCategory = ?2 AND assetClass = ?3 AND lobTag = ?4",
            prospectUuid, assetCategory, assetClass, lobTag) > 0;
    }
}
