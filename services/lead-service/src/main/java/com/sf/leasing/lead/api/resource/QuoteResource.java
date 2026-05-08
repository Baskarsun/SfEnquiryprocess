package com.sf.leasing.lead.api.resource;

import com.sf.leasing.lead.api.dto.request.ApproveQuoteRequest;
import com.sf.leasing.lead.api.dto.request.CreateQuoteRequest;
import com.sf.leasing.lead.api.dto.request.LockUnlockQuoteRequest;
import com.sf.leasing.lead.api.dto.response.QuoteResponse;
import com.sf.leasing.lead.service.QuoteService;
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
@Tag(name = "Quote", description = "PP6: Quote lifecycle — create, approve, share, lock")
public class QuoteResource {

    @Inject
    QuoteService quoteService;

    /**
     * POST /api/v1/opportunities/{opportunityId}/quotes
     * PP6: Create a new quote (rack-rate or customised) for an Opportunity.
     */
    @POST
    @Path("/opportunities/{opportunityId}/quotes")
    @Operation(summary = "Create a quote for an opportunity (PP6.1–PP6.5)")
    public Response createQuote(
        @PathParam("opportunityId") String opportunityId,
        @Valid CreateQuoteRequest req,
        @HeaderParam("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        QuoteResponse quote = quoteService.createQuote(opportunityId, req, userId);
        return Response.status(Response.Status.CREATED).entity(quote).build();
    }

    /**
     * GET /api/v1/opportunities/{opportunityId}/quotes
     * PP6: List all quote versions for an Opportunity.
     */
    @GET
    @Path("/opportunities/{opportunityId}/quotes")
    @Operation(summary = "List all quote versions for an opportunity")
    public Response listQuotes(
        @PathParam("opportunityId") String opportunityId,
        @HeaderParam("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        List<QuoteResponse> quotes = quoteService.listByOpportunity(opportunityId);
        return Response.ok(quotes).build();
    }

    /**
     * GET /api/v1/quotes/{quoteId}
     * PP6: Get a specific quote.
     */
    @GET
    @Path("/quotes/{quoteId}")
    @Operation(summary = "Get quote by ID")
    public Response getQuote(
        @PathParam("quoteId") String quoteId,
        @HeaderParam("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        return Response.ok(quoteService.getByQuoteId(quoteId)).build();
    }

    /**
     * POST /api/v1/quotes/{quoteId}/approve
     * PP6.4: Approve or reject a customised quote.
     */
    @POST
    @Path("/quotes/{quoteId}/approve")
    @Operation(summary = "Approve or reject a customised quote (PP6.4)")
    public Response approveQuote(
        @PathParam("quoteId") String quoteId,
        @Valid ApproveQuoteRequest req,
        @HeaderParam("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        QuoteResponse quote = quoteService.processApproval(quoteId, req, userId);
        return Response.ok(quote).build();
    }

    /**
     * POST /api/v1/quotes/{quoteId}/share
     * PP6.6: Share a quote with the lessee (Draft/Approved → Shared).
     */
    @POST
    @Path("/quotes/{quoteId}/share")
    @Operation(summary = "Share a quote (PP6.6)")
    public Response shareQuote(
        @PathParam("quoteId") String quoteId,
        @HeaderParam("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        QuoteResponse quote = quoteService.shareQuote(quoteId, userId);
        return Response.ok(quote).build();
    }

    /**
     * POST /api/v1/quotes/{quoteId}/lock
     * PP6.6: Lock, request unlock, or approve unlock of a quote.
     * Body action: LOCK | REQUEST_UNLOCK | APPROVE_UNLOCK
     */
    @POST
    @Path("/quotes/{quoteId}/lock")
    @Operation(summary = "Lock / unlock a quote (PP6.6–PP6.7)")
    public Response lockQuote(
        @PathParam("quoteId") String quoteId,
        LockUnlockQuoteRequest req,
        @HeaderParam("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        QuoteResponse quote = quoteService.processLockAction(quoteId, req, userId);
        return Response.ok(quote).build();
    }
}
