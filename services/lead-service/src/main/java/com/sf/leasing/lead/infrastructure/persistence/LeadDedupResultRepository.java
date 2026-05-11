package com.sf.leasing.lead.infrastructure.persistence;

import com.sf.leasing.lead.domain.model.LeadDedupResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LeadDedupResultRepository extends JpaRepository<LeadDedupResult, UUID> {

    List<LeadDedupResult> findByLeadId(UUID leadId);
}
