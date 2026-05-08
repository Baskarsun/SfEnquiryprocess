package com.sf.leasing.lead.infrastructure.adapter;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Adapter for the external UCIC (Unique Customer Identification Code) mapping service (LP4.4).
 *
 * On a successful match the response carries the UCIC code and existing customer codes.
 * These are stored on the Lead for DedupLabel resolution.
 */
@ApplicationScoped
public class UcicMappingAdapter {

    private static final Logger LOG = Logger.getLogger(UcicMappingAdapter.class);

    @ConfigProperty(name = "adapters.ucic.stub-mode", defaultValue = "true")
    boolean stubMode;

    public static class UcicResult {
        public final boolean found;
        public final String ucic;
        public final String existingCustomerCodes;
        public final String riskCategory;

        public UcicResult(boolean found, String ucic, String existingCustomerCodes, String riskCategory) {
            this.found = found;
            this.ucic = ucic;
            this.existingCustomerCodes = existingCustomerCodes;
            this.riskCategory = riskCategory;
        }

        public static UcicResult notFound() {
            return new UcicResult(false, null, null, null);
        }
    }

    /**
     * Look up an existing UCIC by PAN.
     * A match means the lead applicant is a PossibleExisting customer.
     */
    public UcicResult lookupByPan(String pan) {
        if (stubMode) {
            LOG.debugf("UCIC mapping [STUB]: PAN=%s → not found", pan);
            return UcicResult.notFound();
        }
        // TODO: wire to real UCIC REST endpoint
        LOG.warnf("UCIC adapter is not configured. Returning not-found for PAN=%s", pan);
        return UcicResult.notFound();
    }

    /**
     * Look up an existing UCIC by GSTIN.
     */
    public UcicResult lookupByGstin(String gstin) {
        if (stubMode) {
            LOG.debugf("UCIC mapping [STUB]: GSTIN=%s → not found", gstin);
            return UcicResult.notFound();
        }
        LOG.warnf("UCIC adapter is not configured. Returning not-found for GSTIN=%s", gstin);
        return UcicResult.notFound();
    }
}
