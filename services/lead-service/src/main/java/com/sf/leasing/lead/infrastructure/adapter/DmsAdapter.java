package com.sf.leasing.lead.infrastructure.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
@Component
public class DmsAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(DmsAdapter.class);

    @Value("${adapters.dms.stub-mode:true}")
    boolean stubMode;

    @Value("${env.indicator:T}")
    String envIndicator;

    @Value("${adapters.dms.url.test:https://dms-test.stub.local}")
    String testUrl;

    @Value("${adapters.dms.url.beta:https://dms-beta.stub.local}")
    String betaUrl;

    @Value("${adapters.dms.url.live:https://dms-live.stub.local}")
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
            LOG.debug("DmsAdapter stub ({}): returning index {}", endpoint, index);
            return index;
        }
        try {
            // TODO: wire to real DMS REST endpoint
            // POST {endpoint}/documents with multipart form data
            LOG.info("DmsAdapter: uploading {}/{} for APP={} to {}", documentType, applicantPrefix, applicationId, endpoint);
            return "DMS-" + System.currentTimeMillis() + "-" + applicationId;
        } catch (Exception e) {
            LOG.error("DmsAdapter: upload failed for APP={} doc={} ({})", applicationId, documentType, e.getMessage());
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
