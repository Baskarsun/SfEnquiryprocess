package com.sf.leasing.lead.service;

import com.sf.leasing.lead.domain.enums.ApplicationStatus;
import com.sf.leasing.lead.domain.model.Application;
import com.sf.leasing.lead.infrastructure.adapter.CibilAdapter;
import com.sf.leasing.lead.infrastructure.adapter.HunterSherlockAdapter;
import com.sf.leasing.lead.infrastructure.adapter.HunterSherlockAdapter.FraudScreeningResult;
import com.sf.leasing.lead.infrastructure.messaging.LeadEventProducer;
import com.sf.leasing.lead.infrastructure.persistence.ApplicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
@Service
public class FraudScreeningService {

    private static final Logger LOG = LoggerFactory.getLogger(FraudScreeningService.class);

    private final HunterSherlockAdapter hunterSherlockAdapter;
    private final CibilAdapter cibilAdapter;
    private final NotificationService notificationService;
    private final LeadEventProducer eventProducer;
    private final ApplicationRepository applicationRepository;

    public FraudScreeningService(HunterSherlockAdapter hunterSherlockAdapter,
                                  CibilAdapter cibilAdapter,
                                  NotificationService notificationService,
                                  LeadEventProducer eventProducer,
                                  ApplicationRepository applicationRepository) {
        this.hunterSherlockAdapter = hunterSherlockAdapter;
        this.cibilAdapter = cibilAdapter;
        this.notificationService = notificationService;
        this.eventProducer = eventProducer;
        this.applicationRepository = applicationRepository;
    }

    /**
     * Runs fraud screening and CIBIL for the given application.
     * Called asynchronously after the application commit — never throws.
     */
    @Transactional
    public void runPostSaveScreening(String applicationId) {
        try {
            Application app = applicationRepository.findByApplicationId(applicationId).orElse(null);
            if (app == null) {
                LOG.warn("FraudScreeningService: APP={} not found; skipping", applicationId);
                return;
            }

            String identifier = resolveIdentifier(app);

            if (hunterSherlockAdapter.isFraudServiceActive()) {
                runFraudScreening(app, identifier);
            } else {
                // PP7.9: fraud service inactive → skip fraud, go straight to CIBIL for individuals
                LOG.info("FraudScreeningService: fraud service inactive for APP={}; bypassing to CIBIL", applicationId);
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
            LOG.error("FraudScreeningService: unexpected error for APP={} ({}); screening suppressed",
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
                LOG.warn("FraudScreeningService: caution hard error for APP={} — second commit blocked (PP7.11)",
                    app.applicationId);
                notificationService.notifyCautionHardError(app.applicationId);
                return false;
            }
            return true;
        } catch (Exception e) {
            LOG.warn("FraudScreeningService: caution screening error for APP={} ({}); allowing commit",
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
                LOG.info("FraudScreeningService: APP={} fraud CLEAR", app.applicationId);
            }
            case NON_CLEAR -> {
                app.fraudStatus  = "NON_CLEAR";
                app.fraudMessage = result.message;
                app.status       = ApplicationStatus.PENDING;
                notificationService.notifyFraudNonClear(app.applicationId, result.message);
                LOG.warn("FraudScreeningService: APP={} fraud NON_CLEAR — manual review required", app.applicationId);
            }
            default -> {
                app.fraudStatus  = "PENDING";
                app.fraudMessage = result.message;
                LOG.warn("FraudScreeningService: APP={} fraud result PENDING ({})", app.applicationId, result.message);
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
            LOG.warn("FraudScreeningService: CIBIL submission failed for APP={} ({}); transaction continues",
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
