package com.sf.leasing.lead.service;

import com.sf.leasing.lead.api.dto.request.InitiateCamRequest;
import com.sf.leasing.lead.domain.enums.ApplicationStatus;
import com.sf.leasing.lead.domain.enums.CamStatus;
import com.sf.leasing.lead.domain.exception.BusinessException;
import com.sf.leasing.lead.domain.exception.ErrorCodes;
import com.sf.leasing.lead.domain.model.Application;
import com.sf.leasing.lead.domain.model.CamWorkflow;
import com.sf.leasing.lead.infrastructure.messaging.LeadEventProducer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;

/**
 * PP7.8 / 7.8: CAM (Credit Appraisal Memo) workflow management.
 *
 * CAM runs in parallel with KYC; both are non-blocking to each other.
 * Statuses: NOT_STARTED → IN_PROGRESS → APPROVED | DECLINED
 * Sanction ID and package are displayed on the Prospect screen once issued.
 */
@ApplicationScoped
public class CamWorkflowService {

    private static final Logger LOG = Logger.getLogger(CamWorkflowService.class);

    @Inject
    LeadEventProducer eventProducer;

    @Inject
    NotificationService notificationService;

    // -------------------------------------------------------
    // Initiate CAM (parallel to KYC upload)
    // -------------------------------------------------------

    @Transactional
    public CamWorkflow initiateCam(String applicationId, InitiateCamRequest req, String userId) {
        Application app = resolveApplication(applicationId);

        if (app.modificationBlocked) {
            throw new BusinessException(app.modificationBlockReason != null
                ? app.modificationBlockReason : ErrorCodes.MODIFICATION_CONTRACT_IN_PROGRESS,
                "Application modification is blocked.");
        }

        CamWorkflow existing = CamWorkflow.findByApplication(app.id);
        if (existing != null && existing.camStatus != CamStatus.NOT_STARTED) {
            throw new BusinessException(ErrorCodes.CAM_ALREADY_INITIATED,
                "CAM is already initiated for application: " + applicationId);
        }

        String initiatedBy = req.initiatedBy != null ? req.initiatedBy : userId;
        LocalDateTime now  = LocalDateTime.now();

        CamWorkflow cam      = existing != null ? existing : new CamWorkflow();
        cam.applicationId    = app.id;
        cam.camStatus        = CamStatus.IN_PROGRESS;
        cam.initiatedAt      = now;
        cam.initiatedBy      = initiatedBy;
        cam.createdAt        = existing != null ? existing.createdAt : now;
        cam.updatedAt        = now;
        if (existing == null) cam.persist();

        // Sync cam_status back to Application
        app.camStatus    = CamStatus.IN_PROGRESS;
        app.status       = ApplicationStatus.IN_APPRAISAL;
        app.updatedBy    = userId;
        app.updatedAt    = now;

        LOG.infof("CAM initiated: APP=%s by=%s", applicationId, initiatedBy);
        return cam;
    }

    // -------------------------------------------------------
    // CAM Decision: Approve / Decline
    // -------------------------------------------------------

    @Transactional
    public CamWorkflow processDecision(String applicationId, String decision,
                                       String sanctionId, String sanctionPackage,
                                       String declineReason, String decidedBy) {
        Application app = resolveApplication(applicationId);
        CamWorkflow cam = CamWorkflow.findByApplication(app.id);

        if (cam == null || cam.camStatus != CamStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCodes.CAM_NOT_IN_PROGRESS,
                "CAM is not in progress for application: " + applicationId);
        }

        LocalDateTime now = LocalDateTime.now();

        if ("APPROVE".equalsIgnoreCase(decision)) {
            cam.camStatus       = CamStatus.APPROVED;
            cam.sanctionId      = sanctionId;
            cam.sanctionPackage = sanctionPackage;
            cam.approvedAt      = now;
            cam.approvedBy      = decidedBy;

            app.camStatus       = CamStatus.APPROVED;
            app.sanctionId      = sanctionId;
            app.sanctionPackage = sanctionPackage;
            app.status          = ApplicationStatus.SANCTIONED;

            eventProducer.publishCamApproved(app.applicationId, app.prospectBusinessId, sanctionId, decidedBy);
            notificationService.notifyCamApproved(applicationId, sanctionId);
            LOG.infof("CAM approved: APP=%s sanctionId=%s by=%s", applicationId, sanctionId, decidedBy);

        } else if ("DECLINE".equalsIgnoreCase(decision)) {
            cam.camStatus      = CamStatus.DECLINED;
            cam.declinedAt     = now;
            cam.declinedReason = declineReason;

            app.camStatus      = CamStatus.DECLINED;

            notificationService.notifyCamDeclined(applicationId, declineReason);
            LOG.infof("CAM declined: APP=%s reason=%s by=%s", applicationId, declineReason, decidedBy);

        } else {
            throw new BusinessException(ErrorCodes.CAM_INVALID_DECISION,
                "Decision must be APPROVE or DECLINE.");
        }

        cam.updatedAt   = now;
        app.updatedBy   = decidedBy;
        app.updatedAt   = now;

        return cam;
    }

    // -------------------------------------------------------
    // Query
    // -------------------------------------------------------

    public CamWorkflow getByApplicationId(String applicationId) {
        Application app = resolveApplication(applicationId);
        CamWorkflow cam = CamWorkflow.findByApplication(app.id);
        if (cam == null) {
            throw new BusinessException(ErrorCodes.CAM_NOT_FOUND,
                "No CAM workflow found for application: " + applicationId);
        }
        return cam;
    }

    // -------------------------------------------------------
    // Helper
    // -------------------------------------------------------

    private Application resolveApplication(String applicationId) {
        Application app = Application.findByApplicationId(applicationId);
        if (app == null) {
            throw new BusinessException(ErrorCodes.APPLICATION_NOT_FOUND,
                "Application not found: " + applicationId);
        }
        return app;
    }
}
