package com.sf.leasing.lead.infrastructure.persistence;

import com.sf.leasing.lead.domain.model.Applicant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApplicantRepository extends JpaRepository<Applicant, UUID> {

    List<Applicant> findByLeadId(UUID leadId);
}
