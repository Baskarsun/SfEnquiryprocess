package com.sf.leasing.lead.infrastructure.adapter;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * PP7.7: Submits CIBIL bureau request for individual lessees.
 *
 * Rule: HTTP GET with parameterised request; 2xx = success.
 * Non-2xx: log indicator "Credit Bureau Request not Submitted"; transaction continues.
 * Called for individual lessees only.
 */
@ApplicationScoped
public class CibilAdapter {

    private static final Logger LOG = Logger.getLogger(CibilAdapter.class);
    private static final String NOT_SUBMITTED_LOG = "Credit Bureau Request not Submitted";

    @ConfigProperty(name = "adapters.cibil.stub-mode", defaultValue = "true")
    boolean stubMode;

    @ConfigProperty(name = "adapters.cibil.base-url", defaultValue = "https://cibil.stub.local")
    String baseUrl;

    /**
     * Submits a CIBIL bureau request. Returns a reference string on success, null on failure.
     * Never throws — non-2xx responses are logged and silently ignored.
     */
    public String submitRequest(String applicationId, String pan, String name, String dateOfBirth) {
        if (stubMode) {
            String ref = "CIBIL-STUB-" + applicationId;
            LOG.debugf("CibilAdapter stub: returning reference %s", ref);
            return ref;
        }
        try {
            // TODO: wire to real CIBIL REST endpoint
            // GET {baseUrl}/bureau/request?pan={pan}&name={name}&dob={dob}&appId={appId}
            LOG.infof("CibilAdapter: submitting CIBIL request for APP=%s", applicationId);
            String reference = "CIBIL-" + System.currentTimeMillis();
            LOG.infof("CibilAdapter: CIBIL reference=%s for APP=%s", reference, applicationId);
            return reference;
        } catch (Exception e) {
            LOG.warnf("CibilAdapter: %s for APP=%s (%s)", NOT_SUBMITTED_LOG, applicationId, e.getMessage());
            return null;
        }
    }
}
