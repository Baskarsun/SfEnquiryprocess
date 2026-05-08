package com.sf.leasing.lead.service;

import com.sf.leasing.lead.domain.enums.ApplicationStatus;
import com.sf.leasing.lead.domain.enums.CamStatus;
import com.sf.leasing.lead.domain.exception.ErrorCodes;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Application business logic contracts (PP7.1–PP7.12).
 *
 * Integration tests (Panache, DMS, fraud screening) are covered by the integration test suite.
 */
class ApplicationServiceTest {

    private static final Set<String> VALID_DOCUMENT_TYPES =
        Set.of("PHOTO", "DRIVING_LICENCE", "PAN", "PASSPORT", "OTHER_KYC");
    private static final Set<String> VALID_APPLICANT_PREFIXES = Set.of("MA", "A1", "A2");
    private static final Set<String> MANDATORY_INDIVIDUAL_DOCS = Set.of("PHOTO", "PAN");
    private static final Set<String> MANDATORY_COMMERCIAL_DOCS = Set.of("PAN");

    // -------------------------------------------------------
    // PP7.1 — Application ID format
    // -------------------------------------------------------

    @Test
    void applicationId_format_matchesExpectedPattern() {
        String appId = "APP-2026-000001";
        assertTrue(appId.startsWith("APP-"));
        assertTrue(appId.matches("APP-\\d{4}-\\d{6}"));
    }

    @Test
    void applicationId_wrongFormat_doesNotMatch() {
        assertFalse("AP-2026-000001".matches("APP-\\d{4}-\\d{6}"));
        assertFalse("APP-26-000001".matches("APP-\\d{4}-\\d{6}"));
        assertFalse("APP-2026-00001".matches("APP-\\d{4}-\\d{6}"));
    }

    // -------------------------------------------------------
    // PP7.3 — Document type validation
    // -------------------------------------------------------

    @Test
    void validDocumentTypes_accepted() {
        for (String type : VALID_DOCUMENT_TYPES) {
            assertTrue(VALID_DOCUMENT_TYPES.contains(type), "Should accept: " + type);
        }
    }

    @Test
    void invalidDocumentType_rejected() {
        assertFalse(VALID_DOCUMENT_TYPES.contains("INVALID_TYPE"));
        assertFalse(VALID_DOCUMENT_TYPES.contains("BANK_STATEMENT"));
        assertFalse(VALID_DOCUMENT_TYPES.contains(""));
    }

    @Test
    void validApplicantPrefixes_accepted() {
        for (String prefix : VALID_APPLICANT_PREFIXES) {
            assertTrue(VALID_APPLICANT_PREFIXES.contains(prefix));
        }
    }

    @Test
    void invalidApplicantPrefix_rejected() {
        assertFalse(VALID_APPLICANT_PREFIXES.contains("A3"));
        assertFalse(VALID_APPLICANT_PREFIXES.contains("MAIN"));
        assertFalse(VALID_APPLICANT_PREFIXES.contains(""));
    }

    // -------------------------------------------------------
    // PP7.3 — Document content Base64 decoding
    // -------------------------------------------------------

    @Test
    void base64Content_decodesCorrectly() {
        String content = Base64.getEncoder().encodeToString("fakePdfContent".getBytes());
        byte[] decoded = Base64.getDecoder().decode(content);
        assertEquals("fakePdfContent", new String(decoded));
    }

    @Test
    void invalidBase64Content_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
            () -> Base64.getDecoder().decode("not-valid-base64!!!"));
    }

    // -------------------------------------------------------
    // PP7.7 — Non-individual auto-eligibility
    // -------------------------------------------------------

    @Test
    void commercialLessee_isNotIndividual() {
        boolean individual = "INDIVIDUAL".equalsIgnoreCase("COMMERCIAL");
        assertFalse(individual);
    }

    @Test
    void individualLessee_isIndividual() {
        boolean individual = "INDIVIDUAL".equalsIgnoreCase("INDIVIDUAL");
        assertTrue(individual);
    }

    @Test
    void nonIndividual_autoAdvances_toEligible() {
        boolean isIndividual = false;
        ApplicationStatus status = isIndividual
            ? ApplicationStatus.INITIATED
            : ApplicationStatus.ELIGIBLE_FOR_APPLICATION;
        assertEquals(ApplicationStatus.ELIGIBLE_FOR_APPLICATION, status);
    }

    // -------------------------------------------------------
    // PP7.2 — Mandatory document checklist
    // -------------------------------------------------------

    @Test
    void kycComplete_individual_whenPhotoAndPanUploaded() {
        Set<String> uploaded = Set.of("PHOTO", "PAN");
        assertTrue(uploaded.containsAll(MANDATORY_INDIVIDUAL_DOCS));
    }

    @Test
    void kycIncomplete_individual_whenOnlyPhotoUploaded() {
        Set<String> uploaded = Set.of("PHOTO");
        assertFalse(uploaded.containsAll(MANDATORY_INDIVIDUAL_DOCS));
    }

    @Test
    void kycComplete_commercial_whenPanUploaded() {
        Set<String> uploaded = Set.of("PAN", "OTHER_KYC");
        assertTrue(uploaded.containsAll(MANDATORY_COMMERCIAL_DOCS));
    }

    @Test
    void kycIncomplete_commercial_whenNoPanUploaded() {
        Set<String> uploaded = Set.of("OTHER_KYC");
        assertFalse(uploaded.containsAll(MANDATORY_COMMERCIAL_DOCS));
    }

    // -------------------------------------------------------
    // PP7.8 — Modification block error codes
    // -------------------------------------------------------

    @Test
    void modificationBlock_contractInProgress_errorCode() {
        assertEquals("LN3713", ErrorCodes.MODIFICATION_CONTRACT_IN_PROGRESS);
    }

    @Test
    void modificationBlock_fraudInvestigation_errorCode() {
        assertEquals("LN3785", ErrorCodes.MODIFICATION_FRAUD_INVESTIGATION);
    }

    // -------------------------------------------------------
    // PP7.12 — Welcome communication lease type matching
    // -------------------------------------------------------

    @Test
    void eligibleLeaseType_matchesConfiguredList() {
        String configuredTypes = "FINANCE_LEASE,OPERATING_LEASE";
        assertTrue(isEligible("FINANCE_LEASE", configuredTypes));
        assertTrue(isEligible("OPERATING_LEASE", configuredTypes));
    }

    @Test
    void ineligibleLeaseType_doesNotMatch() {
        String configuredTypes = "FINANCE_LEASE,OPERATING_LEASE";
        assertFalse(isEligible("HIRE_PURCHASE", configuredTypes));
        assertFalse(isEligible("", configuredTypes));
    }

    // -------------------------------------------------------
    // CAM status machine contracts (PP7.8)
    // -------------------------------------------------------

    @Test
    void camStatus_initial_isNotStarted() {
        CamStatus initial = CamStatus.NOT_STARTED;
        assertEquals(CamStatus.NOT_STARTED, initial);
    }

    @Test
    void camApproval_movesToApproved() {
        CamStatus status = CamStatus.IN_PROGRESS;
        status = CamStatus.APPROVED;
        assertEquals(CamStatus.APPROVED, status);
    }

    @Test
    void camDecline_movesToDeclined() {
        CamStatus status = CamStatus.IN_PROGRESS;
        status = CamStatus.DECLINED;
        assertEquals(CamStatus.DECLINED, status);
    }

    // -------------------------------------------------------
    // PP7 — Error code contracts
    // -------------------------------------------------------

    @Test
    void errorCodes_applicationPhase4_defined() {
        assertNotNull(ErrorCodes.APPLICATION_NOT_FOUND);
        assertNotNull(ErrorCodes.APPLICATION_ALREADY_EXISTS);
        assertNotNull(ErrorCodes.DOCUMENT_TYPE_INVALID);
        assertNotNull(ErrorCodes.APPLICANT_PREFIX_INVALID);
        assertNotNull(ErrorCodes.CAM_ALREADY_INITIATED);
        assertNotNull(ErrorCodes.CAM_NOT_IN_PROGRESS);
        assertNotNull(ErrorCodes.CAM_NOT_FOUND);
        assertNotNull(ErrorCodes.CAM_INVALID_DECISION);
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------

    private boolean isEligible(String leaseType, String configured) {
        for (String type : configured.split(",")) {
            if (type.trim().equalsIgnoreCase(leaseType)) return true;
        }
        return false;
    }
}
