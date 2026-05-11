package com.sf.leasing.lead.infrastructure.persistence;

import com.sf.leasing.lead.domain.model.CustomerRoleAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRoleAssignmentRepository extends JpaRepository<CustomerRoleAssignment, UUID> {

    List<CustomerRoleAssignment> findByCustomerUuid(UUID customerUuid);

    Optional<CustomerRoleAssignment> findByCustomerUuidAndRoleType(UUID customerUuid, String roleType);
}
