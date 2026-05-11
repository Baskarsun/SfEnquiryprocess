package com.sf.leasing.lead.infrastructure.persistence;

import com.sf.leasing.lead.domain.model.ProspectAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProspectAssignmentRepository extends JpaRepository<ProspectAssignment, UUID> {

    List<ProspectAssignment> findByProspectId(UUID prospectId);
}
