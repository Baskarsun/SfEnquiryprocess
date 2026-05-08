package com.sf.leasing.lead.infrastructure.adapter;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Adapter for the external Caution List service (LP4.3).
 *
 * Rule: PAN on caution list → error LN5337 (hard block).
 * Production wiring: replace stubMode=false and set caution-list.base-url to the real endpoint.
 */
@ApplicationScoped
public class CautionListAdapter {

    private static final Logger LOG = Logger.getLogger(CautionListAdapter.class);

    @ConfigProperty(name = "adapters.caution-list.stub-mode", defaultValue = "true")
    boolean stubMode;

    public enum CautionStatus {
        ALLOWED,
        BLOCKED,
        UNKNOWN
    }

    /**
     * Check whether a PAN appears on the caution list.
     * Returns ALLOWED when no match; BLOCKED when a match is found.
     * Returns UNKNOWN on service error (caller should treat cautiously).
     */
    public CautionStatus checkPan(String pan) {
        if (stubMode) {
            LOG.debugf("Caution list [STUB]: PAN=%s → ALLOWED", pan);
            return CautionStatus.ALLOWED;
        }
        // TODO: wire to real caution list REST endpoint
        LOG.warnf("Caution list adapter is not configured. Returning UNKNOWN for PAN=%s", pan);
        return CautionStatus.UNKNOWN;
    }
}
