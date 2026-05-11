package com.sf.leasing.lead.infrastructure.persistence;

import com.sf.leasing.lead.domain.model.CamWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CamWorkflowRepository extends JpaRepository<CamWorkflow, UUID> {

    Optional<CamWorkflow> findByApplicationId(UUID applicationId);
}
