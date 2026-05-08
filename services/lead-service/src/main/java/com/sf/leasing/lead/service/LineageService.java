package com.sf.leasing.lead.service;

import com.sf.leasing.lead.api.dto.response.LineageResponse;
import com.sf.leasing.lead.domain.exception.BusinessException;
import com.sf.leasing.lead.domain.exception.ErrorCodes;
import com.sf.leasing.lead.domain.model.Lineage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * PP8.2 — Immutable lineage chain service.
 *
 * The lineage record is append-only: each stage adds its ID column; no
 * columns are ever updated once written. The full chain is:
 *   Lead LRN → Prospect ID → Opportunity ID → Quote ID → Application ID → Customer ID
 *
 * Supports cross-reference queries: given any ID in the chain, return full lineage.
 */
@ApplicationScoped
public class LineageService {

    private static final Logger LOG = Logger.getLogger(LineageService.class);

    // -------------------------------------------------------
    // Cross-reference queries (PP8.2)
    // -------------------------------------------------------

    public LineageResponse getByLeadLrn(String lrn) {
        Lineage lineage = Lineage.findByLeadLrn(lrn);
        return resolve(lineage, "LRN", lrn);
    }

    public LineageResponse getByProspectId(String prospectBusinessId) {
        Lineage lineage = Lineage.find("prospectBusinessId", prospectBusinessId).firstResult();
        return resolve(lineage, "PROSPECT_ID", prospectBusinessId);
    }

    public LineageResponse getByApplicationBusinessId(String applicationBusinessId) {
        Lineage lineage = Lineage.find("applicationBusinessId", applicationBusinessId).firstResult();
        return resolve(lineage, "APPLICATION_ID", applicationBusinessId);
    }

    public LineageResponse getByCustomerId(String customerId) {
        Lineage lineage = Lineage.find("customerId", customerId).firstResult();
        return resolve(lineage, "CUSTOMER_ID", customerId);
    }

    public LineageResponse getByOpportunityBusinessId(String opportunityBusinessId) {
        Lineage lineage = Lineage.find("opportunityBusinessId", opportunityBusinessId).firstResult();
        return resolve(lineage, "OPPORTUNITY_ID", opportunityBusinessId);
    }

    // -------------------------------------------------------
    // Stage updates — called by downstream services when an
    // entity is linked to an existing lineage record
    // -------------------------------------------------------

    @Transactional
    public void linkOpportunity(UUID prospectUuid, UUID opportunityUuid, String opportunityBusinessId) {
        Lineage lineage = Lineage.findByProspectUuid(prospectUuid);
        if (lineage == null) {
            LOG.warnf("Lineage not found for prospect=%s; opportunity link skipped.", prospectUuid);
            return;
        }
        if (lineage.opportunityUuid == null) {
            lineage.opportunityUuid        = opportunityUuid;
            lineage.opportunityBusinessId  = opportunityBusinessId;
            lineage.updatedAt              = java.time.LocalDateTime.now();
            LOG.infof("Lineage linked opportunity: PROSPECT=%s OPP=%s", prospectUuid, opportunityBusinessId);
        }
    }

    @Transactional
    public void linkQuote(UUID prospectUuid, UUID quoteUuid, String quoteBusinessId) {
        Lineage lineage = Lineage.findByProspectUuid(prospectUuid);
        if (lineage == null) {
            LOG.warnf("Lineage not found for prospect=%s; quote link skipped.", prospectUuid);
            return;
        }
        if (lineage.quoteUuid == null) {
            lineage.quoteUuid        = quoteUuid;
            lineage.quoteBusinessId  = quoteBusinessId;
            lineage.updatedAt        = java.time.LocalDateTime.now();
            LOG.infof("Lineage linked quote: PROSPECT=%s QT=%s", prospectUuid, quoteBusinessId);
        }
    }

    @Transactional
    public void linkApplication(UUID prospectUuid, UUID applicationUuid, String applicationBusinessId) {
        Lineage lineage = Lineage.findByProspectUuid(prospectUuid);
        if (lineage == null) {
            LOG.warnf("Lineage not found for prospect=%s; application link skipped.", prospectUuid);
            return;
        }
        if (lineage.applicationUuid == null) {
            lineage.applicationUuid        = applicationUuid;
            lineage.applicationBusinessId  = applicationBusinessId;
            lineage.updatedAt              = java.time.LocalDateTime.now();
            LOG.infof("Lineage linked application: PROSPECT=%s APP=%s", prospectUuid, applicationBusinessId);
        }
    }

    // -------------------------------------------------------
    // Private
    // -------------------------------------------------------

    private LineageResponse resolve(Lineage lineage, String idType, String idValue) {
        if (lineage == null) {
            throw new BusinessException(ErrorCodes.LINEAGE_NOT_FOUND,
                "No lineage record found for " + idType + "=" + idValue);
        }
        return LineageResponse.from(lineage);
    }
}
