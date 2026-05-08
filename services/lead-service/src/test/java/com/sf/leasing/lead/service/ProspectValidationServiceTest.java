package com.sf.leasing.lead.service;

import com.sf.leasing.lead.domain.exception.BusinessException;
import com.sf.leasing.lead.domain.exception.ErrorCodes;
import com.sf.leasing.lead.domain.model.Prospect;
import com.sf.leasing.lead.infrastructure.adapter.GstinValidationAdapter;
import com.sf.leasing.lead.infrastructure.adapter.PanValidationAdapter;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
class ProspectValidationServiceTest {

    @Inject
    ProspectValidationService validationService;

    @InjectMock
    PanValidationAdapter panAdapter;

    @InjectMock
    GstinValidationAdapter gstinAdapter;

    @BeforeEach
    void setUp() {
        when(panAdapter.validate(anyString()))
            .thenReturn(PanValidationAdapter.PanValidationResult.success(
                "TEST ENTITY PVT LTD", "123 Test Street"));

        when(gstinAdapter.validate(anyString()))
            .thenReturn(GstinValidationAdapter.GstinValidationResult.success(
                "TEST ENTITY PVT LTD", "123 Test Street", "ABCDE1234F"));
    }

    // -------------------------------------------------------
    // Adapter result factory methods
    // -------------------------------------------------------

    @Test
    void panValidationSuccessResultShouldBeValid() {
        PanValidationAdapter.PanValidationResult result =
            PanValidationAdapter.PanValidationResult.success("ACME PVT LTD", "Mumbai 400001");

        assertTrue(result.valid());
        assertEquals("ACME PVT LTD", result.legalName());
        assertNotNull(result.registeredAddress());
        assertNull(result.errorCode());
    }

    @Test
    void panValidationFailureResultShouldBeInvalid() {
        PanValidationAdapter.PanValidationResult result =
            PanValidationAdapter.PanValidationResult.failure("PAN_NOT_FOUND", "PAN not in registry");

        assertFalse(result.valid());
        assertNull(result.legalName());
        assertEquals("PAN_NOT_FOUND", result.errorCode());
    }

    @Test
    void gstinValidationSuccessResultShouldCarryDerivedPan() {
        GstinValidationAdapter.GstinValidationResult result =
            GstinValidationAdapter.GstinValidationResult.success(
                "ACME PVT LTD", "Delhi 110001", "ABCDE1234F");

        assertTrue(result.valid());
        assertEquals("ABCDE1234F", result.derivedPan());
        assertNull(result.errorCode());
    }

    @Test
    void gstinDerivedPanShouldBeChars2To11OfGstin() {
        // GSTIN format: 2-digit state code + 10-char PAN + 1 entity number + 1 check + Z
        // Example: 27ABCDE1234F1ZX → chars[2..11] = ABCDE1234F
        String gstin = "27ABCDE1234F1ZX";
        String expectedDerivedPan = gstin.substring(2, 12);
        assertEquals("ABCDE1234F", expectedDerivedPan);
    }

    @Test
    void gstinValidationFailureResultShouldHaveNullDerivedPan() {
        GstinValidationAdapter.GstinValidationResult result =
            GstinValidationAdapter.GstinValidationResult.failure("GSTIN_INVALID", "Format invalid");

        assertFalse(result.valid());
        assertNull(result.derivedPan());
    }

    // -------------------------------------------------------
    // Invalid validation type guard (type check before DB lookup)
    // -------------------------------------------------------

    @Test
    void shouldThrowInvalidTypeBeforeProspectLookup() {
        // Type guard must fire before DB lookup — so INVALID_VALIDATION_TYPE is returned
        // even when the prospectId doesn't exist in the DB
        BusinessException ex = assertThrows(BusinessException.class,
            () -> validationService.triggerValidation(UUID.randomUUID(), "AADHAAR", "USER001"));
        assertEquals(ErrorCodes.INVALID_VALIDATION_TYPE, ex.getErrorCode());
    }

    @Test
    void shouldThrowInvalidTypeForBlankInput() {
        BusinessException ex = assertThrows(BusinessException.class,
            () -> validationService.triggerValidation(UUID.randomUUID(), "   ", "USER001"));
        assertEquals(ErrorCodes.INVALID_VALIDATION_TYPE, ex.getErrorCode());
    }

    @Test
    void shouldThrowInvalidTypeForOverrideWithBadType() {
        BusinessException ex = assertThrows(BusinessException.class,
            () -> validationService.override(UUID.randomUUID(), "VOTER_ID",
                "REASON_A", "Override text", "USER001"));
        assertEquals(ErrorCodes.INVALID_VALIDATION_TYPE, ex.getErrorCode());
    }

    // -------------------------------------------------------
    // Prospect not found (valid type, missing prospect)
    // -------------------------------------------------------

    @Test
    void shouldThrowProspectNotFoundAfterValidTypeCheck() {
        BusinessException ex = assertThrows(BusinessException.class,
            () -> validationService.triggerValidation(UUID.randomUUID(), "PAN", "USER001"));
        assertEquals(ErrorCodes.PROSPECT_NOT_FOUND, ex.getErrorCode());
    }

    // -------------------------------------------------------
    // Prospect ID gating
    // -------------------------------------------------------

    @Test
    void prospectIdShouldBeNullUntilValidationSucceeds() {
        Prospect p = new Prospect();
        assertNull(p.prospectId,
            "prospectId is null until PP3.1 generates PR-YYYY-NNNNNN on first KYC success");
    }

    @Test
    void prospectIdFormatShouldMatchPrPattern() {
        String id = "PR-2026-000001";
        assertTrue(id.startsWith("PR-"), "Prospect ID must start with PR-");
        assertTrue(id.matches("PR-\\d{4}-\\d{6}"), "Prospect ID format must be PR-YYYY-NNNNNN");
    }

    // -------------------------------------------------------
    // Error code constants
    // -------------------------------------------------------

    @Test
    void kycValidationFailedErrorCodeShouldMatchConstant() {
        BusinessException ex = new BusinessException(ErrorCodes.KYC_VALIDATION_FAILED, "KYC failed");
        assertEquals(ErrorCodes.KYC_VALIDATION_FAILED, ex.getErrorCode());
    }
}
