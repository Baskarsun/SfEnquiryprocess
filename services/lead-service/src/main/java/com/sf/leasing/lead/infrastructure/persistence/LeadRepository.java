package com.sf.leasing.lead.infrastructure.persistence;

import com.sf.leasing.lead.domain.enums.LeadStatus;
import com.sf.leasing.lead.domain.enums.LeadTemperature;
import com.sf.leasing.lead.domain.model.Lead;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeadRepository extends JpaRepository<Lead, UUID> {

    Optional<Lead> findByLrn(String lrn);

    Optional<Lead> findByTempCustomerNumber(String tempCustomerNumber);

    @Query("SELECT l FROM Lead l WHERE " +
           "(:status IS NULL OR l.status = :status) AND " +
           "(:assignedTo IS NULL OR l.assignedUserId = :assignedTo) AND " +
           "(:branchCode IS NULL OR l.assignedBranchCode = :branchCode) AND " +
           "(:temperature IS NULL OR l.temperature = :temperature)")
    Page<Lead> search(
        @Param("status")      LeadStatus status,
        @Param("assignedTo")  String assignedTo,
        @Param("branchCode")  String branchCode,
        @Param("temperature") LeadTemperature temperature,
        Pageable pageable
    );
}
