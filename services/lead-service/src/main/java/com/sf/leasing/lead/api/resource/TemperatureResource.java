package com.sf.leasing.lead.api.resource;

import com.sf.leasing.lead.api.dto.request.OverrideTemperatureRequest;
import com.sf.leasing.lead.domain.model.Lead;
import com.sf.leasing.lead.domain.model.TemperatureAudit;
import com.sf.leasing.lead.service.TemperatureEngineService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

/**
 * LP7: Lead Temperature endpoints.
 *
 * PUT  /api/v1/leads/{lrn}/temperature  — manual override
 * GET  /api/v1/leads/{lrn}/temperature  — current temperature + audit history
 */
@Path("/api/v1/leads")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Lead Temperature", description = "LP7: Temperature classification and manual override")
public class TemperatureResource {

    @Inject
    TemperatureEngineService temperatureEngineService;

    /**
     * PUT /api/v1/leads/{lrn}/temperature
     * LP7.3: Manual temperature override. StatusReason (code + free text) mandatory.
     */
    @PUT
    @Path("/{lrn}/temperature")
    @Operation(summary = "Manually override lead temperature (LP7.3)")
    public Response overrideTemperature(
        @PathParam("lrn") String lrn,
        @Valid OverrideTemperatureRequest req,
        @HeaderParam("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        temperatureEngineService.overrideTemperature(lrn, req, userId);
        return Response.ok("{\"message\":\"Temperature updated successfully\"}").build();
    }

    /**
     * GET /api/v1/leads/{lrn}/temperature
     * Returns current temperature and full audit trail.
     */
    @GET
    @Path("/{lrn}/temperature")
    @Operation(summary = "Get current temperature and history (LP7)")
    public Response getTemperature(
        @PathParam("lrn") String lrn,
        @HeaderParam("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        Lead lead = Lead.findByLrn(lrn);
        if (lead == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        List<TemperatureAudit> history = TemperatureAudit.findByLeadId(lead.id);
        return Response.ok(new TemperatureInfo(lead, history)).build();
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
