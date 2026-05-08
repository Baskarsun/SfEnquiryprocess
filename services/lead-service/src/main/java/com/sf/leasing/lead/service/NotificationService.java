package com.sf.leasing.lead.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

/**
 * Handles all outbound notifications (SMS, email, in-app).
 * Rule LP6.4: SMS dispatch failures are logged silently — never block transactions.
 */
@ApplicationScoped
public class NotificationService {

    private static final Logger LOG = Logger.getLogger(NotificationService.class);

    /**
     * Async SMS dispatch — logged silently on failure, never throws.
     */
    public void dispatchSmsAsync(String lrn, String userId) {
        try {
            // TODO Phase 1 Week 6: wire to actual SMS provider via async message queue
            LOG.debugf("SMS dispatch queued for LRN=%s", lrn);
        } catch (Exception e) {
            LOG.warnf("SMS dispatch failed for LRN=%s (suppressed): %s", lrn, e.getMessage());
        }
    }

    public void notifyFoAbsenceEscalation(String lrn, String absentFo, String escalatedTo) {
        LOG.infof("NOTIFY: Lead %s escalated from absent FO %s to Branch Manager %s", lrn, absentFo, escalatedTo);
        // TODO Phase 1 Week 6: dispatch in-app notification
    }

    public void notifySlaBreached(String lrn, String userId, String level) {
        LOG.warnf("NOTIFY: SLA breached — LRN=%s assigned to %s at level %s", lrn, userId, level);
        // TODO Phase 1 Week 6: dispatch escalation email
    }

    // -------------------------------------------------------
    // Phase 3 — Prospect notifications
    // -------------------------------------------------------

    /** EQ001: KYC validation failed — notify Central Team for manual review. */
    public void notifyKycValidationFailure(String prospectId, String validationType, String reason) {
        LOG.warnf("NOTIFY EQ001: KYC validation failed — Prospect=%s type=%s reason=%s",
            prospectId, validationType, reason);
        // TODO Phase 3: dispatch to Central Team in-app + email alert
    }

    /** EQ002: KYC validation succeeded — notify assigned user + manager. */
    public void notifyKycValidationSuccess(String prospectId, String prospectBusinessId) {
        LOG.infof("NOTIFY EQ002: KYC validated — Prospect=%s businessId=%s",
            prospectId, prospectBusinessId);
        // TODO Phase 3: dispatch in-app notification to assigned FO
    }

    /** EQ003: KYC override applied by authorised officer. */
    public void notifyKycOverride(String prospectId, String validationType, String overrideBy) {
        LOG.infof("NOTIFY EQ003: KYC override recorded — Prospect=%s type=%s by=%s",
            prospectId, validationType, overrideBy);
        // TODO Phase 3: dispatch audit notification to compliance team
    }

    /** Notify an FO/manager that a prospect has been assigned to them. */
    public void notifyProspectAssigned(String prospectId, String assignedToUserId, String assignedBy) {
        LOG.infof("NOTIFY: Prospect assigned — Prospect=%s to=%s by=%s",
            prospectId, assignedToUserId, assignedBy);
        // TODO Phase 3: dispatch in-app + push notification
    }

    /** Notify the marketing employee who sourced the lead that their lead was promoted to a prospect. */
    public void notifyLeadPromoted(String lrn, String prospectUuid, String promotedBy) {
        LOG.infof("NOTIFY: Lead promoted to prospect — LRN=%s prospectUuid=%s by=%s",
            lrn, prospectUuid, promotedBy);
        // TODO Phase 3: dispatch in-app notification
    }

    // -------------------------------------------------------
    // Phase 4 — Application notifications
    // -------------------------------------------------------

    /** PP7.11: Hard caution match found post-save — notify Central Team. */
    public void notifyCautionHardError(String applicationId) {
        LOG.warnf("NOTIFY: Caution hard error — APP=%s blocked for Central Team review (PP7.11)", applicationId);
        // TODO Phase 4: dispatch to Central Team in-app + email alert
    }

    /** PP7.4: Fraud screening returned non-clear result — manual review triggered. */
    public void notifyFraudNonClear(String applicationId, String fraudMessage) {
        LOG.warnf("NOTIFY: Fraud non-clear — APP=%s message=%s (PP7.4)", applicationId, fraudMessage);
        // TODO Phase 4: dispatch to underwriting team review queue
    }

    /** PP7.12: Welcome email + SMS for eligible lease types when app reaches ELIGIBLE_FOR_APPLICATION. */
    public void dispatchWelcomeCommunication(String applicationId, String prospectId, String leaseType) {
        LOG.infof("NOTIFY: Welcome communication — APP=%s prospectId=%s leaseType=%s (PP7.12)",
            applicationId, prospectId, leaseType);
        // TODO Phase 4: dispatch email + SMS via async message queue
    }

    /** PP7.8: CAM approved — notify assigned field officer. */
    public void notifyCamApproved(String applicationId, String sanctionId) {
        LOG.infof("NOTIFY: CAM approved — APP=%s sanctionId=%s (PP7.8)", applicationId, sanctionId);
        // TODO Phase 4: dispatch in-app notification + email
    }

    /** PP7.8: CAM declined — notify assigned field officer. */
    public void notifyCamDeclined(String applicationId, String reason) {
        LOG.infof("NOTIFY: CAM declined — APP=%s reason=%s (PP7.8)", applicationId, reason);
        // TODO Phase 4: dispatch in-app notification + email
    }
}
