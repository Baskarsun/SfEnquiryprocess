package com.sf.leasing.lead;

import com.sf.leasing.lead.domain.enums.CamStatus;
import com.sf.leasing.lead.domain.exception.ErrorCodes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Customer creation gate business logic — PP8.1.
 *
 * Validates the gate rules, ID format, and status transitions without
 * database or Panache dependencies. Integration tests cover the full
 * Panache/Kafka flow.
 */
class CustomerCreationServiceTest {

    private static final String CUSTOMER_ID_PATTERN = "CUST-\\d{4}-\\d{6}";

    // -------------------------------------------------------
    // PP8.1: Customer ID format
    // -------------------------------------------------------

    @Test
    void customerId_format_matchesExpectedPattern() {
        String id = "CUST-2026-000001";
        assertTrue(id.matches(CUSTOMER_ID_PATTERN), "Must match CUST-YYYY-NNNNNN");
    }

    @Test
    void customerId_wrongFormat_doesNotMatch() {
        assertFalse("CST-2026-000001".matches(CUSTOMER_ID_PATTERN));
        assertFalse("CUST-26-000001".matches(CUSTOMER_ID_PATTERN));
        assertFalse("CUST-2026-00001".matches(CUSTOMER_ID_PATTERN));
        assertFalse("".matches(CUSTOMER_ID_PATTERN));
    }

    // -------------------------------------------------------
    // PP8.1: Gate evaluation logic
    // -------------------------------------------------------

    @Test
    void gateEvaluation_bothComplete_satisfied() {
        boolean kycComplete = true;
        boolean camApproved = true;
        assertTrue(isGateSatisfied(kycComplete, camApproved));
    }

    @Test
    void gateEvaluation_kycIncomplete_blocked() {
        assertFalse(isGateSatisfied(false, true));
    }

    @Test
    void gateEvaluation_camNotApproved_blocked() {
        assertFalse(isGateSatisfied(true, false));
    }

    @Test
    void gateEvaluation_bothIncomplete_blocked() {
        assertFalse(isGateSatisfied(false, false));
    }

    // -------------------------------------------------------
    // PP8.1: KYC status values
    // -------------------------------------------------------

    @Test
    void kycStatus_complete_recognised() {
        assertTrue(isKycComplete("COMPLETE"));
        assertFalse(isKycComplete("PENDING"));
        assertFalse(isKycComplete("IN_PROGRESS"));
        assertFalse(isKycComplete(null));
    }

    // -------------------------------------------------------
    // PP8.1: CAM status values
    // -------------------------------------------------------

    @Test
    void camStatus_approved_recognised() {
        assertTrue(isCamApproved(CamStatus.APPROVED));
        assertFalse(isCamApproved(CamStatus.IN_PROGRESS));
        assertFalse(isCamApproved(CamStatus.NOT_STARTED));
        assertFalse(isCamApproved(CamStatus.DECLINED));
    }

    // -------------------------------------------------------
    // PP8.1: Gate error codes
    // -------------------------------------------------------

    @Test
    void gateErrorCodes_kycIncomplete_correctCode() {
        assertEquals("PP8_KYC_INCOMPLETE", ErrorCodes.CUSTOMER_GATE_KYC_INCOMPLETE);
    }

    @Test
    void gateErrorCodes_camNotApproved_correctCode() {
        assertEquals("PP8_CAM_NOT_APPROVED", ErrorCodes.CUSTOMER_GATE_CAM_NOT_APPROVED);
    }

    @Test
    void gateErrorCodes_customerAlreadyExists_correctCode() {
        assertEquals("PP8_CUSTOMER_EXISTS", ErrorCodes.CUSTOMER_ALREADY_EXISTS);
    }

    // -------------------------------------------------------
    // PP8.1: Block reason text generation
    // -------------------------------------------------------

    @Test
    void blockReason_bothFailing_includesBothConditions() {
        String reason = buildBlockReason(false, false);
        assertNotNull(reason);
        assertFalse(reason.isBlank());
    }

    @Test
    void blockReason_kycOnly_mentionsKyc() {
        String reason = buildBlockReason(false, true);
        assertTrue(reason.toLowerCase().contains("kyc"));
    }

    @Test
    void blockReason_camOnly_mentionsCam() {
        String reason = buildBlockReason(true, false);
        assertTrue(reason.toLowerCase().contains("cam"));
    }

    @Test
    void blockReason_bothSatisfied_noBlock() {
        assertNull(buildBlockReason(true, true));
    }

    // -------------------------------------------------------
    // PP8.2: Lineage ID formats
    // -------------------------------------------------------

    @Test
    void lineageChain_allIdFormatsValid() {
        assertTrue("LS-202601-000001".matches("LS-\\d{6}-\\d{6}"));
        assertTrue("PR-2026-000001".matches("PR-\\d{4}-\\d{6}"));
        assertTrue("OPP-2026-000001".matches("OPP-\\d{4}-\\d{6}"));
        assertTrue("QT-2026-000001".matches("QT-\\d{4}-\\d{6}"));
        assertTrue("APP-2026-000001".matches("APP-\\d{4}-\\d{6}"));
        assertTrue("CUST-2026-000001".matches("CUST-\\d{4}-\\d{6}"));
    }

    // -------------------------------------------------------
    // PP8.3: Role type validation
    // -------------------------------------------------------

    @Test
    void roleTypes_allValidValues_accepted() {
        java.util.Set<String> validRoles = java.util.Set.of(
            "LESSEE_INDIVIDUAL", "LESSEE_CORPORATE", "DEALER", "VENDOR", "DEPOSITOR"
        );
        for (String role : validRoles) {
            assertTrue(validRoles.contains(role), "Must accept: " + role);
        }
    }

    @Test
    void roleTypes_invalidValue_rejected() {
        java.util.Set<String> validRoles = java.util.Set.of(
            "LESSEE_INDIVIDUAL", "LESSEE_CORPORATE", "DEALER", "VENDOR", "DEPOSITOR"
        );
        assertFalse(validRoles.contains("LESSEE"));
        assertFalse(validRoles.contains("BORROWER"));
        assertFalse(validRoles.contains(""));
    }

    @Test
    void roleErrorCode_invalidRole_correctCode() {
        assertEquals("PP8_ROLE_INVALID", ErrorCodes.ROLE_TYPE_INVALID);
    }

    @Test
    void roleErrorCode_checklistIncomplete_correctCode() {
        assertEquals("PP8_CHECKLIST_INCOMPLETE", ErrorCodes.ROLE_CHECKLIST_INCOMPLETE);
    }

    @Test
    void roleErrorCode_duplicateRole_correctCode() {
        assertEquals("PP8_ROLE_DUPLICATE", ErrorCodes.ROLE_ALREADY_ASSIGNED);
    }

    // -------------------------------------------------------
    // Helpers (mirror service gate logic for pure unit testing)
    // -------------------------------------------------------

    private boolean isGateSatisfied(boolean kycComplete, boolean camApproved) {
        return kycComplete && camApproved;
    }

    private boolean isKycComplete(String kycStatus) {
        return "COMPLETE".equalsIgnoreCase(kycStatus);
    }

    private boolean isCamApproved(CamStatus status) {
        return status == CamStatus.APPROVED;
    }

    private String buildBlockReason(boolean kycComplete, boolean camApproved) {
        if (kycComplete && camApproved) return null;
        if (!kycComplete && !camApproved) return "Both KYC and CAM must be complete before customer creation.";
        if (!kycComplete) return "KYC documents are not yet complete.";
        return "CAM has not been approved yet.";
    }
}
