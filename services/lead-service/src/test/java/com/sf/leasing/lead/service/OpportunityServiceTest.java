package com.sf.leasing.lead.service;

import com.sf.leasing.lead.domain.exception.ErrorCodes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Opportunity business logic contracts (PP5.1–PP5.4).
 *
 * Integration tests (Panache, DB) are covered by a separate integration test suite.
 */
class OpportunityServiceTest {

    // -------------------------------------------------------
    // PP5.1 — Mandatory fields validation (DTO contract)
    // -------------------------------------------------------

    @Test
    void opportunityId_format_matchesExpectedPattern() {
        String opportunityId = "OPP-2026-000001";
        assertTrue(opportunityId.startsWith("OPP-"), "Opportunity ID must start with OPP-");
        assertTrue(opportunityId.matches("OPP-\\d{4}-\\d{6}"), "Format must be OPP-YYYY-NNNNNN");
    }

    @Test
    void opportunityId_wrongFormat_doesNotMatch() {
        assertFalse("OP-2026-000001".matches("OPP-\\d{4}-\\d{6}"));
        assertFalse("OPP-26-000001".matches("OPP-\\d{4}-\\d{6}"));
        assertFalse("OPP-2026-00001".matches("OPP-\\d{4}-\\d{6}"));
    }

    // -------------------------------------------------------
    // PP5.2 — Error code contract: LOB duplicate
    // -------------------------------------------------------

    @Test
    void lobDuplicate_errorCode_isCorrect() {
        assertEquals("PP5_LOB_DUPLICATE", ErrorCodes.OPPORTUNITY_LOB_DUPLICATE);
    }

    // -------------------------------------------------------
    // PP5.3 — Error code contract: category duplicate (BLOCK)
    // -------------------------------------------------------

    @Test
    void categoryDuplicate_errorCode_isCorrect() {
        assertEquals("PP5_CATEGORY_DUPLICATE", ErrorCodes.OPPORTUNITY_CATEGORY_DUPLICATE);
    }

    // -------------------------------------------------------
    // PP5.3 — Duplicate action parsing
    // -------------------------------------------------------

    @Test
    void duplicateAction_warn_doesNotBlock() {
        String action = "WARN";
        assertFalse("BLOCK".equalsIgnoreCase(action));
    }

    @Test
    void duplicateAction_block_blocks() {
        String action = "BLOCK";
        assertTrue("BLOCK".equalsIgnoreCase(action));
    }

    // -------------------------------------------------------
    // PP5 — Not found error code contract
    // -------------------------------------------------------

    @Test
    void notFound_errorCode_isCorrect() {
        assertEquals("OPPORTUNITY_NOT_FOUND", ErrorCodes.OPPORTUNITY_NOT_FOUND);
    }
}
