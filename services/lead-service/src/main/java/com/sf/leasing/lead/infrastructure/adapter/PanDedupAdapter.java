package com.sf.leasing.lead.infrastructure.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Adapter for the external PAN deduplication API (LP4.5).
 *
 * Response codes per business spec:
 *   COM110 → Confirmed duplicate (hard reject — lead creation blocked)
 *   COM111 → Potential duplicate (warning — proceed with DedupLabel = POSSIBLE_EXISTING)
 *   COM66  → Soft warning (proceed; flag for review)
 *   CLEAR  → No duplicate found
 */
@Component
public class PanDedupAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(PanDedupAdapter.class);

    @Value("${adapters.pan-dedup.stub-mode:true}")
    boolean stubMode;

    public enum DedupCode {
        CLEAR,
        COM66,
        COM111,
        COM110,
        SERVICE_ERROR
    }

    public static class PanDedupResult {
        public final DedupCode code;
        public final String matchedEntityId;

        public PanDedupResult(DedupCode code, String matchedEntityId) {
            this.code = code;
            this.matchedEntityId = matchedEntityId;
        }
    }

    /**
     * Call the external PAN dedup API for the given PAN.
     */
    public PanDedupResult check(String pan) {
        if (stubMode) {
            LOG.debug("PAN dedup [STUB]: PAN={} → CLEAR", pan);
            return new PanDedupResult(DedupCode.CLEAR, null);
        }
        // TODO: wire to real PAN dedup REST endpoint
        LOG.warn("PAN dedup adapter is not configured. Returning CLEAR for PAN={}", pan);
        return new PanDedupResult(DedupCode.CLEAR, null);
    }
}
