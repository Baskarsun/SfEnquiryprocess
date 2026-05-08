package com.sf.leasing.lead.api.resource;

import com.sf.leasing.lead.api.dto.response.DashboardResponse;
import com.sf.leasing.lead.service.DashboardService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Phase 2 enhanced dashboards (Section 5.7 of the implementation plan).
 *
 * GET /api/v1/dashboards/temperature    — temperature distribution
 * GET /api/v1/dashboards/sla            — SLA compliance tracker
 * GET /api/v1/dashboards/aging          — aging report (0-7 / 8-15 / 16-30 / 30+ days)
 * GET /api/v1/dashboards/marketing      — marketing source report
 */
@Path("/api/v1/dashboards")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Dashboards", description = "Phase 2 enhanced dashboards: temperature, SLA, aging, marketing source")
public class DashboardResource {

    @Inject
    DashboardService dashboardService;

    @GET
    @Path("/temperature")
    @Operation(summary = "Lead temperature distribution (Hot / Warm / Cold counts)")
    public Response temperatureDistribution(
        @QueryParam("branchCode")   String branchCode,
        @HeaderParam("X-User-Id")   String userId
    ) {
        if (userId == null || userId.isBlank()) return Response.status(Response.Status.UNAUTHORIZED).build();
        DashboardResponse report = dashboardService.temperatureDistribution(branchCode);
        return Response.ok(report).build();
    }

    @GET
    @Path("/sla")
    @Operation(summary = "SLA compliance tracker (compliant vs. breached assignments)")
    public Response slaCompliance(
        @QueryParam("branchCode")   String branchCode,
        @HeaderParam("X-User-Id")   String userId
    ) {
        if (userId == null || userId.isBlank()) return Response.status(Response.Status.UNAUTHORIZED).build();
        DashboardResponse report = dashboardService.slaComplianceTracker(branchCode);
        return Response.ok(report).build();
    }

    @GET
    @Path("/aging")
    @Operation(summary = "Lead aging report bucketed by days (0-7, 8-15, 16-30, 30+)")
    public Response agingReport(
        @QueryParam("branchCode")   String branchCode,
        @HeaderParam("X-User-Id")   String userId
    ) {
        if (userId == null || userId.isBlank()) return Response.status(Response.Status.UNAUTHORIZED).build();
        DashboardResponse report = dashboardService.agingReport(branchCode);
        return Response.ok(report).build();
    }

    @GET
    @Path("/marketing")
    @Operation(summary = "Leads by Source Category and Source Name")
    public Response marketingSourceReport(
        @QueryParam("branchCode")   String branchCode,
        @HeaderParam("X-User-Id")   String userId
    ) {
        if (userId == null || userId.isBlank()) return Response.status(Response.Status.UNAUTHORIZED).build();
        DashboardResponse report = dashboardService.marketingSourceReport(branchCode);
        return Response.ok(report).build();
    }
}
