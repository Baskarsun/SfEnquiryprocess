package com.sf.leasing.lead.infrastructure.persistence;

import com.sf.leasing.lead.domain.model.Prospect;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProspectRepository extends JpaRepository<Prospect, UUID> {

    Optional<Prospect> findByProspectId(String prospectId);

    Optional<Prospect> findByLeadLrn(String leadLrn);
}
