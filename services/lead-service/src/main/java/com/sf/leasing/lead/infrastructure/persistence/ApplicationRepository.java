package com.sf.leasing.lead.infrastructure.persistence;

import com.sf.leasing.lead.domain.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    Optional<Application> findByApplicationId(String applicationId);

    List<Application> findByProspectUuid(UUID prospectUuid);

    Optional<Application> findByQuoteId(UUID quoteId);
}
