package com.sf.leasing.lead.infrastructure.adapter;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Adapter for the external PAN deduplication API (LP4.5).
 *
 * Response codes per business spec:
 *   COM110 → Confirmed duplicate (hard reject — lead creation blocked)
 *   COM111 → Potential duplicate (warning — proceed with DedupLabel = POSSIBLE_EXISTING)
 *   COM66  → Soft warning (proceed; flag for review)
 *   CLEAR  → No duplicate found
 */
@ApplicationScoped
public class PanDedupAdapter {

    private static final Logger LOG = Logger.getLogger(PanDedupAdapter.class);

    @ConfigProperty(name = "adapters.pan-dedup.stub-mode", defaultValue = "true")
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
            LOG.debugf("PAN dedup [STUB]: PAN=%s → CLEAR", pan);
            return new PanDedupResult(DedupCode.CLEAR, null);
        }
        // TODO: wire to real PAN dedup REST endpoint
        LOG.warnf("PAN dedup adapter is not configured. Returning CLEAR for PAN=%s", pan);
        return new PanDedupResult(DedupCode.CLEAR, null);
    }
}
