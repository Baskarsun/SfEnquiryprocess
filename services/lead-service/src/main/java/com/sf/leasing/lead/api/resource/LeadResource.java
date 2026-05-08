package com.sf.leasing.lead.api.resource;

import com.sf.leasing.lead.api.dto.request.CreateLeadRequest;
import com.sf.leasing.lead.api.dto.response.CreateLeadResponse;
import com.sf.leasing.lead.domain.enums.Channel;
import com.sf.leasing.lead.domain.model.Lead;
import com.sf.leasing.lead.service.LeadCreationService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/api/v1/leads")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Lead Management", description = "LP1-LP2: Lead creation and retrieval")
public class LeadResource {

    @Inject
    LeadCreationService leadCreationService;

    /**
     * POST /api/v1/leads
     * LP1 + LP2: Authenticate user, then create lead record.
     * Headers:
     *   X-User-Id        — authenticated user ID (from API gateway JWT extraction)
     *   X-Device-Id      — primary device IMEI or UUID
     *   X-Device-Id-Alt  — secondary device identifier
     *   X-Channel        — MOBILE | DESKTOP | API | BULK
     *   X-GPS-Lat        — latitude (MOBILE with GPS active)
     *   X-GPS-Lon        — longitude (MOBILE with GPS active)
     */
    @POST
    @Operation(summary = "Create a new leasing lead (LP1 + LP2)")
    public Response createLead(
        @Valid CreateLeadRequest req,
        @HeaderParam("X-User-Id")       String userId,
        @HeaderParam("X-Device-Id")     String primaryDeviceId,
        @HeaderParam("X-Device-Id-Alt") String secondaryDeviceId,
        @HeaderParam("X-Channel")       String channelHeader,
        @HeaderParam("X-GPS-Lat")       Double latitude,
        @HeaderParam("X-GPS-Lon")       Double longitude
    ) {
        // LP1: Authenticate
        leadCreationService.validateUserAndDevice(userId, primaryDeviceId, secondaryDeviceId);

        Channel channel = parseChannel(channelHeader);

        // LP2: Create lead
        CreateLeadResponse result = leadCreationService.createLead(req, userId, channel, latitude, longitude);

        if (result.routedToExceptionQueue) {
            return Response.accepted(result).build();   // 202 — routed to exception queue
        }
        return Response.status(Response.Status.CREATED).entity(result).build();  // 201
    }

    @GET
    @Path("/{lrn}")
    @Operation(summary = "Get lead by LRN")
    public Response getLeadByLrn(
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
        return Response.ok(lead).build();
    }

    @GET
    @Operation(summary = "Search leads by status, assigned user, or branch")
    public Response searchLeads(
        @QueryParam("status")      String status,
        @QueryParam("assignedTo")  String assignedTo,
        @QueryParam("branchCode")  String branchCode,
        @QueryParam("temperature") String temperature,
        @QueryParam("page")        @DefaultValue("0")  int page,
        @QueryParam("size")        @DefaultValue("20") int size,
        @HeaderParam("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        StringBuilder query = new StringBuilder("1=1");
        if (status      != null) query.append(" AND status = '").append(status).append("'");
        if (assignedTo  != null) query.append(" AND assignedUserId = '").append(assignedTo).append("'");
        if (branchCode  != null) query.append(" AND assignedBranchCode = '").append(branchCode).append("'");
        if (temperature != null) query.append(" AND temperature = '").append(temperature).append("'");

        List<Lead> leads = Lead.find(query.toString())
            .page(page, size)
            .list();

        return Response.ok(leads).build();
    }

    private Channel parseChannel(String header) {
        if (header == null) return Channel.DESKTOP;
        try {
            return Channel.valueOf(header.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Channel.DESKTOP;
        }
    }
}
