package com.sf.leasing.lead.infrastructure.persistence;

import com.sf.leasing.lead.domain.model.TemperatureAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TemperatureAuditRepository extends JpaRepository<TemperatureAudit, UUID> {

    List<TemperatureAudit> findByLeadId(UUID leadId);
}
