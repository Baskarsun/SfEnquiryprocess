package com.sf.leasing.lead.infrastructure.persistence;

import com.sf.leasing.lead.domain.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByCustomerId(String customerId);

    Optional<Customer> findByProspectUuid(UUID prospectUuid);

    Optional<Customer> findByApplicationUuid(UUID applicationUuid);
}
