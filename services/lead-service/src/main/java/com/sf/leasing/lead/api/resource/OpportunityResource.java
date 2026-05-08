package com.sf.leasing.lead.api.resource;

import com.sf.leasing.lead.api.dto.request.CreateOpportunityRequest;
import com.sf.leasing.lead.api.dto.response.OpportunityResponse;
import com.sf.leasing.lead.service.OpportunityService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Opportunity", description = "PP5: Opportunity management")
public class OpportunityResource {

    @Inject
    OpportunityService opportunityService;

    /**
     * POST /api/v1/prospects/{prospectId}/opportunities
     * PP5: Create a new Opportunity for a Prospect.
     */
    @POST
    @Path("/prospects/{prospectId}/opportunities")
    @Operation(summary = "Create opportunity for prospect (PP5.1–PP5.4)")
    public Response createOpportunity(
        @PathParam("prospectId") String prospectId,
        @Valid CreateOpportunityRequest req,
        @HeaderParam("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        OpportunityResponse opp = opportunityService.createOpportunity(prospectId, req, userId);
        return Response.status(Response.Status.CREATED).entity(opp).build();
    }

    /**
     * GET /api/v1/prospects/{prospectId}/opportunities
     * PP5: List all opportunities for a Prospect.
     */
    @GET
    @Path("/prospects/{prospectId}/opportunities")
    @Operation(summary = "List opportunities for prospect")
    public Response listOpportunities(
        @PathParam("prospectId") String prospectId,
        @HeaderParam("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        List<OpportunityResponse> list = opportunityService.listByProspect(prospectId);
        return Response.ok(list).build();
    }

    /**
     * GET /api/v1/opportunities/{opportunityId}
     * PP5: Get a specific Opportunity by ID.
     */
    @GET
    @Path("/opportunities/{opportunityId}")
    @Operation(summary = "Get opportunity by ID")
    public Response getOpportunity(
        @PathParam("opportunityId") String opportunityId,
        @HeaderParam("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        OpportunityResponse opp = opportunityService.getByOpportunityId(opportunityId);
        return Response.ok(opp).build();
    }
}
