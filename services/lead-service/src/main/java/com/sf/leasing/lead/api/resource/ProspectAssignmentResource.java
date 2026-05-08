package com.sf.leasing.lead.api.resource;

import com.sf.leasing.lead.api.dto.request.AssignProspectRequest;
import com.sf.leasing.lead.domain.model.ProspectAssignment;
import com.sf.leasing.lead.service.ProspectAssignmentService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/prospects/{prospectId}/assignments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "ProspectAssignment", description = "PP2: Prospect Assignment Workflow")
public class ProspectAssignmentResource {

    @Inject
    ProspectAssignmentService assignmentService;

    /**
     * POST /api/v1/prospects/{prospectId}/assignments
     * PP2: Assign or re-assign a prospect.
     * Assignment chain: CPU → Branch Manager → Associate FO.
     * Each assignment creates an immutable audit record.
     */
    @POST
    @Operation(summary = "Assign or re-assign a prospect (PP2)")
    public Response assignProspect(
        @PathParam("prospectId") UUID prospectId,
        @Valid AssignProspectRequest req,
        @HeaderParam("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        ProspectAssignment assignment = assignmentService.assign(prospectId, req);
        return Response.status(Response.Status.CREATED).entity(assignment).build();
    }

    /**
     * GET /api/v1/prospects/{prospectId}/assignments
     * PP2: Retrieve full immutable assignment audit history.
     */
    @GET
    @Operation(summary = "Get assignment history for a prospect (PP2)")
    public Response getAssignmentHistory(
        @PathParam("prospectId") UUID prospectId,
        @HeaderParam("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        List<ProspectAssignment> history = assignmentService.getAssignmentHistory(prospectId);
        return Response.ok(history).build();
    }
}
