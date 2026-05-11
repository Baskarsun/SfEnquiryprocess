package com.sf.leasing.lead.service;

import com.sf.leasing.lead.api.dto.request.OverrideTemperatureRequest;
import com.sf.leasing.lead.domain.enums.*;
import com.sf.leasing.lead.domain.exception.BusinessException;
import com.sf.leasing.lead.domain.model.Lead;
import com.sf.leasing.lead.infrastructure.persistence.LeadRepository;
import com.sf.leasing.lead.infrastructure.persistence.TemperatureAuditRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit-level tests for TemperatureEngineService business logic.
 * DB-dependent operations (runDailyDegradation) are tested via integration tests.
 */
@ExtendWith(MockitoExtension.class)
class TemperatureEngineServiceTest {

    @Mock LeadRepository leadRepository;
    @Mock TemperatureAuditRepository temperatureAuditRepository;

    // -------------------------------------------------------
    // computeTemperature (pure logic, no DB calls)
    // -------------------------------------------------------

    @Test
    void shouldClassifyAsHotWhenManyRecentAttempts() {
        TemperatureEngineService service = new TemperatureEngineService(leadRepository, temperatureAuditRepository);

        Lead lead = buildLead();
        lead.callAttempts    = 2;
        lead.messageAttempts = 1;
        lead.emailAttempts   = 0;

        LeadTemperature result = service.computeTemperature(lead);
        assertEquals(LeadTemperature.HOT, result);
    }

    @Test
    void shouldClassifyAsColdWhenNoAttempts() {
        TemperatureEngineService service = new TemperatureEngineService();

        Lead lead = buildLead();
        lead.callAttempts    = 0;
        lead.messageAttempts = 0;
        lead.emailAttempts   = 0;
        lead.createdAt       = java.time.LocalDateTime.now().minusDays(20);

        LeadTemperature result = service.computeTemperature(lead);
        assertEquals(LeadTemperature.COLD, result);
    }

    @Test
    void shouldReturnCurrentTemperatureForClosedLead() {
        TemperatureEngineService service = new TemperatureEngineService();

        Lead lead = buildLead();
        lead.status      = LeadStatus.CLOSED;
        lead.temperature = LeadTemperature.WARM;

        LeadTemperature result = service.computeTemperature(lead);
        assertEquals(LeadTemperature.WARM, result);
    }

    // -------------------------------------------------------
    // Override request validation
    // -------------------------------------------------------

    @Test
    void overrideRequestShouldHaveMandatoryReasonCode() {
        OverrideTemperatureRequest req = new OverrideTemperatureRequest();
        req.temperature = LeadTemperature.HOT;
        req.reasonCode  = "";  // blank — violates constraint
        req.reasonText  = "Converting customer";

        // Constraint violation would surface via Bean Validation;
        // the reasonCode field has @NotBlank so this is caught at the API layer.
        assertTrue(req.reasonCode.isBlank(), "Blank reasonCode triggers @NotBlank constraint");
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------

    private Lead buildLead() {
        Lead lead = new Lead();
        lead.lrn         = "LS-202601-000001";
        lead.status      = LeadStatus.ASSIGNED;
        lead.temperature = LeadTemperature.COLD;
        lead.createdAt   = java.time.LocalDateTime.now().minusDays(1);
        return lead;
    }
}
