package com.sf.leasing.lead.api.resource;

import com.sf.leasing.lead.api.dto.request.CloseLeadRequest;
import com.sf.leasing.lead.api.dto.request.RunDedupRequest;
import com.sf.leasing.lead.domain.enums.DedupLabel;
import com.sf.leasing.lead.domain.model.Lead;
import com.sf.leasing.lead.domain.model.LeadDedupResult;
import com.sf.leasing.lead.service.DeduplicationService;
import com.sf.leasing.lead.service.LeadQualificationService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

/**
 * LP4 + LP8: Deduplication and Lead Qualification endpoints.
 *
 * POST /api/v1/leads/{lrn}/dedup          — run dedup checks
 * GET  /api/v1/leads/{lrn}/dedup          — view dedup results
 * POST /api/v1/leads/{lrn}/validate       — pre-promotion validation (LP8)
 * POST /api/v1/leads/{lrn}/close          — close lead with mandatory reason
 */
@Path("/api/v1/leads")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Lead Qualification", description = "LP4 deduplication and LP8 pre-promotion validation")
public class QualificationResource {

    @Inject
    DeduplicationService deduplicationService;

    @Inject
    LeadQualificationService qualificationService;

    /**
     * POST /api/v1/leads/{lrn}/dedup
     * LP4: Run identity deduplication on an existing lead.
     */
    @POST
    @Path("/{lrn}/dedup")
    @Operation(summary = "Run deduplication checks on a lead (LP4)")
    public Response runDedup(
        @PathParam("lrn") String lrn,
        RunDedupRequest req,
        @HeaderParam("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        Lead lead = Lead.findByLrn(lrn);
        if (lead == null) return Response.status(Response.Status.NOT_FOUND).build();

        boolean includeExternal = req == null || req.includeExternalChecks;
        DedupLabel label = deduplicationService.runDedupChecks(lead, includeExternal);

        return Response.ok(Map.of("lrn", lrn, "dedupLabel", label.name())).build();
    }

    /**
     * GET /api/v1/leads/{lrn}/dedup
     * View all recorded dedup results for a lead.
     */
    @GET
    @Path("/{lrn}/dedup")
    @Operation(summary = "Retrieve deduplication results for a lead (LP4)")
    public Response getDedupResults(
        @PathParam("lrn") String lrn,
        @HeaderParam("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        Lead lead = Lead.findByLrn(lrn);
        if (lead == null) return Response.status(Response.Status.NOT_FOUND).build();

        List<LeadDedupResult> results = LeadDedupResult.findByLeadId(lead.id);
        return Response.ok(results).build();
    }

    /**
     * POST /api/v1/leads/{lrn}/validate
     * LP8: Run pre-promotion validation checklist.
     * Returns 200 with any warnings if all checks pass.
     * Returns 422 with error detail if any check fails.
     */
    @POST
    @Path("/{lrn}/validate")
    @Operation(summary = "Run LP8 pre-promotion validation (LP8)")
    public Response validateForPromotion(
        @PathParam("lrn") String lrn,
        @HeaderParam("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        List<String> warnings = qualificationService.validateForPromotion(lrn);
        return Response.ok(Map.of(
            "lrn", lrn,
            "status", "ELIGIBLE",
            "warnings", warnings
        )).build();
    }

    /**
     * POST /api/v1/leads/{lrn}/close
     * LP8 closure sub-flow: ClosureReason mandatory; record locked post-closure.
     */
    @POST
    @Path("/{lrn}/close")
    @Operation(summary = "Close a lead with mandatory closure reason (LP8)")
    public Response closeLead(
        @PathParam("lrn") String lrn,
        @Valid CloseLeadRequest req,
        @HeaderParam("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        qualificationService.closeLead(lrn, req, userId);
        return Response.ok(Map.of("lrn", lrn, "status", "CLOSED")).build();
    }
}
