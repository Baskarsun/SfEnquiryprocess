package com.sf.leasing.lead.service;

import com.sf.leasing.lead.api.dto.response.LineageResponse;
import com.sf.leasing.lead.domain.exception.BusinessException;
import com.sf.leasing.lead.domain.exception.ErrorCodes;
import com.sf.leasing.lead.domain.model.Lineage;
import com.sf.leasing.lead.infrastructure.persistence.LineageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
@Service
public class LineageService {

    private static final Logger LOG = LoggerFactory.getLogger(LineageService.class);

    private final LineageRepository lineageRepository;

    public LineageService(LineageRepository lineageRepository) {
        this.lineageRepository = lineageRepository;
    }

    // -------------------------------------------------------
    // Cross-reference queries (PP8.2)
    // -------------------------------------------------------

    public LineageResponse getByLeadLrn(String lrn) {
        Lineage lineage = lineageRepository.findByLeadLrn(lrn).orElse(null);
        return resolve(lineage, "LRN", lrn);
    }

    public LineageResponse getByProspectId(String prospectBusinessId) {
        Lineage lineage = lineageRepository.findByProspectBusinessId(prospectBusinessId).orElse(null);
        return resolve(lineage, "PROSPECT_ID", prospectBusinessId);
    }

    public LineageResponse getByApplicationBusinessId(String applicationBusinessId) {
        Lineage lineage = lineageRepository.findByApplicationBusinessId(applicationBusinessId).orElse(null);
        return resolve(lineage, "APPLICATION_ID", applicationBusinessId);
    }

    public LineageResponse getByCustomerId(String customerId) {
        Lineage lineage = lineageRepository.findByCustomerId(customerId).orElse(null);
        return resolve(lineage, "CUSTOMER_ID", customerId);
    }

    public LineageResponse getByOpportunityBusinessId(String opportunityBusinessId) {
        Lineage lineage = lineageRepository.findByOpportunityBusinessId(opportunityBusinessId).orElse(null);
        return resolve(lineage, "OPPORTUNITY_ID", opportunityBusinessId);
    }

    // -------------------------------------------------------
    // Stage updates — called by downstream services when an
    // entity is linked to an existing lineage record
    // -------------------------------------------------------

    @Transactional
    public void linkOpportunity(UUID prospectUuid, UUID opportunityUuid, String opportunityBusinessId) {
        Lineage lineage = lineageRepository.findByProspectUuid(prospectUuid).orElse(null);
        if (lineage == null) {
            LOG.warn("Lineage not found for prospect={}; opportunity link skipped.", prospectUuid);
            return;
        }
        if (lineage.opportunityUuid == null) {
            lineage.opportunityUuid        = opportunityUuid;
            lineage.opportunityBusinessId  = opportunityBusinessId;
            lineage.updatedAt              = java.time.LocalDateTime.now();
            LOG.info("Lineage linked opportunity: PROSPECT={} OPP={}", prospectUuid, opportunityBusinessId);
        }
    }

    @Transactional
    public void linkQuote(UUID prospectUuid, UUID quoteUuid, String quoteBusinessId) {
        Lineage lineage = lineageRepository.findByProspectUuid(prospectUuid).orElse(null);
        if (lineage == null) {
            LOG.warn("Lineage not found for prospect={}; quote link skipped.", prospectUuid);
            return;
        }
        if (lineage.quoteUuid == null) {
            lineage.quoteUuid        = quoteUuid;
            lineage.quoteBusinessId  = quoteBusinessId;
            lineage.updatedAt        = java.time.LocalDateTime.now();
            LOG.info("Lineage linked quote: PROSPECT={} QT={}", prospectUuid, quoteBusinessId);
        }
    }

    @Transactional
    public void linkApplication(UUID prospectUuid, UUID applicationUuid, String applicationBusinessId) {
        Lineage lineage = lineageRepository.findByProspectUuid(prospectUuid).orElse(null);
        if (lineage == null) {
            LOG.warn("Lineage not found for prospect={}; application link skipped.", prospectUuid);
            return;
        }
        if (lineage.applicationUuid == null) {
            lineage.applicationUuid        = applicationUuid;
            lineage.applicationBusinessId  = applicationBusinessId;
            lineage.updatedAt              = java.time.LocalDateTime.now();
            LOG.info("Lineage linked application: PROSPECT={} APP={}", prospectUuid, applicationBusinessId);
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
