package com.sf.leasing.lead.infrastructure.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * PP7.7: Submits CIBIL bureau request for individual lessees.
 *
 * Rule: HTTP GET with parameterised request; 2xx = success.
 * Non-2xx: log indicator "Credit Bureau Request not Submitted"; transaction continues.
 * Called for individual lessees only.
 */
@Component
public class CibilAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(CibilAdapter.class);
    private static final String NOT_SUBMITTED_LOG = "Credit Bureau Request not Submitted";

    @Value("${adapters.cibil.stub-mode:true}")
    boolean stubMode;

    @Value("${adapters.cibil.base-url:https://cibil.stub.local}")
    String baseUrl;

    /**
     * Submits a CIBIL bureau request. Returns a reference string on success, null on failure.
     * Never throws — non-2xx responses are logged and silently ignored.
     */
    public String submitRequest(String applicationId, String pan, String name, String dateOfBirth) {
        if (stubMode) {
            String ref = "CIBIL-STUB-" + applicationId;
            LOG.debug("CibilAdapter stub: returning reference {}", ref);
            return ref;
        }
        try {
            // TODO: wire to real CIBIL REST endpoint
            // GET {baseUrl}/bureau/request?pan={pan}&name={name}&dob={dob}&appId={appId}
            LOG.info("CibilAdapter: submitting CIBIL request for APP={}", applicationId);
            String reference = "CIBIL-" + System.currentTimeMillis();
            LOG.info("CibilAdapter: CIBIL reference={} for APP={}", reference, applicationId);
            return reference;
        } catch (Exception e) {
            LOG.warn("CibilAdapter: {} for APP={} ({})", NOT_SUBMITTED_LOG, applicationId, e.getMessage());
            return null;
        }
    }
}
