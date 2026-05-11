package com.sf.leasing.lead.api.resource;

import com.sf.leasing.lead.api.dto.request.OverrideTemperatureRequest;
import com.sf.leasing.lead.domain.model.Lead;
import com.sf.leasing.lead.domain.model.TemperatureAudit;
import com.sf.leasing.lead.infrastructure.persistence.LeadRepository;
import com.sf.leasing.lead.infrastructure.persistence.TemperatureAuditRepository;
import com.sf.leasing.lead.service.TemperatureEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * LP7: Lead Temperature endpoints.
 *
 * PUT  /api/v1/leads/{lrn}/temperature  — manual override
 * GET  /api/v1/leads/{lrn}/temperature  — current temperature + audit history
 */
@RestController
@RequestMapping("/api/v1/leads")
@Tag(name = "Lead Temperature", description = "LP7: Temperature classification and manual override")
public class TemperatureResource {

    private final TemperatureEngineService temperatureEngineService;
    private final LeadRepository leadRepository;
    private final TemperatureAuditRepository temperatureAuditRepository;

    public TemperatureResource(TemperatureEngineService temperatureEngineService,
                               LeadRepository leadRepository,
                               TemperatureAuditRepository temperatureAuditRepository) {
        this.temperatureEngineService = temperatureEngineService;
        this.leadRepository = leadRepository;
        this.temperatureAuditRepository = temperatureAuditRepository;
    }

    /**
     * PUT /api/v1/leads/{lrn}/temperature
     * LP7.3: Manual temperature override. StatusReason (code + free text) mandatory.
     */
    @PutMapping("/{lrn}/temperature")
    @Operation(summary = "Manually override lead temperature (LP7.3)")
    public ResponseEntity<?> overrideTemperature(
        @PathVariable("lrn") String lrn,
        @Valid @RequestBody OverrideTemperatureRequest req,
        @RequestHeader("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        temperatureEngineService.overrideTemperature(lrn, req, userId);
        return ResponseEntity.ok("{\"message\":\"Temperature updated successfully\"}");
    }

    /**
     * GET /api/v1/leads/{lrn}/temperature
     * Returns current temperature and full audit trail.
     */
    @GetMapping("/{lrn}/temperature")
    @Operation(summary = "Get current temperature and history (LP7)")
    public ResponseEntity<?> getTemperature(
        @PathVariable("lrn") String lrn,
        @RequestHeader("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Lead lead = leadRepository.findByLrn(lrn).orElse(null);
        if (lead == null) {
            return ResponseEntity.notFound().build();
        }
        List<TemperatureAudit> history = temperatureAuditRepository.findByLeadId(lead.id);
        return ResponseEntity.ok(new TemperatureInfo(lead, history));
    }

    // -------------------------------------------------------
    // Response model
    // -------------------------------------------------------

    public static class TemperatureInfo {
        public String lrn;
        public String currentTemperature;
        public String suggestedBy;
        public String reasonCode;
        public String reasonText;
        public List<TemperatureAudit> history;

        public TemperatureInfo(Lead lead, List<TemperatureAudit> history) {
            this.lrn                = lead.lrn;
            this.currentTemperature = lead.temperature != null ? lead.temperature.name() : null;
            this.suggestedBy        = lead.temperatureSuggestedBy;
            this.reasonCode         = lead.temperatureReasonCode;
            this.reasonText         = lead.temperatureReasonText;
            this.history            = history;
        }
    }
}
