package com.sf.leasing.lead.infrastructure.persistence;

import com.sf.leasing.lead.domain.model.BulkUploadJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BulkUploadJobRepository extends JpaRepository<BulkUploadJob, UUID> {
}
