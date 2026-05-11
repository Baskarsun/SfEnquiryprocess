package com.sf.leasing.lead.service;

import com.sf.leasing.lead.api.dto.request.AssignRoleRequest;
import com.sf.leasing.lead.api.dto.response.RoleAssignmentResponse;
import com.sf.leasing.lead.domain.exception.BusinessException;
import com.sf.leasing.lead.domain.exception.ErrorCodes;
import com.sf.leasing.lead.domain.model.Customer;
import com.sf.leasing.lead.domain.model.CustomerRoleAssignment;
import com.sf.leasing.lead.infrastructure.persistence.CustomerRepository;
import com.sf.leasing.lead.infrastructure.persistence.CustomerRoleAssignmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * PP8.3 — Customer role assignment service.
 *
 * Business rules:
 *   - Valid roles: LESSEE_INDIVIDUAL, LESSEE_CORPORATE, DEALER, VENDOR, DEPOSITOR
 *   - Checklist must be marked complete before role status transitions to ACTIVE
 *   - Duplicate role assignment for same customer is rejected
 *   - All assignment decisions are immutably audited (operator + timestamp)
 */
@Service
public class RoleAssignmentService {

    private static final Logger LOG = LoggerFactory.getLogger(RoleAssignmentService.class);

    private static final Set<String> VALID_ROLES = Set.of(
        "LESSEE_INDIVIDUAL", "LESSEE_CORPORATE", "DEALER", "VENDOR", "DEPOSITOR"
    );

    private final CustomerRepository customerRepository;
    private final CustomerRoleAssignmentRepository customerRoleAssignmentRepository;

    public RoleAssignmentService(CustomerRepository customerRepository,
                                  CustomerRoleAssignmentRepository customerRoleAssignmentRepository) {
        this.customerRepository = customerRepository;
        this.customerRoleAssignmentRepository = customerRoleAssignmentRepository;
    }

    // -------------------------------------------------------
    // PP8.3: Assign role
    // -------------------------------------------------------

    @Transactional
    public CustomerRoleAssignment assignRole(String customerId, AssignRoleRequest req) {
        Customer customer = resolveCustomer(customerId);

        // Validate role type
        if (req.roleType == null || !VALID_ROLES.contains(req.roleType.toUpperCase())) {
            throw new BusinessException(ErrorCodes.ROLE_TYPE_INVALID,
                "Invalid role type: " + req.roleType +
                ". Valid roles: " + String.join(", ", VALID_ROLES));
        }

        String normalised = req.roleType.toUpperCase();

        // Guard: no duplicate role assignment
        CustomerRoleAssignment existing =
            customerRoleAssignmentRepository.findByCustomerUuidAndRoleType(customer.id, normalised).orElse(null);
        if (existing != null) {
            throw new BusinessException(ErrorCodes.ROLE_ALREADY_ASSIGNED,
                "Role " + normalised + " is already assigned to customer: " + customerId);
        }

        // Determine active status based on checklist
        String status = req.checklistComplete ? "ACTIVE" : "PENDING";

        LocalDateTime now = LocalDateTime.now();
        CustomerRoleAssignment assignment = new CustomerRoleAssignment();
        assignment.customerUuid     = customer.id;
        assignment.customerId       = customer.customerId;
        assignment.roleType         = normalised;
        assignment.checklistComplete = req.checklistComplete;
        assignment.checklistItems   = serialiseChecklist(req.checklistItems);
        assignment.status           = status;
        assignment.remarks          = req.remarks;
        assignment.assignedBy       = req.assignedBy != null ? req.assignedBy : "SYSTEM";
        assignment.assignedAt       = now;
        customerRoleAssignmentRepository.save(assignment);

        LOG.info("Role assigned: CUST={} ROLE={} STATUS={} by={}",
            customerId, normalised, status, assignment.assignedBy);
        return assignment;
    }

    // -------------------------------------------------------
    // PP8.3: Complete checklist → activate role
    // -------------------------------------------------------

    @Transactional
    public CustomerRoleAssignment completeChecklist(String customerId, String roleType,
                                                     List<String> checklistItems, String updatedBy) {
        Customer customer = resolveCustomer(customerId);
        String normalised = roleType != null ? roleType.toUpperCase() : "";

        CustomerRoleAssignment assignment =
            customerRoleAssignmentRepository.findByCustomerUuidAndRoleType(customer.id, normalised)
                .orElseThrow(() -> new BusinessException(ErrorCodes.ROLE_TYPE_INVALID,
                    "Role " + normalised + " is not assigned to customer: " + customerId));

        assignment.checklistItems   = serialiseChecklist(checklistItems);
        assignment.checklistComplete = true;
        assignment.status           = "ACTIVE";
        assignment.updatedBy        = updatedBy;
        assignment.updatedAt        = LocalDateTime.now();

        LOG.info("Checklist completed: CUST={} ROLE={} by={}", customerId, normalised, updatedBy);
        return assignment;
    }

    // -------------------------------------------------------
    // Query
    // -------------------------------------------------------

    public List<RoleAssignmentResponse> getRoles(String customerId) {
        Customer customer = resolveCustomer(customerId);
        return customerRoleAssignmentRepository.findByCustomerUuid(customer.id)
            .stream()
            .map(RoleAssignmentResponse::from)
            .collect(Collectors.toList());
    }

    // -------------------------------------------------------
    // Private
    // -------------------------------------------------------

    private Customer resolveCustomer(String customerId) {
        return customerRepository.findByCustomerId(customerId)
            .orElseThrow(() -> new BusinessException(ErrorCodes.CUSTOMER_NOT_FOUND,
                "Customer not found: " + customerId));
    }

    private String serialiseChecklist(List<String> items) {
        if (items == null || items.isEmpty()) return "[]";
        return "[\"" + String.join("\",\"", items) + "\"]";
    }
}
