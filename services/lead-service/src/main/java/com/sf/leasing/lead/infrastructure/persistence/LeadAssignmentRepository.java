package com.sf.leasing.lead.infrastructure.persistence;

import com.sf.leasing.lead.domain.model.LeadAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LeadAssignmentRepository extends JpaRepository<LeadAssignment, UUID> {

    List<LeadAssignment> findByLeadId(UUID leadId);
}
