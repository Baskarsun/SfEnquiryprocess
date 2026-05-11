package com.sf.leasing.lead.infrastructure.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * External GSTIN validation adapter (PP3.1).
 * GSTIN format: 2-digit state code + 10-digit PAN + 1-digit entity number + 1-digit check + 'Z'.
 * derivedPan = chars at index 2–11 (positions 3–12 in 1-based notation).
 */
@Component
public class GstinValidationAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(GstinValidationAdapter.class);

    @Value("${adapters.gstin-validation.stub-mode:true}")
    boolean stubMode;

    public record GstinValidationResult(
        boolean valid,
        String  legalEntityName,
        String  registeredAddress,
        String  derivedPan,         // chars 2-11 (0-indexed) of the GSTIN — equivalent to the PAN
        String  errorCode,
        String  errorMessage
    ) {
        public static GstinValidationResult success(String legalEntityName, String registeredAddress, String derivedPan) {
            return new GstinValidationResult(true, legalEntityName, registeredAddress, derivedPan, null, null);
        }

        public static GstinValidationResult failure(String errorCode, String message) {
            return new GstinValidationResult(false, null, null, null, errorCode, message);
        }
    }

    public GstinValidationResult validate(String gstin) {
        if (stubMode) {
            LOG.debug("GstinValidationAdapter STUB — GSTIN={}", gstin);
            String derivedPan = (gstin != null && gstin.length() >= 12)
                ? gstin.substring(2, 12)
                : "ABCDE1234F";
            return GstinValidationResult.success(
                "STUB ENTITY PVT LTD",
                "456, Stub Avenue, Delhi - 110001",
                derivedPan
            );
        }

        // TODO: wire real GSTN verification API
        LOG.warn("GstinValidationAdapter real mode not implemented — GSTIN={}", gstin);
        return GstinValidationResult.failure("SERVICE_UNAVAILABLE", "GSTIN validation service not configured.");
    }
}
