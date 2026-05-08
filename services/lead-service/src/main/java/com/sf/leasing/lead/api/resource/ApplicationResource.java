package com.sf.leasing.lead.api.resource;

import com.sf.leasing.lead.api.dto.request.InitiateApplicationRequest;
import com.sf.leasing.lead.api.dto.request.InitiateCamRequest;
import com.sf.leasing.lead.api.dto.request.UploadDocumentRequest;
import com.sf.leasing.lead.api.dto.response.ApplicationResponse;
import com.sf.leasing.lead.domain.model.CamWorkflow;
import com.sf.leasing.lead.service.ApplicationService;
import com.sf.leasing.lead.service.CamWorkflowService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Application", description = "PP7: Application origination, KYC upload, CAM workflow")
public class ApplicationResource {

    @Inject
    ApplicationService applicationService;

    @Inject
    CamWorkflowService camWorkflowService;

    // -------------------------------------------------------
    // PP7.1: Initiate Application
    // -------------------------------------------------------

    /**
     * POST /api/v1/prospects/{prospectId}/applications
     * PP7.1: Initiate an Application from a locked Quote.
     */
    @POST
    @Path("/prospects/{prospectId}/applications")
    @Operation(summary = "Initiate application for a prospect (PP7.1)")
    public Response initiateApplication(
        @PathParam("prospectId") String prospectId,
        @Valid InitiateApplicationRequest req,
        @HeaderParam("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        ApplicationResponse app = applicationService.initiateApplication(prospectId, req, userId);
        return Response.status(Response.Status.CREATED).entity(app).build();
    }

    // -------------------------------------------------------
    // PP7.5: List applications for Prospect (status display)
    // -------------------------------------------------------

    /**
     * GET /api/v1/prospects/{prospectId}/applications
     * PP7.5: List all applications; KYC status and CAM status visible at Prospect screen.
     */
    @GET
    @Path("/prospects/{prospectId}/applications")
    @Operation(summary = "List applications for prospect with KYC/CAM status (PP7.5)")
    public Response listApplications(
        @PathParam("prospectId") String prospectId,
        @HeaderParam("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        List<ApplicationResponse> apps = applicationService.listByProspect(prospectId);
        return Response.ok(apps).build();
    }

    /**
     * GET /api/v1/applications/{applicationId}
     * PP7.5: Get application by ID.
     */
    @GET
    @Path("/applications/{applicationId}")
    @Operation(summary = "Get application by ID (PP7.5)")
    public Response getApplication(
        @PathParam("applicationId") String applicationId,
        @HeaderParam("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        return Response.ok(applicationService.getByApplicationId(applicationId)).build();
    }

    // -------------------------------------------------------
    // PP7.3: KYC Document Upload
    // -------------------------------------------------------

    /**
     * POST /api/v1/applications/{applicationId}/documents
     * PP7.3: Upload a KYC document. Routes to DMS (T/B/L per ENV_INDICATOR).
     */
    @POST
    @Path("/applications/{applicationId}/documents")
    @Operation(summary = "Upload KYC document to DMS (PP7.3)")
    public Response uploadDocument(
        @PathParam("applicationId") String applicationId,
        @Valid UploadDocumentRequest req,
        @HeaderParam("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        ApplicationResponse app = applicationService.uploadDocument(applicationId, req, userId);
        return Response.ok(app).build();
    }

    // -------------------------------------------------------
    // PP7.8: CAM Workflow
    // -------------------------------------------------------

    /**
     * POST /api/v1/applications/{applicationId}/cam/initiate
     * PP7.8: Initiate CAM workflow (parallel to KYC).
     */
    @POST
    @Path("/applications/{applicationId}/cam/initiate")
    @Operation(summary = "Initiate CAM workflow (PP7.8)")
    public Response initiateCam(
        @PathParam("applicationId") String applicationId,
        InitiateCamRequest req,
        @HeaderParam("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        CamWorkflow cam = camWorkflowService.initiateCam(applicationId, req, userId);
        return Response.ok(cam).build();
    }

    /**
     * POST /api/v1/applications/{applicationId}/cam/decision
     * PP7.8: Record CAM approval or decline decision (with optional sanction ID).
     */
    @POST
    @Path("/applications/{applicationId}/cam/decision")
    @Operation(summary = "Record CAM approval/decline decision (PP7.8)")
    public Response camDecision(
        @PathParam("applicationId") String applicationId,
        Map<String, String> body,
        @HeaderParam("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String decision        = body.getOrDefault("decision", "");
        String sanctionId      = body.get("sanctionId");
        String sanctionPackage = body.get("sanctionPackage");
        String declineReason   = body.get("declineReason");
        String decidedBy       = body.getOrDefault("decidedBy", userId);

        CamWorkflow cam = camWorkflowService.processDecision(
            applicationId, decision, sanctionId, sanctionPackage, declineReason, decidedBy);
        return Response.ok(cam).build();
    }

    /**
     * GET /api/v1/applications/{applicationId}/cam
     * PP7.8: Get CAM workflow status for an Application.
     */
    @GET
    @Path("/applications/{applicationId}/cam")
    @Operation(summary = "Get CAM workflow status (PP7.8)")
    public Response getCam(
        @PathParam("applicationId") String applicationId,
        @HeaderParam("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        CamWorkflow cam = camWorkflowService.getByApplicationId(applicationId);
        return Response.ok(cam).build();
    }
}
