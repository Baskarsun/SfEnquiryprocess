package com.sf.leasing.lead.api.resource;

import com.sf.leasing.lead.api.dto.response.DashboardResponse;
import com.sf.leasing.lead.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Phase 2 enhanced dashboards (Section 5.7 of the implementation plan).
 *
 * GET /api/v1/dashboards/temperature    — temperature distribution
 * GET /api/v1/dashboards/sla            — SLA compliance tracker
 * GET /api/v1/dashboards/aging          — aging report (0-7 / 8-15 / 16-30 / 30+ days)
 * GET /api/v1/dashboards/marketing      — marketing source report
 */
@RestController
@RequestMapping("/api/v1/dashboards")
@Tag(name = "Dashboards", description = "Phase 2 enhanced dashboards: temperature, SLA, aging, marketing source")
public class DashboardResource {

    private final DashboardService dashboardService;

    public DashboardResource(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/temperature")
    @Operation(summary = "Lead temperature distribution (Hot / Warm / Cold counts)")
    public ResponseEntity<?> temperatureDistribution(
        @RequestParam(value = "branchCode", required = false) String branchCode,
        @RequestHeader("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        DashboardResponse report = dashboardService.temperatureDistribution(branchCode);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/sla")
    @Operation(summary = "SLA compliance tracker (compliant vs. breached assignments)")
    public ResponseEntity<?> slaCompliance(
        @RequestParam(value = "branchCode", required = false) String branchCode,
        @RequestHeader("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        DashboardResponse report = dashboardService.slaComplianceTracker(branchCode);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/aging")
    @Operation(summary = "Lead aging report bucketed by days (0-7, 8-15, 16-30, 30+)")
    public ResponseEntity<?> agingReport(
        @RequestParam(value = "branchCode", required = false) String branchCode,
        @RequestHeader("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        DashboardResponse report = dashboardService.agingReport(branchCode);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/marketing")
    @Operation(summary = "Leads by Source Category and Source Name")
    public ResponseEntity<?> marketingSourceReport(
        @RequestParam(value = "branchCode", required = false) String branchCode,
        @RequestHeader("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        DashboardResponse report = dashboardService.marketingSourceReport(branchCode);
        return ResponseEntity.ok(report);
    }
}
