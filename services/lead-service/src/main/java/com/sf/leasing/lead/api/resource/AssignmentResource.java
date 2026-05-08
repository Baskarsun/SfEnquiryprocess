package com.sf.leasing.lead.api.resource;

import com.sf.leasing.lead.api.dto.request.AssignLeadRequest;
import com.sf.leasing.lead.domain.model.LeadAssignment;
import com.sf.leasing.lead.service.AssignmentService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/api/v1/leads/{lrn}/assignments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Assignment", description = "LP5: Lead Assignment & Re-assignment")
public class AssignmentResource {

    @Inject
    AssignmentService assignmentService;

    /**
     * POST /api/v1/leads/{lrn}/assignments
     * LP5: Assign or re-assign a lead.
     * Reason code and remarks are mandatory (Rule LP5.2).
     */
    @POST
    @Operation(summary = "Assign or re-assign a lead (LP5)")
    public Response assignLead(
        @PathParam("lrn") String lrn,
        @Valid AssignLeadRequest req,
        @HeaderParam("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        assignmentService.assignLead(lrn, req, userId);
        return Response.ok().entity("{\"message\":\"Lead assigned successfully.\"}").build();
    }

    /**
     * GET /api/v1/leads/{lrn}/assignments
     * Returns immutable assignment audit history.
     */
    @GET
    @Operation(summary = "Get assignment history for a lead")
    public Response getAssignmentHistory(
        @PathParam("lrn") String lrn,
        @HeaderParam("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        List<LeadAssignment> history = LeadAssignment.find(
            "lead.lrn = ?1 ORDER BY assignedAt ASC", lrn
        ).list();
        return Response.ok(history).build();
    }
}
