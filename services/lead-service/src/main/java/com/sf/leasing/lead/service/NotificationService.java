package com.sf.leasing.lead.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Handles all outbound notifications (SMS, email, in-app).
 * Rule LP6.4: SMS dispatch failures are logged silently — never block transactions.
 */
@Service
public class NotificationService {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationService.class);

    /**
     * Async SMS dispatch — logged silently on failure, never throws.
     */
    public void dispatchSmsAsync(String lrn, String userId) {
        try {
            // TODO Phase 1 Week 6: wire to actual SMS provider via async message queue
            LOG.debug("SMS dispatch queued for LRN={}", lrn);
        } catch (Exception e) {
            LOG.warn("SMS dispatch failed for LRN={} (suppressed): {}", lrn, e.getMessage());
        }
    }

    public void notifyFoAbsenceEscalation(String lrn, String absentFo, String escalatedTo) {
        LOG.info("NOTIFY: Lead {} escalated from absent FO {} to Branch Manager {}", lrn, absentFo, escalatedTo);
        // TODO Phase 1 Week 6: dispatch in-app notification
    }

    public void notifySlaBreached(String lrn, String userId, String level) {
        LOG.warn("NOTIFY: SLA breached — LRN={} assigned to {} at level {}", lrn, userId, level);
        // TODO Phase 1 Week 6: dispatch escalation email
    }

    // -------------------------------------------------------
    // Phase 3 — Prospect notifications
    // -------------------------------------------------------

    /** EQ001: KYC validation failed — notify Central Team for manual review. */
    public void notifyKycValidationFailure(String prospectId, String validationType, String reason) {
        LOG.warn("NOTIFY EQ001: KYC validation failed — Prospect={} type={} reason={}",
            prospectId, validationType, reason);
        // TODO Phase 3: dispatch to Central Team in-app + email alert
    }

    /** EQ002: KYC validation succeeded — notify assigned user + manager. */
    public void notifyKycValidationSuccess(String prospectId, String prospectBusinessId) {
        LOG.info("NOTIFY EQ002: KYC validated — Prospect={} businessId={}",
            prospectId, prospectBusinessId);
        // TODO Phase 3: dispatch in-app notification to assigned FO
    }

    /** EQ003: KYC override applied by authorised officer. */
    public void notifyKycOverride(String prospectId, String validationType, String overrideBy) {
        LOG.info("NOTIFY EQ003: KYC override recorded — Prospect={} type={} by={}",
            prospectId, validationType, overrideBy);
        // TODO Phase 3: dispatch audit notification to compliance team
    }

    /** Notify an FO/manager that a prospect has been assigned to them. */
    public void notifyProspectAssigned(String prospectId, String assignedToUserId, String assignedBy) {
        LOG.info("NOTIFY: Prospect assigned — Prospect={} to={} by={}",
            prospectId, assignedToUserId, assignedBy);
        // TODO Phase 3: dispatch in-app + push notification
    }

    /** Notify the marketing employee who sourced the lead that their lead was promoted to a prospect. */
    public void notifyLeadPromoted(String lrn, String prospectUuid, String promotedBy) {
        LOG.info("NOTIFY: Lead promoted to prospect — LRN={} prospectUuid={} by={}",
            lrn, prospectUuid, promotedBy);
        // TODO Phase 3: dispatch in-app notification
    }

    // -------------------------------------------------------
    // Phase 4 — Application notifications
    // -------------------------------------------------------

    /** PP7.11: Hard caution match found post-save — notify Central Team. */
    public void notifyCautionHardError(String applicationId) {
        LOG.warn("NOTIFY: Caution hard error — APP={} blocked for Central Team review (PP7.11)", applicationId);
        // TODO Phase 4: dispatch to Central Team in-app + email alert
    }

    /** PP7.4: Fraud screening returned non-clear result — manual review triggered. */
    public void notifyFraudNonClear(String applicationId, String fraudMessage) {
        LOG.warn("NOTIFY: Fraud non-clear — APP={} message={} (PP7.4)", applicationId, fraudMessage);
        // TODO Phase 4: dispatch to underwriting team review queue
    }

    /** PP7.12: Welcome email + SMS for eligible lease types when app reaches ELIGIBLE_FOR_APPLICATION. */
    public void dispatchWelcomeCommunication(String applicationId, String prospectId, String leaseType) {
        LOG.info("NOTIFY: Welcome communication — APP={} prospectId={} leaseType={} (PP7.12)",
            applicationId, prospectId, leaseType);
        // TODO Phase 4: dispatch email + SMS via async message queue
    }

    /** PP7.8: CAM approved — notify assigned field officer. */
    public void notifyCamApproved(String applicationId, String sanctionId) {
        LOG.info("NOTIFY: CAM approved — APP={} sanctionId={} (PP7.8)", applicationId, sanctionId);
        // TODO Phase 4: dispatch in-app notification + email
    }

    /** PP7.8: CAM declined — notify assigned field officer. */
    public void notifyCamDeclined(String applicationId, String reason) {
        LOG.info("NOTIFY: CAM declined — APP={} reason={} (PP7.8)", applicationId, reason);
        // TODO Phase 4: dispatch in-app notification + email
    }
}
