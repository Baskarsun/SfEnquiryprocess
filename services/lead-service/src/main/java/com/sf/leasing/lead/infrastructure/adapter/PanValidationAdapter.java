package com.sf.leasing.lead.infrastructure.adapter;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * External PAN validation adapter (PP3.1).
 * Stub mode returns synthetic valid data so development and testing proceed without external calls.
 * Real implementation wires to NSDL / CBDT PAN verification API.
 */
@ApplicationScoped
public class PanValidationAdapter {

    private static final Logger LOG = Logger.getLogger(PanValidationAdapter.class);

    @ConfigProperty(name = "adapters.pan-validation.stub-mode", defaultValue = "true")
    boolean stubMode;

    public record PanValidationResult(
        boolean valid,
        String  legalName,
        String  registeredAddress,
        String  errorCode,
        String  errorMessage
    ) {
        public static PanValidationResult success(String legalName, String registeredAddress) {
            return new PanValidationResult(true, legalName, registeredAddress, null, null);
        }

        public static PanValidationResult failure(String errorCode, String message) {
            return new PanValidationResult(false, null, null, errorCode, message);
        }
    }

    public PanValidationResult validate(String pan) {
        if (stubMode) {
            LOG.debugf("PanValidationAdapter STUB — PAN=%s", pan);
            return PanValidationResult.success(
                "STUB LEGAL NAME PVT LTD",
                "123, Stub Lane, Mumbai, Maharashtra - 400001"
            );
        }

        // TODO: wire real CBDT/NSDL endpoint
        LOG.warnf("PanValidationAdapter real mode not implemented — PAN=%s", pan);
        return PanValidationResult.failure("SERVICE_UNAVAILABLE", "PAN validation service not configured.");
    }
}
