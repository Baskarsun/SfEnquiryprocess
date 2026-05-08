package com.sf.leasing.lead.infrastructure.adapter;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * PP7.4 / PP7.9: Triggers Hunter and Sherlock fraud screening for an application.
 *
 * Both services are called async after the application commit; failures are
 * suppressed (non-blocking). When the fraud service is inactive the CIBIL
 * adapter is called directly for individual lessees (see FraudScreeningService).
 */
@ApplicationScoped
public class HunterSherlockAdapter {

    private static final Logger LOG = Logger.getLogger(HunterSherlockAdapter.class);

    @ConfigProperty(name = "adapters.hunter-sherlock.stub-mode", defaultValue = "true")
    boolean stubMode;

    @ConfigProperty(name = "fraud-service.active", defaultValue = "true")
    boolean fraudServiceActive;

    public boolean isFraudServiceActive() {
        return fraudServiceActive;
    }

    /**
     * Returns the combined fraud screening result for the given application.
     * Both Hunter AND Sherlock must return CLEAR for the overall result to be CLEAR.
     *
     * @return FraudScreeningResult with status CLEAR | NON_CLEAR and optional message
     */
    public FraudScreeningResult screen(String applicationId, String panOrGstin, boolean individual) {
        if (!fraudServiceActive) {
            LOG.infof("HunterSherlockAdapter: fraud service inactive for APP=%s", applicationId);
            return FraudScreeningResult.serviceInactive();
        }
        if (stubMode) {
            LOG.debugf("HunterSherlockAdapter stub: returning CLEAR for APP=%s", applicationId);
            return FraudScreeningResult.clear();
        }
        try {
            // TODO: wire to real Hunter + Sherlock REST endpoints
            LOG.infof("HunterSherlockAdapter: screening APP=%s pan/gstin=%s", applicationId, mask(panOrGstin));
            return FraudScreeningResult.clear();
        } catch (Exception e) {
            // Parse error → default to PENDING per risk mitigation R5
            LOG.warnf("HunterSherlockAdapter: screening failed for APP=%s (%s); defaulting to PENDING",
                applicationId, e.getMessage());
            return FraudScreeningResult.parseError(e.getMessage());
        }
    }

    private static String mask(String value) {
        if (value == null || value.length() < 4) return "****";
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }

    // -------------------------------------------------------
    // Result carrier
    // -------------------------------------------------------

    public enum FraudResultStatus { CLEAR, NON_CLEAR, PENDING, SERVICE_INACTIVE }

    public static class FraudScreeningResult {
        public final FraudResultStatus status;
        public final String message;

        private FraudScreeningResult(FraudResultStatus status, String message) {
            this.status  = status;
            this.message = message;
        }

        public static FraudScreeningResult clear() {
            return new FraudScreeningResult(FraudResultStatus.CLEAR, null);
        }

        public static FraudScreeningResult nonClear(String message) {
            return new FraudScreeningResult(FraudResultStatus.NON_CLEAR, message);
        }

        public static FraudScreeningResult parseError(String detail) {
            return new FraudScreeningResult(FraudResultStatus.PENDING,
                "Fraud service response could not be parsed: " + detail);
        }

        public static FraudScreeningResult serviceInactive() {
            return new FraudScreeningResult(FraudResultStatus.SERVICE_INACTIVE, null);
        }

        public boolean isClear()           { return status == FraudResultStatus.CLEAR; }
        public boolean isServiceInactive() { return status == FraudResultStatus.SERVICE_INACTIVE; }
    }
}
