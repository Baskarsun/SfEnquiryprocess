package com.sf.leasing.lead.api.resource;

import com.sf.leasing.lead.api.dto.response.DashboardResponse;
import com.sf.leasing.lead.service.ProspectDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboards/prospects")
@Tag(name = "ProspectDashboard", description = "Phase 3 dashboards: Pipeline, KYC, Conversion")
public class ProspectDashboardResource {

    private final ProspectDashboardService dashboardService;

    public ProspectDashboardResource(ProspectDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * GET /api/v1/dashboards/prospects/pipeline?branchCode=
     * Prospect count grouped by status (DRAFT / VALIDATED / ACTIVE / IN_APPRAISAL / etc.).
     */
    @GetMapping("/pipeline")
    @Operation(summary = "Prospect pipeline by status")
    public ResponseEntity<?> pipeline(
        @RequestParam(value = "branchCode", required = false) String branchCode
    ) {
        DashboardResponse report = dashboardService.prospectPipeline(branchCode);
        return ResponseEntity.ok(report);
    }

    /**
     * GET /api/v1/dashboards/prospects/exception-queue
     * Summary of prospect validation exception queue entries by status.
     */
    @GetMapping("/exception-queue")
    @Operation(summary = "Prospect validation exception queue summary")
    public ResponseEntity<?> exceptionQueue(
        @RequestParam(value = "branchCode", required = false) String branchCode
    ) {
        DashboardResponse report = dashboardService.exceptionQueueSummary(branchCode);
        return ResponseEntity.ok(report);
    }

    /**
     * GET /api/v1/dashboards/prospects/conversion?branchCode=
     * Lead → Prospect conversion metrics for the last 30 days.
     */
    @GetMapping("/conversion")
    @Operation(summary = "Lead-to-Prospect conversion metrics (30d)")
    public ResponseEntity<?> conversion(
        @RequestParam(value = "branchCode", required = false) String branchCode
    ) {
        DashboardResponse report = dashboardService.conversionMetrics(branchCode);
        return ResponseEntity.ok(report);
    }

    /**
     * GET /api/v1/dashboards/prospects/kyc?branchCode=
     * KYC outcome breakdown: SUCCESS / FAILED / OVERRIDDEN.
     */
    @GetMapping("/kyc")
    @Operation(summary = "KYC validation outcomes breakdown")
    public ResponseEntity<?> kycOutcomes(
        @RequestParam(value = "branchCode", required = false) String branchCode
    ) {
        DashboardResponse report = dashboardService.kycOutcomes(branchCode);
        return ResponseEntity.ok(report);
    }
}
