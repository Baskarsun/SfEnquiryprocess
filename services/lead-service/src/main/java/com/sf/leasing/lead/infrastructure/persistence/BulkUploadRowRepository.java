package com.sf.leasing.lead.infrastructure.persistence;

import com.sf.leasing.lead.domain.model.BulkUploadRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BulkUploadRowRepository extends JpaRepository<BulkUploadRow, UUID> {
}
