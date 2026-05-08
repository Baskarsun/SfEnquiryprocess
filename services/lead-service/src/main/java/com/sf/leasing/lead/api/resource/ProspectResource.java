package com.sf.leasing.lead.api.resource;

import com.sf.leasing.lead.api.dto.request.CloseProspectRequest;
import com.sf.leasing.lead.api.dto.request.PromoteToProspectRequest;
import com.sf.leasing.lead.api.dto.response.ProspectPromotionResponse;
import com.sf.leasing.lead.api.dto.response.ProspectResponse;
import com.sf.leasing.lead.domain.enums.ProspectStatus;
import com.sf.leasing.lead.domain.exception.BusinessException;
import com.sf.leasing.lead.domain.exception.ErrorCodes;
import com.sf.leasing.lead.domain.model.Lineage;
import com.sf.leasing.lead.domain.model.Prospect;
import com.sf.leasing.lead.service.ProspectPromotionService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.LocalDateTime;
import java.util.UUID;

@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Prospect", description = "PP1/LP8: Prospect lifecycle management")
public class ProspectResource {

    @Inject
    ProspectPromotionService promotionService;

    /**
     * POST /api/v1/leads/{lrn}/promote
     * LP8 → PP1: Run 17-point qualification checklist and promote lead to DRAFT Prospect.
     */
    @POST
    @Path("/leads/{lrn}/promote")
    @Operation(summary = "Promote a qualified lead to a Prospect (LP8/PP1)")
    public Response promoteToProspect(
        @PathParam("lrn") String lrn,
        @Valid PromoteToProspectRequest req,
        @HeaderParam("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String promotedBy = req != null && req.promotedBy != null ? req.promotedBy : userId;
        ProspectPromotionResponse response = promotionService.promote(lrn, promotedBy);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    /**
     * GET /api/v1/prospects/{prospectId}
     * PP1: Retrieve prospect with PAN/GSTIN masking and lineage.
     * prospectId may be the UUID (system) or the business ID (PR-YYYY-NNNNNN).
     */
    @GET
    @Path("/prospects/{prospectId}")
    @Operation(summary = "Get prospect details with masked KYC identifiers (PP1)")
    public Response getProspect(
        @PathParam("prospectId") String prospectId,
        @HeaderParam("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        Prospect prospect = resolveProspect(prospectId);
        if (prospect == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"Prospect not found: " + prospectId + "\"}").build();
        }

        ProspectResponse body = ProspectResponse.from(prospect);

        // Attach lineage info
        Lineage lineage = Lineage.findByProspectUuid(prospect.id);
        // lineage.leadLrn and lineage.prospectBusinessId are already reflected in ProspectResponse

        return Response.ok(body).build();
    }

    /**
     * GET /api/v1/leads/{lrn}/prospect
     * PP1: Retrieve prospect linked to a Lead via LRN (lineage view).
     */
    @GET
    @Path("/leads/{lrn}/prospect")
    @Operation(summary = "Get prospect linked to a lead (lineage view, PP1)")
    public Response getProspectByLrn(
        @PathParam("lrn") String lrn,
        @HeaderParam("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        Prospect prospect = Prospect.findByLeadLrn(lrn);
        if (prospect == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"No prospect found for LRN: " + lrn + "\"}").build();
        }

        return Response.ok(ProspectResponse.from(prospect)).build();
    }

    /**
     * POST /api/v1/prospects/{prospectId}/close
     * Close a prospect with mandatory reason code.
     */
    @POST
    @Path("/prospects/{prospectId}/close")
    @Transactional
    @Operation(summary = "Close a prospect with reason (PP1)")
    public Response closeProspect(
        @PathParam("prospectId") String prospectId,
        @Valid CloseProspectRequest req,
        @HeaderParam("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        Prospect prospect = resolveProspect(prospectId);
        if (prospect == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"Prospect not found: " + prospectId + "\"}").build();
        }
        if (prospect.isClosed()) {
            throw new BusinessException(ErrorCodes.PROSPECT_ALREADY_CLOSED,
                "Prospect is already closed.");
        }

        prospect.status            = ProspectStatus.CLOSED;
        prospect.closureReasonCode = req.closureReasonCode;
        prospect.closureReasonText = req.closureReasonText;
        prospect.closedAt          = LocalDateTime.now();
        prospect.closedBy          = req.closedBy != null ? req.closedBy : userId;
        prospect.updatedBy         = userId;
        prospect.updatedAt         = LocalDateTime.now();

        return Response.ok("{\"message\":\"Prospect closed.\"}").build();
    }

    // -------------------------------------------------------

    private Prospect resolveProspect(String id) {
        // Try business ID first (PR-YYYY-NNNNNN)
        if (id.startsWith("PR-")) {
            return Prospect.findByProspectId(id);
        }
        // Try UUID
        try {
            return Prospect.findById(UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            return Prospect.findByProspectId(id);
        }
    }
}
