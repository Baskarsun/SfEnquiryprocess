package com.sf.leasing.lead.infrastructure.persistence;

import com.sf.leasing.lead.domain.model.ProspectKycValidation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProspectKycValidationRepository extends JpaRepository<ProspectKycValidation, UUID> {

    List<ProspectKycValidation> findByProspectId(UUID prospectId);
}
