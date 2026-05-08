package com.sf.leasing.lead.infrastructure.adapter;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * PP7.4: Routes KYC document uploads to the DMS (Document Management System).
 *
 * Environment is controlled by system parameter ENV_INDICATOR:
 *   T → Test DMS endpoint
 *   B → Beta DMS endpoint
 *   L → Live (Production) DMS endpoint
 *
 * Returns the DMS document index on success; throws on failure (caller queues retry).
 */
@ApplicationScoped
public class DmsAdapter {

    private static final Logger LOG = Logger.getLogger(DmsAdapter.class);

    @ConfigProperty(name = "adapters.dms.stub-mode", defaultValue = "true")
    boolean stubMode;

    @ConfigProperty(name = "env.indicator", defaultValue = "T")
    String envIndicator;

    @ConfigProperty(name = "adapters.dms.url.test",  defaultValue = "https://dms-test.stub.local")
    String testUrl;

    @ConfigProperty(name = "adapters.dms.url.beta",  defaultValue = "https://dms-beta.stub.local")
    String betaUrl;

    @ConfigProperty(name = "adapters.dms.url.live",  defaultValue = "https://dms-live.stub.local")
    String liveUrl;

    public String getActiveEnvironment() {
        return envIndicator;
    }

    /**
     * Uploads a document archive to the DMS and returns the document index.
     *
     * @param applicationId Application business ID
     * @param documentType  PHOTO | DRIVING_LICENCE | PAN | PASSPORT | OTHER_KYC
     * @param applicantPrefix MA | A1 | A2
     * @param documentName  Original file name
     * @param documentBytes Raw bytes of the document/archive
     * @return DMS document index
     */
    public String uploadDocument(String applicationId,
                                  String documentType,
                                  String applicantPrefix,
                                  String documentName,
                                  byte[] documentBytes) {
        String endpoint = resolveEndpoint();
        if (stubMode) {
            String index = "DMS-" + envIndicator + "-" + applicationId + "-" + documentType + "-" + applicantPrefix;
            LOG.debugf("DmsAdapter stub (%s): returning index %s", endpoint, index);
            return index;
        }
        try {
            // TODO: wire to real DMS REST endpoint
            // POST {endpoint}/documents with multipart form data
            LOG.infof("DmsAdapter: uploading %s/%s for APP=%s to %s", documentType, applicantPrefix, applicationId, endpoint);
            return "DMS-" + System.currentTimeMillis() + "-" + applicationId;
        } catch (Exception e) {
            LOG.errorf("DmsAdapter: upload failed for APP=%s doc=%s (%s)", applicationId, documentType, e.getMessage());
            throw new RuntimeException("DMS upload failed: " + e.getMessage(), e);
        }
    }

    private String resolveEndpoint() {
        return switch (envIndicator.toUpperCase()) {
            case "B" -> betaUrl;
            case "L" -> liveUrl;
            default  -> testUrl;
        };
    }
}
