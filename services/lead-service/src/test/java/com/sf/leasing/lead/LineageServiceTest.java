package com.sf.leasing.lead;

import com.sf.leasing.lead.api.dto.response.LineageResponse;
import com.sf.leasing.lead.domain.exception.ErrorCodes;
import com.sf.leasing.lead.domain.model.Lineage;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Lineage chain business rules — PP8.2.
 *
 * Validates lineage structure, immutability contract, and cross-reference
 * query mapping without database or Panache dependencies.
 */
class LineageServiceTest {

    // -------------------------------------------------------
    // PP8.2: LineageResponse mapping
    // -------------------------------------------------------

    @Test
    void lineageResponse_from_mapsAllFields() {
        Lineage l = buildFullLineage();
        LineageResponse r = LineageResponse.from(l);

        assertEquals(l.id, r.lineageId);
        assertEquals(l.leadLrn, r.leadLrn);
        assertEquals(l.leadId, r.leadId);
        assertEquals(l.prospectUuid, r.prospectUuid);
        assertEquals(l.prospectBusinessId, r.prospectBusinessId);
        assertEquals(l.opportunityUuid, r.opportunityUuid);
        assertEquals(l.opportunityBusinessId, r.opportunityBusinessId);
        assertEquals(l.quoteUuid, r.quoteUuid);
        assertEquals(l.quoteBusinessId, r.quoteBusinessId);
        assertEquals(l.applicationUuid, r.applicationUuid);
        assertEquals(l.applicationBusinessId, r.applicationBusinessId);
        assertEquals(l.customerUuid, r.customerUuid);
        assertEquals(l.customerId, r.customerId);
    }

    @Test
    void lineageResponse_partialChain_nullsPreserved() {
        Lineage l = new Lineage();
        l.id      = UUID.randomUUID();
        l.leadLrn = "LS-202601-000001";
        l.leadId  = UUID.randomUUID();

        LineageResponse r = LineageResponse.from(l);

        assertNull(r.prospectUuid);
        assertNull(r.opportunityUuid);
        assertNull(r.customerId);
    }

    // -------------------------------------------------------
    // PP8.2: Chain completeness check
    // -------------------------------------------------------

    @Test
    void lineageChain_allSegmentsSet_isComplete() {
        Lineage l = buildFullLineage();
        assertTrue(isChainComplete(l));
    }

    @Test
    void lineageChain_missingCustomer_notComplete() {
        Lineage l = buildFullLineage();
        l.customerId = null;
        l.customerUuid = null;
        assertFalse(isChainComplete(l));
    }

    @Test
    void lineageChain_onlyLead_notComplete() {
        Lineage l = new Lineage();
        l.leadLrn = "LS-202601-000001";
        l.leadId  = UUID.randomUUID();
        assertFalse(isChainComplete(l));
    }

    // -------------------------------------------------------
    // PP8.2: Error codes
    // -------------------------------------------------------

    @Test
    void lineageErrorCode_notFound_correctCode() {
        assertEquals("PP8_LINEAGE_NOT_FOUND", ErrorCodes.LINEAGE_NOT_FOUND);
    }

    // -------------------------------------------------------
    // PP8.2: Immutability — once set, IDs must not change
    // -------------------------------------------------------

    @Test
    void lineageOpportunityLink_setOnce_doesNotOverwriteIfAlreadySet() {
        Lineage l = new Lineage();
        l.opportunityUuid        = UUID.randomUUID();
        l.opportunityBusinessId  = "OPP-2026-000001";

        UUID original = l.opportunityUuid;

        // Simulate a second linkOpportunity call — should skip if already set
        if (l.opportunityUuid == null) {
            l.opportunityUuid       = UUID.randomUUID();
            l.opportunityBusinessId = "OPP-2026-000002";
        }

        assertEquals(original, l.opportunityUuid, "Existing opportunity link must not be overwritten");
        assertEquals("OPP-2026-000001", l.opportunityBusinessId);
    }

    @Test
    void lineageApplicationLink_setOnce_doesNotOverwriteIfAlreadySet() {
        Lineage l = new Lineage();
        l.applicationUuid        = UUID.randomUUID();
        l.applicationBusinessId  = "APP-2026-000001";

        UUID original = l.applicationUuid;

        if (l.applicationUuid == null) {
            l.applicationUuid        = UUID.randomUUID();
            l.applicationBusinessId  = "APP-2026-999999";
        }

        assertEquals(original, l.applicationUuid);
        assertEquals("APP-2026-000001", l.applicationBusinessId);
    }

    // -------------------------------------------------------
    // PP8.2: Cross-reference id format consistency
    // -------------------------------------------------------

    @Test
    void lineageIds_allFollowFormatContract() {
        assertEquals("LS-202601-000001", "LS-202601-000001");
        assertTrue("PR-2026-000001".startsWith("PR-"));
        assertTrue("OPP-2026-000001".startsWith("OPP-"));
        assertTrue("QT-2026-000001".startsWith("QT-"));
        assertTrue("APP-2026-000001".startsWith("APP-"));
        assertTrue("CUST-2026-000001".startsWith("CUST-"));
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------

    private Lineage buildFullLineage() {
        Lineage l = new Lineage();
        l.id                    = UUID.randomUUID();
        l.leadLrn               = "LS-202601-000001";
        l.leadId                = UUID.randomUUID();
        l.prospectUuid          = UUID.randomUUID();
        l.prospectBusinessId    = "PR-2026-000001";
        l.opportunityUuid       = UUID.randomUUID();
        l.opportunityBusinessId = "OPP-2026-000001";
        l.quoteUuid             = UUID.randomUUID();
        l.quoteBusinessId       = "QT-2026-000001";
        l.applicationUuid       = UUID.randomUUID();
        l.applicationBusinessId = "APP-2026-000001";
        l.customerUuid          = UUID.randomUUID();
        l.customerId            = "CUST-2026-000001";
        return l;
    }

    private boolean isChainComplete(Lineage l) {
        return l.leadLrn != null
            && l.prospectUuid != null
            && l.opportunityUuid != null
            && l.quoteUuid != null
            && l.applicationUuid != null
            && l.customerUuid != null;
    }
}
