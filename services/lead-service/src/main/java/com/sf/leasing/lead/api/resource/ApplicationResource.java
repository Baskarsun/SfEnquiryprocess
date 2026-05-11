package com.sf.leasing.lead.api.resource;

import com.sf.leasing.lead.api.dto.request.InitiateApplicationRequest;
import com.sf.leasing.lead.api.dto.request.InitiateCamRequest;
import com.sf.leasing.lead.api.dto.request.UploadDocumentRequest;
import com.sf.leasing.lead.api.dto.response.ApplicationResponse;
import com.sf.leasing.lead.domain.model.CamWorkflow;
import com.sf.leasing.lead.service.ApplicationService;
import com.sf.leasing.lead.service.CamWorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Application", description = "PP7: Application origination, KYC upload, CAM workflow")
public class ApplicationResource {

    private final ApplicationService applicationService;
    private final CamWorkflowService camWorkflowService;

    public ApplicationResource(ApplicationService applicationService,
                                CamWorkflowService camWorkflowService) {
        this.applicationService = applicationService;
        this.camWorkflowService = camWorkflowService;
    }

    // -------------------------------------------------------
    // PP7.1: Initiate Application
    // -------------------------------------------------------

    /**
     * POST /api/v1/prospects/{prospectId}/applications
     * PP7.1: Initiate an Application from a locked Quote.
     */
    @PostMapping("/prospects/{prospectId}/applications")
    @Operation(summary = "Initiate application for a prospect (PP7.1)")
    public ResponseEntity<ApplicationResponse> initiateApplication(
        @PathVariable("prospectId") String prospectId,
        @Valid @RequestBody InitiateApplicationRequest req,
        @RequestHeader("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(401).build();
        }
        ApplicationResponse app = applicationService.initiateApplication(prospectId, req, userId);
        return ResponseEntity.status(201).body(app);
    }

    // -------------------------------------------------------
    // PP7.5: List applications for Prospect (status display)
    // -------------------------------------------------------

    /**
     * GET /api/v1/prospects/{prospectId}/applications
     * PP7.5: List all applications; KYC status and CAM status visible at Prospect screen.
     */
    @GetMapping("/prospects/{prospectId}/applications")
    @Operation(summary = "List applications for prospect with KYC/CAM status (PP7.5)")
    public ResponseEntity<List<ApplicationResponse>> listApplications(
        @PathVariable("prospectId") String prospectId,
        @RequestHeader("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(401).build();
        }
        List<ApplicationResponse> apps = applicationService.listByProspect(prospectId);
        return ResponseEntity.ok(apps);
    }

    /**
     * GET /api/v1/applications/{applicationId}
     * PP7.5: Get application by ID.
     */
    @GetMapping("/applications/{applicationId}")
    @Operation(summary = "Get application by ID (PP7.5)")
    public ResponseEntity<ApplicationResponse> getApplication(
        @PathVariable("applicationId") String applicationId,
        @RequestHeader("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(applicationService.getByApplicationId(applicationId));
    }

    // -------------------------------------------------------
    // PP7.3: KYC Document Upload
    // -------------------------------------------------------

    /**
     * POST /api/v1/applications/{applicationId}/documents
     * PP7.3: Upload a KYC document. Routes to DMS (T/B/L per ENV_INDICATOR).
     */
    @PostMapping("/applications/{applicationId}/documents")
    @Operation(summary = "Upload KYC document to DMS (PP7.3)")
    public ResponseEntity<ApplicationResponse> uploadDocument(
        @PathVariable("applicationId") String applicationId,
        @Valid @RequestBody UploadDocumentRequest req,
        @RequestHeader("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(401).build();
        }
        ApplicationResponse app = applicationService.uploadDocument(applicationId, req, userId);
        return ResponseEntity.ok(app);
    }

    // -------------------------------------------------------
    // PP7.8: CAM Workflow
    // -------------------------------------------------------

    /**
     * POST /api/v1/applications/{applicationId}/cam/initiate
     * PP7.8: Initiate CAM workflow (parallel to KYC).
     */
    @PostMapping("/applications/{applicationId}/cam/initiate")
    @Operation(summary = "Initiate CAM workflow (PP7.8)")
    public ResponseEntity<CamWorkflow> initiateCam(
        @PathVariable("applicationId") String applicationId,
        @RequestBody InitiateCamRequest req,
        @RequestHeader("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(401).build();
        }
        CamWorkflow cam = camWorkflowService.initiateCam(applicationId, req, userId);
        return ResponseEntity.ok(cam);
    }

    /**
     * POST /api/v1/applications/{applicationId}/cam/decision
     * PP7.8: Record CAM approval or decline decision (with optional sanction ID).
     */
    @PostMapping("/applications/{applicationId}/cam/decision")
    @Operation(summary = "Record CAM approval/decline decision (PP7.8)")
    public ResponseEntity<CamWorkflow> camDecision(
        @PathVariable("applicationId") String applicationId,
        @RequestBody Map<String, String> body,
        @RequestHeader("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(401).build();
        }
        String decision        = body.getOrDefault("decision", "");
        String sanctionId      = body.get("sanctionId");
        String sanctionPackage = body.get("sanctionPackage");
        String declineReason   = body.get("declineReason");
        String decidedBy       = body.getOrDefault("decidedBy", userId);

        CamWorkflow cam = camWorkflowService.processDecision(
            applicationId, decision, sanctionId, sanctionPackage, declineReason, decidedBy);
        return ResponseEntity.ok(cam);
    }

    /**
     * GET /api/v1/applications/{applicationId}/cam
     * PP7.8: Get CAM workflow status for an Application.
     */
    @GetMapping("/applications/{applicationId}/cam")
    @Operation(summary = "Get CAM workflow status (PP7.8)")
    public ResponseEntity<CamWorkflow> getCam(
        @PathVariable("applicationId") String applicationId,
        @RequestHeader("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(401).build();
        }
        CamWorkflow cam = camWorkflowService.getByApplicationId(applicationId);
        return ResponseEntity.ok(cam);
    }
}
