package com.sf.leasing.lead.api.resource;

import com.sf.leasing.lead.api.dto.response.DashboardResponse;
import com.sf.leasing.lead.service.ProspectDashboardService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/dashboards/prospects")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "ProspectDashboard", description = "Phase 3 dashboards: Pipeline, KYC, Conversion")
public class ProspectDashboardResource {

    @Inject
    ProspectDashboardService dashboardService;

    /**
     * GET /api/v1/dashboards/prospects/pipeline?branchCode=
     * Prospect count grouped by status (DRAFT / VALIDATED / ACTIVE / IN_APPRAISAL / etc.).
     */
    @GET
    @Path("/pipeline")
    @Operation(summary = "Prospect pipeline by status")
    public Response pipeline(@QueryParam("branchCode") String branchCode) {
        DashboardResponse report = dashboardService.prospectPipeline(branchCode);
        return Response.ok(report).build();
    }

    /**
     * GET /api/v1/dashboards/prospects/exception-queue
     * Summary of prospect validation exception queue entries by status.
     */
    @GET
    @Path("/exception-queue")
    @Operation(summary = "Prospect validation exception queue summary")
    public Response exceptionQueue(@QueryParam("branchCode") String branchCode) {
        DashboardResponse report = dashboardService.exceptionQueueSummary(branchCode);
        return Response.ok(report).build();
    }

    /**
     * GET /api/v1/dashboards/prospects/conversion?branchCode=
     * Lead → Prospect conversion metrics for the last 30 days.
     */
    @GET
    @Path("/conversion")
    @Operation(summary = "Lead-to-Prospect conversion metrics (30d)")
    public Response conversion(@QueryParam("branchCode") String branchCode) {
        DashboardResponse report = dashboardService.conversionMetrics(branchCode);
        return Response.ok(report).build();
    }

    /**
     * GET /api/v1/dashboards/prospects/kyc?branchCode=
     * KYC outcome breakdown: SUCCESS / FAILED / OVERRIDDEN.
     */
    @GET
    @Path("/kyc")
    @Operation(summary = "KYC validation outcomes breakdown")
    public Response kycOutcomes(@QueryParam("branchCode") String branchCode) {
        DashboardResponse report = dashboardService.kycOutcomes(branchCode);
        return Response.ok(report).build();
    }
}
