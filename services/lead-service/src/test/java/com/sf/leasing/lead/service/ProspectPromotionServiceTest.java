package com.sf.leasing.lead.service;

import com.sf.leasing.lead.api.dto.response.ProspectPromotionResponse;
import com.sf.leasing.lead.domain.enums.LeadStatus;
import com.sf.leasing.lead.domain.enums.ProspectStatus;
import com.sf.leasing.lead.domain.exception.BusinessException;
import com.sf.leasing.lead.domain.exception.ErrorCodes;
import com.sf.leasing.lead.domain.model.Lead;
import com.sf.leasing.lead.domain.model.Prospect;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ProspectPromotionService business logic.
 *
 * Panache-dependent methods (promote()) are exercised via integration tests.
 * These tests cover guard conditions and response builder contracts.
 */
class ProspectPromotionServiceTest {

    // -------------------------------------------------------
    // ProspectPromotionResponse.of() contract
    // -------------------------------------------------------

    @Test
    void promotionResponseShouldHaveNullProspectIdAtCreation() {
        UUID uuid = UUID.randomUUID();
        ProspectPromotionResponse resp = ProspectPromotionResponse.of(uuid, "LS-202601-000001", List.of());

        assertNotNull(resp.prospectUuid, "prospectUuid must not be null");
        assertNull(resp.prospectId, "prospectId must be null at promotion time — gated on PP3 KYC");
        assertEquals("DRAFT", resp.status);
        assertEquals("LS-202601-000001", resp.leadLrn);
    }

    @Test
    void promotionResponseShouldCarryWarnings() {
        UUID uuid = UUID.randomUUID();
        List<String> warnings = List.of("NRI: alternate mobile required", "NRI: passport expiring soon");
        ProspectPromotionResponse resp = ProspectPromotionResponse.of(uuid, "LS-202601-000002", warnings);

        assertEquals(2, resp.promotionWarnings.size());
    }

    // -------------------------------------------------------
    // Lead guard checks (tested through domain model helpers)
    // -------------------------------------------------------

    @Test
    void closedLeadShouldBeDetectedByHelper() {
        Lead lead = buildLead();
        lead.status = LeadStatus.CLOSED;
        assertTrue(lead.isClosed(), "isClosed() must return true for CLOSED lead");
    }

    @Test
    void promotedLeadShouldBeDetectedByHelper() {
        Lead lead = buildLead();
        lead.status = LeadStatus.PROMOTED;
        assertTrue(lead.isPromoted(), "isPromoted() must return true for PROMOTED lead");
    }

    @Test
    void newLeadShouldNotBeConsideredClosedOrPromoted() {
        Lead lead = buildLead();
        lead.status = LeadStatus.NEW;
        assertFalse(lead.isClosed());
        assertFalse(lead.isPromoted());
    }

    // -------------------------------------------------------
    // Prospect initial state contract
    // -------------------------------------------------------

    @Test
    void freshProspectShouldBeInDraftStatus() {
        Prospect p = new Prospect();
        p.status = ProspectStatus.DRAFT;

        assertTrue(p.isDraft());
        assertFalse(p.isValidated());
        assertFalse(p.isActive());
        assertFalse(p.isClosed());
    }

    @Test
    void prospectIdShouldBeNullBeforeKycValidation() {
        Prospect p = new Prospect();
        p.status = ProspectStatus.DRAFT;
        // Simulates newly created prospect — no KYC validation run yet
        assertNull(p.prospectId, "prospectId must be null until PP3 validation generates PR-YYYY-NNNNNN");
    }

    @Test
    void errorCodeLeadNotFoundShouldMatchConstant() {
        BusinessException ex = new BusinessException(ErrorCodes.LEAD_NOT_FOUND, "Lead not found: LS-X");
        assertEquals(ErrorCodes.LEAD_NOT_FOUND, ex.getErrorCode());
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------

    private Lead buildLead() {
        Lead lead = new Lead();
        lead.lrn = "LS-202601-000001";
        lead.id  = UUID.randomUUID();
        return lead;
    }
}
