package com.sf.leasing.lead.infrastructure.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Adapter for the external Caution List service (LP4.3).
 *
 * Rule: PAN on caution list → error LN5337 (hard block).
 * Production wiring: replace stubMode=false and set caution-list.base-url to the real endpoint.
 */
@Component
public class CautionListAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(CautionListAdapter.class);

    @Value("${adapters.caution-list.stub-mode:true}")
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
            LOG.debug("Caution list [STUB]: PAN={} → ALLOWED", pan);
            return CautionStatus.ALLOWED;
        }
        // TODO: wire to real caution list REST endpoint
        LOG.warn("Caution list adapter is not configured. Returning UNKNOWN for PAN={}", pan);
        return CautionStatus.UNKNOWN;
    }
}
