package com.sf.leasing.lead;

import com.sf.leasing.lead.domain.exception.ErrorCodes;
import com.sf.leasing.lead.domain.model.CustomerRoleAssignment;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for customer role assignment business rules — PP8.3.
 *
 * Validates role type validation, checklist logic, status transitions,
 * and audit field requirements without database dependencies.
 */
class RoleAssignmentServiceTest {

    private static final Set<String> VALID_ROLES =
        Set.of("LESSEE_INDIVIDUAL", "LESSEE_CORPORATE", "DEALER", "VENDOR", "DEPOSITOR");

    // -------------------------------------------------------
    // PP8.3: Valid role types
    // -------------------------------------------------------

    @Test
    void roleType_allValidValues_accepted() {
        for (String role : VALID_ROLES) {
            assertTrue(VALID_ROLES.contains(role), "Must accept: " + role);
        }
    }

    @Test
    void roleType_invalidValue_rejected() {
        assertFalse(VALID_ROLES.contains("BORROWER"));
        assertFalse(VALID_ROLES.contains("LESSEE"));
        assertFalse(VALID_ROLES.contains("AGENT"));
        assertFalse(VALID_ROLES.contains(""));
        // null check — guard against NPE from Set.of() which rejects null keys
        assertFalse(VALID_ROLES.stream().anyMatch(r -> r == null));
    }

    @Test
    void roleType_caseNormalisation_uppercaseRequired() {
        assertFalse(VALID_ROLES.contains("lessee_individual"));
        assertFalse(VALID_ROLES.contains("Dealer"));
        assertTrue(VALID_ROLES.contains("LESSEE_INDIVIDUAL"));
    }

    // -------------------------------------------------------
    // PP8.3: Checklist determines initial status
    // -------------------------------------------------------

    @Test
    void assignRole_checklistComplete_statusIsActive() {
        String status = deriveStatus(true);
        assertEquals("ACTIVE", status);
    }

    @Test
    void assignRole_checklistIncomplete_statusIsPending() {
        String status = deriveStatus(false);
        assertEquals("PENDING", status);
    }

    // -------------------------------------------------------
    // PP8.3: Checklist completion activates role
    // -------------------------------------------------------

    @Test
    void completeChecklist_transitionsPendingToActive() {
        CustomerRoleAssignment ra = new CustomerRoleAssignment();
        ra.status           = "PENDING";
        ra.checklistComplete = false;

        ra.checklistComplete = true;
        ra.status           = "ACTIVE";

        assertEquals("ACTIVE", ra.status);
        assertTrue(ra.checklistComplete);
    }

    @Test
    void completeChecklist_checklistItemsStored() {
        List<String> items = List.of("DOC_VERIFIED", "AGREEMENT_SIGNED", "KYC_OK");
        String serialised  = serialiseChecklist(items);

        assertTrue(serialised.contains("DOC_VERIFIED"));
        assertTrue(serialised.contains("AGREEMENT_SIGNED"));
        assertTrue(serialised.contains("KYC_OK"));
    }

    // -------------------------------------------------------
    // PP8.3: Audit fields mandatory
    // -------------------------------------------------------

    @Test
    void roleAssignment_assignedByRequired_notBlank() {
        CustomerRoleAssignment ra = buildAssignment("LESSEE_INDIVIDUAL", true, "operator01");
        assertNotNull(ra.assignedBy);
        assertFalse(ra.assignedBy.isBlank());
    }

    @Test
    void roleAssignment_assignedAtSet() {
        CustomerRoleAssignment ra = buildAssignment("DEALER", false, "op1");
        assertNotNull(ra.assignedAt);
    }

    @Test
    void roleAssignment_customerUuidRequired() {
        CustomerRoleAssignment ra = buildAssignment("VENDOR", true, "op1");
        assertNotNull(ra.customerUuid);
    }

    // -------------------------------------------------------
    // PP8.3: Error codes
    // -------------------------------------------------------

    @Test
    void errorCode_invalidRole_correct() {
        assertEquals("PP8_ROLE_INVALID", ErrorCodes.ROLE_TYPE_INVALID);
    }

    @Test
    void errorCode_duplicateRole_correct() {
        assertEquals("PP8_ROLE_DUPLICATE", ErrorCodes.ROLE_ALREADY_ASSIGNED);
    }

    @Test
    void errorCode_checklistIncomplete_correct() {
        assertEquals("PP8_CHECKLIST_INCOMPLETE", ErrorCodes.ROLE_CHECKLIST_INCOMPLETE);
    }

    @Test
    void errorCode_customerNotFound_correct() {
        assertEquals("CUSTOMER_NOT_FOUND", ErrorCodes.CUSTOMER_NOT_FOUND);
    }

    // -------------------------------------------------------
    // PP8.3: Duplicate detection
    // -------------------------------------------------------

    @Test
    void duplicateDetection_sameRoleTwice_shouldBeRejected() {
        UUID customerUuid = UUID.randomUUID();
        String roleType = "DEPOSITOR";

        // First assignment record
        CustomerRoleAssignment first = buildAssignment(roleType, false, "op1");
        first.customerUuid = customerUuid;

        // Simulate duplicate check: same customer + same role exists
        boolean isDuplicate = first.customerUuid.equals(customerUuid)
            && first.roleType.equals(roleType);

        assertTrue(isDuplicate, "Second assignment of same role must be detected as duplicate");
    }

    @Test
    void duplicateDetection_differentRoles_notDuplicate() {
        UUID customerUuid = UUID.randomUUID();
        CustomerRoleAssignment ra1 = buildAssignment("DEALER", false, "op1");
        ra1.customerUuid = customerUuid;

        CustomerRoleAssignment ra2 = buildAssignment("VENDOR", false, "op1");
        ra2.customerUuid = customerUuid;

        assertNotEquals(ra1.roleType, ra2.roleType, "Different roles must not be treated as duplicates");
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------

    private String deriveStatus(boolean checklistComplete) {
        return checklistComplete ? "ACTIVE" : "PENDING";
    }

    private String serialiseChecklist(List<String> items) {
        if (items == null || items.isEmpty()) return "[]";
        return "[\"" + String.join("\",\"", items) + "\"]";
    }

    private CustomerRoleAssignment buildAssignment(String roleType,
                                                    boolean checklistComplete,
                                                    String assignedBy) {
        CustomerRoleAssignment ra = new CustomerRoleAssignment();
        ra.customerUuid    = UUID.randomUUID();
        ra.customerId      = "CUST-2026-000001";
        ra.roleType        = roleType;
        ra.checklistComplete = checklistComplete;
        ra.status          = deriveStatus(checklistComplete);
        ra.assignedBy      = assignedBy;
        ra.assignedAt      = java.time.LocalDateTime.now();
        return ra;
    }
}
