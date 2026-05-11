package com.sf.leasing.lead.infrastructure.persistence;

import com.sf.leasing.lead.domain.model.Lineage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LineageRepository extends JpaRepository<Lineage, UUID> {

    Optional<Lineage> findByLeadLrn(String leadLrn);

    Optional<Lineage> findByProspectUuid(UUID prospectUuid);

    Optional<Lineage> findByCustomerUuid(UUID customerUuid);

    Optional<Lineage> findByApplicationUuid(UUID applicationUuid);

    Optional<Lineage> findByProspectBusinessId(String prospectBusinessId);

    Optional<Lineage> findByApplicationBusinessId(String applicationBusinessId);

    Optional<Lineage> findByCustomerId(String customerId);

    Optional<Lineage> findByOpportunityBusinessId(String opportunityBusinessId);
}
