package com.sf.leasing.lead.infrastructure.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * External PAN validation adapter (PP3.1).
 * Stub mode returns synthetic valid data so development and testing proceed without external calls.
 * Real implementation wires to NSDL / CBDT PAN verification API.
 */
@Component
public class PanValidationAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(PanValidationAdapter.class);

    @Value("${adapters.pan-validation.stub-mode:true}")
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
            LOG.debug("PanValidationAdapter STUB — PAN={}", pan);
            return PanValidationResult.success(
                "STUB LEGAL NAME PVT LTD",
                "123, Stub Lane, Mumbai, Maharashtra - 400001"
            );
        }

        // TODO: wire real CBDT/NSDL endpoint
        LOG.warn("PanValidationAdapter real mode not implemented — PAN={}", pan);
        return PanValidationResult.failure("SERVICE_UNAVAILABLE", "PAN validation service not configured.");
    }
}
