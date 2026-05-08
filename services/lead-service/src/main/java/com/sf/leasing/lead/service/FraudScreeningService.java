package com.sf.leasing.lead.service;

import com.sf.leasing.lead.domain.enums.ApplicationStatus;
import com.sf.leasing.lead.domain.enums.CamStatus;
import com.sf.leasing.lead.domain.model.Application;
import com.sf.leasing.lead.domain.model.CamWorkflow;
import com.sf.leasing.lead.infrastructure.adapter.CibilAdapter;
import com.sf.leasing.lead.infrastructure.adapter.HunterSherlockAdapter;
import com.sf.leasing.lead.infrastructure.adapter.HunterSherlockAdapter.FraudScreeningResult;
import com.sf.leasing.lead.infrastructure.messaging.LeadEventProducer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;

/**
 * PP7.4 / PP7.7 / PP7.9 / PP7.11: Post-save fraud and bureau screening.
 *
 * Rules:
 *   - Hunter + Sherlock run async after application commit; failures are non-blocking.
 *   - Both CLEAR → fraud status = CLEAR; CIBIL request initiated for individuals.
 *   - Non-clear → Application → PENDING; fraud message logged; manual review triggered.
 *   - Fraud service inactive → call CIBIL directly for individual lessees.
 */
@ApplicationScoped
public class FraudScreeningService {

    private static final Logger LOG = Logger.getLogger(FraudScreeningService.class);

    @Inject
    HunterSherlockAdapter hunterSherlockAdapter;

    @Inject
    CibilAdapter cibilAdapter;

    @Inject
    NotificationService notificationService;

    @Inject
    LeadEventProducer eventProducer;

    /**
     * Runs fraud screening and CIBIL for the given application.
     * Called asynchronously after the application commit — never throws.
     */
    @Transactional
    public void runPostSaveScreening(String applicationId) {
        try {
            Application app = Application.findByApplicationId(applicationId);
            if (app == null) {
                LOG.warnf("FraudScreeningService: APP=%s not found; skipping", applicationId);
                return;
            }

            String identifier = resolveIdentifier(app);

            if (hunterSherlockAdapter.isFraudServiceActive()) {
                runFraudScreening(app, identifier);
            } else {
                // PP7.9: fraud service inactive → skip fraud, go straight to CIBIL for individuals
                LOG.infof("FraudScreeningService: fraud service inactive for APP=%s; bypassing to CIBIL", applicationId);
                app.fraudStatus      = "CLEAR";
                app.fraudScreenedAt  = LocalDateTime.now();
                app.fraudMessage     = "Fraud service inactive — bypassed (PP7.9)";
                advanceToEligibleIfReady(app);
            }

            // PP7.7: CIBIL for individual lessees
            if (app.individual && !app.cibilSubmitted) {
                submitCibil(app);
            }

            app.updatedAt = LocalDateTime.now();

        } catch (Exception e) {
            // Non-blocking: any unexpected error is logged and suppressed
            LOG.errorf("FraudScreeningService: unexpected error for APP=%s (%s); screening suppressed",
                applicationId, e.getMessage());
        }
    }

    // -------------------------------------------------------
    // Post-save caution list screening (PP7.11)
    // -------------------------------------------------------

    /**
     * PP7.11: Screens all applicants against the caution database after first commit.
     * No hard error → second commit proceeds.
     * Hard error → block second commit; log for Central Team.
     */
    @Transactional
    public boolean runCautionScreening(Application app) {
        try {
            // Delegate to CautionListAdapter (Phase 2 existing adapter)
            // Stub: always returns no hard match
            boolean hardError = false; // TODO: wire to CautionListAdapter.screenApplicants()

            app.cautionScreened    = true;
            app.cautionBlocked     = hardError;
            app.cautionScreenedAt  = LocalDateTime.now();
            app.updatedAt          = LocalDateTime.now();

            if (hardError) {
                LOG.warnf("FraudScreeningService: caution hard error for APP=%s — second commit blocked (PP7.11)",
                    app.applicationId);
                notificationService.notifyCautionHardError(app.applicationId);
                return false;
            }
            return true;
        } catch (Exception e) {
            LOG.warnf("FraudScreeningService: caution screening error for APP=%s (%s); allowing commit",
                app.applicationId, e.getMessage());
            return true;
        }
    }

    // -------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------

    private void runFraudScreening(Application app, String identifier) {
        FraudScreeningResult result = hunterSherlockAdapter.screen(
            app.applicationId, identifier, app.individual);

        app.fraudScreenedAt = LocalDateTime.now();

        switch (result.status) {
            case CLEAR -> {
                app.fraudStatus = "CLEAR";
                advanceToEligibleIfReady(app);
                LOG.infof("FraudScreeningService: APP=%s fraud CLEAR", app.applicationId);
            }
            case NON_CLEAR -> {
                app.fraudStatus  = "NON_CLEAR";
                app.fraudMessage = result.message;
                app.status       = ApplicationStatus.PENDING;
                notificationService.notifyFraudNonClear(app.applicationId, result.message);
                LOG.warnf("FraudScreeningService: APP=%s fraud NON_CLEAR — manual review required", app.applicationId);
            }
            default -> {
                app.fraudStatus  = "PENDING";
                app.fraudMessage = result.message;
                LOG.warnf("FraudScreeningService: APP=%s fraud result PENDING (%s)", app.applicationId, result.message);
            }
        }
    }

    private void submitCibil(Application app) {
        try {
            String ref = cibilAdapter.submitRequest(app.applicationId, null, null, null);
            if (ref != null) {
                app.cibilSubmitted    = true;
                app.cibilReference    = ref;
                app.cibilRequestedAt  = LocalDateTime.now();
            }
        } catch (Exception e) {
            LOG.warnf("FraudScreeningService: CIBIL submission failed for APP=%s (%s); transaction continues",
                app.applicationId, e.getMessage());
        }
    }

    private void advanceToEligibleIfReady(Application app) {
        if (app.status == ApplicationStatus.INITIATED
                || app.status == ApplicationStatus.FRAUD_SCREENING) {
            app.status                  = ApplicationStatus.ELIGIBLE_FOR_APPLICATION;
            app.eligibleForApplication  = true;
            eventProducer.publishApplicationEligible(app.applicationId, app.prospectBusinessId);
        }
    }

    private String resolveIdentifier(Application app) {
        // Prefer PAN; fallback to prospectBusinessId as reference
        return app.prospectBusinessId;
    }
}
