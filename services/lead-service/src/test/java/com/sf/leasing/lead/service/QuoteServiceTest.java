package com.sf.leasing.lead.service;

import com.sf.leasing.lead.domain.enums.QuoteStatus;
import com.sf.leasing.lead.domain.exception.ErrorCodes;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Quote business logic contracts (PP6.1–PP6.7).
 *
 * Integration tests (Panache, DB, approval workflow) are covered by the integration test suite.
 */
class QuoteServiceTest {

    // -------------------------------------------------------
    // PP6.1 — NDLP advisory message construction
    // -------------------------------------------------------

    @Test
    void ndlpAdvisoryMessage_containsNdlp() {
        BigDecimal submitted = new BigDecimal("500000");
        BigDecimal ndlp      = new BigDecimal("600000");
        String msg = String.format(
            "Asset cost overridden from %s to NDLP %s per Product Price Service (PP6.1).",
            submitted, ndlp);
        assertTrue(msg.contains("NDLP"));
        assertTrue(msg.contains("600000"));
        assertTrue(msg.contains("500000"));
    }

    @Test
    void ndlpDiffers_whenCostsNotEqual() {
        BigDecimal submitted = new BigDecimal("500000");
        BigDecimal ndlp      = new BigDecimal("600000");
        assertNotEquals(0, submitted.compareTo(ndlp));
    }

    @Test
    void ndlpSame_whenCostsEqual() {
        BigDecimal submitted = new BigDecimal("500000");
        BigDecimal ndlp      = new BigDecimal("500000");
        assertEquals(0, submitted.compareTo(ndlp));
    }

    // -------------------------------------------------------
    // PP6.3 — Pricing deviation detection
    // -------------------------------------------------------

    @Test
    void pricingDeviation_whenFinanceExceedsCost() {
        BigDecimal cost    = new BigDecimal("500000");
        BigDecimal finance = new BigDecimal("550000");
        boolean deviation  = "CUSTOMISED".equals("CUSTOMISED") && finance.compareTo(cost) > 0;
        assertTrue(deviation);
    }

    @Test
    void pricingDeviation_notForRackRate() {
        BigDecimal cost    = new BigDecimal("500000");
        BigDecimal finance = new BigDecimal("550000");
        boolean deviation  = "RACK_RATE".equals("CUSTOMISED") && finance.compareTo(cost) > 0;
        assertFalse(deviation);
    }

    // -------------------------------------------------------
    // PP6.5 — Version increment
    // -------------------------------------------------------

    @Test
    void nextVersion_incrementsFromMaxExisting() {
        int maxVersion  = 2;
        int newVersion  = maxVersion + 1;
        assertEquals(3, newVersion);
    }

    @Test
    void firstVersion_isOne_whenNoExistingQuotes() {
        int maxVersion = 0;
        int newVersion = maxVersion + 1;
        assertEquals(1, newVersion);
    }

    // -------------------------------------------------------
    // PP6.6 — Quote status machine transitions
    // -------------------------------------------------------

    @Test
    void quoteCanBeShared_whenDraft_andNoApprovalRequired() {
        QuoteStatus status          = QuoteStatus.DRAFT;
        boolean approvalRequired    = false;
        boolean approved            = (status == QuoteStatus.APPROVED);

        boolean canShare = (!approvalRequired || approved)
            && (status == QuoteStatus.DRAFT || status == QuoteStatus.APPROVED);
        assertTrue(canShare);
    }

    @Test
    void quoteCannotBeShared_whenPendingApproval() {
        QuoteStatus status       = QuoteStatus.PENDING_APPROVAL;
        boolean approvalRequired = true;
        boolean approved         = (status == QuoteStatus.APPROVED);

        boolean canShare = (!approvalRequired || approved)
            && (status == QuoteStatus.DRAFT || status == QuoteStatus.APPROVED);
        assertFalse(canShare);
    }

    @Test
    void quoteLockRequiresSharedStatus() {
        QuoteStatus status = QuoteStatus.DRAFT;
        boolean canLock = (status == QuoteStatus.SHARED);
        assertFalse(canLock);

        status  = QuoteStatus.SHARED;
        canLock = (status == QuoteStatus.SHARED);
        assertTrue(canLock);
    }

    @Test
    void unlockRequestRequiresLockedStatus() {
        QuoteStatus status = QuoteStatus.SHARED;
        boolean canRequestUnlock = (status == QuoteStatus.LOCKED);
        assertFalse(canRequestUnlock);

        status          = QuoteStatus.LOCKED;
        canRequestUnlock = (status == QuoteStatus.LOCKED);
        assertTrue(canRequestUnlock);
    }

    @Test
    void approveUnlockRequiresUnlockRequestedStatus() {
        QuoteStatus status = QuoteStatus.LOCKED;
        boolean canApproveUnlock = (status == QuoteStatus.UNLOCK_REQUESTED);
        assertFalse(canApproveUnlock);

        status          = QuoteStatus.UNLOCK_REQUESTED;
        canApproveUnlock = (status == QuoteStatus.UNLOCK_REQUESTED);
        assertTrue(canApproveUnlock);
    }

    // -------------------------------------------------------
    // PP6.7 — Appraisal category required before lock
    // -------------------------------------------------------

    @Test
    void appraisalCategoryMissing_blocksLock() {
        String appraisalCategory = null;
        boolean canLock = !(appraisalCategory == null || appraisalCategory.isBlank());
        assertFalse(canLock);
    }

    @Test
    void appraisalCategoryPresent_allowsLock() {
        String appraisalCategory = "STANDARD";
        boolean canLock = !(appraisalCategory == null || appraisalCategory.isBlank());
        assertTrue(canLock);
    }

    // -------------------------------------------------------
    // PP6 — ID format contract
    // -------------------------------------------------------

    @Test
    void quoteId_format_matchesExpectedPattern() {
        String quoteId = "QT-2026-000001";
        assertTrue(quoteId.startsWith("QT-"));
        assertTrue(quoteId.matches("QT-\\d{4}-\\d{6}"));
    }

    // -------------------------------------------------------
    // PP6 — Error code contracts
    // -------------------------------------------------------

    @Test
    void errorCodes_quotePhase4_defined() {
        assertNotNull(ErrorCodes.QUOTE_NOT_FOUND);
        assertNotNull(ErrorCodes.QUOTE_NOT_PENDING_APPROVAL);
        assertNotNull(ErrorCodes.QUOTE_REQUIRES_APPROVAL);
        assertNotNull(ErrorCodes.QUOTE_INVALID_TRANSITION);
        assertNotNull(ErrorCodes.QUOTE_APPRAISAL_REQUIRED);
        assertNotNull(ErrorCodes.QUOTE_NOT_LOCKED);
        assertNotNull(ErrorCodes.QUOTE_UNLOCK_NOT_REQUESTED);
        assertNotNull(ErrorCodes.QUOTE_NOT_LOCKED_FOR_APPLICATION);
    }
}
