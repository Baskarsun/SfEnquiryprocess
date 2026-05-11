package com.sf.leasing.lead.infrastructure.persistence;

import com.sf.leasing.lead.domain.model.Opportunity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OpportunityRepository extends JpaRepository<Opportunity, UUID> {

    Optional<Opportunity> findByOpportunityId(String opportunityId);

    List<Opportunity> findByProspectUuid(UUID prospectUuid);

    Optional<Opportunity> findByProspectUuidAndLobTag(UUID prospectUuid, String lobTag);

    boolean existsByProspectUuidAndAssetCategoryAndAssetClassAndLobTag(
        UUID prospectUuid, String assetCategory, String assetClass, String lobTag);
}
