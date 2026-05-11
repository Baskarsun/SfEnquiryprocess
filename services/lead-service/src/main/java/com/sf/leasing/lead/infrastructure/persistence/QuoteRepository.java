package com.sf.leasing.lead.infrastructure.persistence;

import com.sf.leasing.lead.domain.model.Quote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuoteRepository extends JpaRepository<Quote, UUID> {

    Optional<Quote> findByQuoteId(String quoteId);

    List<Quote> findByOpportunityIdOrderByVersionAsc(UUID opportunityId);

    @Query("SELECT COALESCE(MAX(q.version), 0) FROM Quote q WHERE q.opportunityId = :opportunityId")
    int maxVersionForOpportunity(@Param("opportunityId") UUID opportunityId);
}
