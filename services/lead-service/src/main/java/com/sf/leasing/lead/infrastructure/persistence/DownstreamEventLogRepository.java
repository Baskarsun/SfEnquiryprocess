package com.sf.leasing.lead.infrastructure.persistence;

import com.sf.leasing.lead.domain.model.DownstreamEventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DownstreamEventLogRepository extends JpaRepository<DownstreamEventLog, UUID> {
}
