package com.sf.leasing.lead.api.resource;

import com.sf.leasing.lead.api.dto.request.LogInteractionRequest;
import com.sf.leasing.lead.domain.model.Interaction;
import com.sf.leasing.lead.service.InteractionService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/api/v1/leads/{lrn}/interactions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Interactions", description = "LP6: Interaction Logging & Next Action Scheduling")
public class InteractionResource {

    @Inject
    InteractionService interactionService;

    /**
     * POST /api/v1/leads/{lrn}/interactions
     * LP6: Log an interaction. Immutable once created.
     * For In-Progress leads: nextActionDate and nextActionMode are mandatory (Rule LP6.2).
     */
    @POST
    @Operation(summary = "Log an interaction for a lead (LP6)")
    public Response logInteraction(
        @PathParam("lrn") String lrn,
        @Valid LogInteractionRequest req,
        @HeaderParam("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        Interaction interaction = interactionService.logInteraction(lrn, req, userId);
        return Response.status(Response.Status.CREATED).entity(interaction).build();
    }

    /**
     * GET /api/v1/leads/{lrn}/interactions
     * Returns immutable interaction history ordered by timestamp.
     */
    @GET
    @Operation(summary = "Get interaction history for a lead")
    public Response getInteractionHistory(
        @PathParam("lrn") String lrn,
        @HeaderParam("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        List<Interaction> history = Interaction.find(
            "lead.lrn = ?1 ORDER BY interactionTimestamp ASC", lrn
        ).list();
        return Response.ok(history).build();
    }
}
