package com.sf.leasing.lead.api.resource;

import com.sf.leasing.lead.api.dto.request.OverrideKycValidationRequest;
import com.sf.leasing.lead.api.dto.request.TriggerKycValidationRequest;
import com.sf.leasing.lead.api.dto.response.ProspectResponse;
import com.sf.leasing.lead.domain.model.Prospect;
import com.sf.leasing.lead.domain.model.ProspectKycValidation;
import com.sf.leasing.lead.service.ProspectValidationService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/prospects/{prospectId}/kyc")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "ProspectKYC", description = "PP3: External KYC validation and override")
public class ProspectValidationResource {

    @Inject
    ProspectValidationService validationService;

    /**
     * POST /api/v1/prospects/{prospectId}/kyc/validate
     * PP3.1: Trigger external PAN or GSTIN validation.
     * On success → Prospect ID generated, status → VALIDATED.
     * On failure → exception queue with 24-hr SLA.
     */
    @POST
    @Path("/validate")
    @Operation(summary = "Trigger KYC validation for PAN or GSTIN (PP3)")
    public Response triggerValidation(
        @PathParam("prospectId") UUID prospectId,
        @Valid TriggerKycValidationRequest req,
        @HeaderParam("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        Prospect updated = validationService.triggerValidation(
            prospectId, req.validationType, req.triggeredBy);
        return Response.ok(ProspectResponse.from(updated)).build();
    }

    /**
     * POST /api/v1/prospects/{prospectId}/kyc/override
     * PP3.2: Manual override by authorised officer when external validation is inconclusive.
     * Override reason and officer ID are mandatory audit fields.
     */
    @POST
    @Path("/override")
    @Operation(summary = "Manual KYC override by authorised officer (PP3)")
    public Response overrideValidation(
        @PathParam("prospectId") UUID prospectId,
        @Valid OverrideKycValidationRequest req,
        @HeaderParam("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        Prospect updated = validationService.override(
            prospectId, req.validationType,
            req.overrideReasonCode, req.overrideReasonText,
            req.overrideBy != null ? req.overrideBy : userId);
        return Response.ok(ProspectResponse.from(updated)).build();
    }

    /**
     * GET /api/v1/prospects/{prospectId}/kyc/history
     * Returns all KYC validation attempts (immutable audit log).
     */
    @GET
    @Path("/history")
    @Operation(summary = "Get KYC validation history for a prospect (PP3)")
    public Response getKycHistory(
        @PathParam("prospectId") UUID prospectId,
        @HeaderParam("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        List<ProspectKycValidation> history = ProspectKycValidation.findByProspectId(prospectId);
        return Response.ok(history).build();
    }
}
