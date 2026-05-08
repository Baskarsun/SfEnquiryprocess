package com.sf.leasing.lead.service;

import com.sf.leasing.lead.domain.enums.DedupLabel;
import com.sf.leasing.lead.domain.exception.BusinessException;
import com.sf.leasing.lead.domain.exception.ErrorCodes;
import com.sf.leasing.lead.domain.model.*;
import com.sf.leasing.lead.infrastructure.adapter.CautionListAdapter;
import com.sf.leasing.lead.infrastructure.adapter.PanDedupAdapter;
import com.sf.leasing.lead.infrastructure.adapter.UcicMappingAdapter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Implements LP4: Identity Capture & Deduplication.
 *
 * Runs in order:
 *  1. Gender/occupation consistency check (COM122)
 *  2. PAN mandatory check (COM129)
 *  3. Caution list check (LN5337)
 *  4. UCIC mapping
 *  5. External PAN dedup API (COM110 / COM111 / COM66)
 *  6. Internal database dedup (Mobile / Email / PAN / GSTIN)
 *  7. DedupLabel resolution + risk category check
 *  8. Effectively Filed check
 */
@ApplicationScoped
public class DeduplicationService {

    private static final Logger LOG = Logger.getLogger(DeduplicationService.class);

    @Inject EntityManager em;
    @Inject CautionListAdapter cautionListAdapter;
    @Inject UcicMappingAdapter ucicMappingAdapter;
    @Inject PanDedupAdapter panDedupAdapter;
    @Inject ExceptionQueueService exceptionQueueService;

    /**
     * Run all dedup checks for a newly created or re-checked lead.
     * Updates lead.dedupLabel, lead.ucic, and persists dedup result records.
     * Routes to exception queue when DedupLabel = CONFLICT.
     *
     * Called after lead + applicant records are persisted.
     */
    @Transactional(Transactional.TxType.REQUIRED)
    public DedupLabel runDedupChecks(Lead lead, boolean includeExternalChecks) {
        List<Applicant> applicants = lead.applicants;
        if (applicants == null || applicants.isEmpty()) {
            lead.dedupLabel = DedupLabel.UNKNOWN;
            return DedupLabel.UNKNOWN;
        }

        Applicant mainApplicant = applicants.get(0);

        // Rule LP4.10: Gender + occupation consistency
        checkGenderOccupation(mainApplicant);

        // Rule LP4.11: PAN mandatory for individual without DAN exemption
        checkPanMandatory(lead, mainApplicant);

        DedupLabel resolvedLabel = DedupLabel.NEW;
        String resolvedUcic = null;
        String resolvedCustomerCodes = null;

        for (Applicant applicant : applicants) {
            // Rule LP4.3: Caution list check on PAN
            if (includeExternalChecks && applicant.pan != null) {
                CautionListAdapter.CautionStatus cautionStatus = cautionListAdapter.checkPan(applicant.pan);
                recordDedupResult(lead, applicant.id, "CAUTION_LIST", cautionStatus.name(), null, "EXTERNAL");

                if (cautionStatus == CautionListAdapter.CautionStatus.BLOCKED) {
                    throw new BusinessException(ErrorCodes.PAN_CAUTION_LIST,
                        "LN5337: PAN appears on caution list — lead creation blocked.");
                }
            }

            // Rule LP4.4: UCIC mapping
            if (includeExternalChecks) {
                UcicMappingAdapter.UcicResult ucicResult = null;
                if (applicant.pan != null) {
                    ucicResult = ucicMappingAdapter.lookupByPan(applicant.pan);
                } else if (applicant.gstin != null) {
                    ucicResult = ucicMappingAdapter.lookupByGstin(applicant.gstin);
                }
                if (ucicResult != null && ucicResult.found) {
                    resolvedUcic = ucicResult.ucic;
                    resolvedCustomerCodes = ucicResult.existingCustomerCodes;
                    resolvedLabel = promoteLabel(resolvedLabel, DedupLabel.POSSIBLE_EXISTING);

                    // Rule LP4.8: risk category check
                    if (ucicResult.riskCategory != null && isRestrictedRiskCategory(ucicResult.riskCategory)) {
                        throw new BusinessException("RISK_CATEGORY_RESTRICTED",
                            "Existing customer has a restricted risk category. Lead creation blocked.");
                    }
                }
            }

            // Rule LP4.5: External PAN dedup API
            if (includeExternalChecks && applicant.pan != null) {
                PanDedupAdapter.PanDedupResult panDedup = panDedupAdapter.check(applicant.pan);
                recordDedupResult(lead, applicant.id, "EXTERNAL_PAN", panDedup.code.name(),
                    panDedup.matchedEntityId, "EXTERNAL");

                switch (panDedup.code) {
                    case COM110:
                        // Confirmed duplicate — hard reject
                        throw new BusinessException(ErrorCodes.CONFIRMED_DUPLICATE,
                            "COM110: Confirmed duplicate — lead creation blocked.");
                    case COM111:
                        // Potential duplicate — warning, proceed
                        resolvedLabel = promoteLabel(resolvedLabel, DedupLabel.POSSIBLE_EXISTING);
                        LOG.warnf("COM111: Potential duplicate for LRN=%s PAN=%s", lead.lrn, applicant.pan);
                        break;
                    case COM66:
                        // Soft warning — proceed, flagged for review
                        LOG.infof("COM66: Soft dedup warning for LRN=%s PAN=%s", lead.lrn, applicant.pan);
                        break;
                    default:
                        break;
                }
            }

            // Rule LP4.6: Internal database dedup
            DedupLabel internalLabel = runInternalDedup(lead, applicant);
            resolvedLabel = promoteLabel(resolvedLabel, internalLabel);
        }

        // Rule LP4.9: Effectively Filed check (existing open prospect/enquiry on same identifiers)
        boolean effectivelyFiled = checkEffectivelyFiled(mainApplicant);
        if (effectivelyFiled) {
            LOG.infof("Lead %s has an effectively filed prospect on same KYC identifiers", lead.lrn);
        }

        // Persist final dedup outcome on lead
        lead.dedupLabel = resolvedLabel;
        if (resolvedUcic != null) {
            lead.ucic = resolvedUcic;
            lead.existingCustomerCodes = resolvedCustomerCodes;
        }

        // Rule LP4.7: CONFLICT routes to exception queue
        if (resolvedLabel == DedupLabel.CONFLICT) {
            exceptionQueueService.routeToExceptionQueue(
                lead.channel.name(), lead.lrn, "DEDUP_CONFLICT",
                "DedupLabel resolved to CONFLICT — manual review required.",
                "pan, mobile, email, gstin", "{\"lrn\":\"" + lead.lrn + "\"}"
            );
            LOG.warnf("Lead %s routed to exception queue: DedupLabel=CONFLICT", lead.lrn);
        }

        LOG.infof("Dedup complete: LRN=%s DedupLabel=%s UCIC=%s", lead.lrn, resolvedLabel, resolvedUcic);
        return resolvedLabel;
    }

    // -------------------------------------------------------
    // Internal dedup — PostgreSQL query against existing records
    // -------------------------------------------------------

    private DedupLabel runInternalDedup(Lead lead, Applicant applicant) {
        // Skip internal DB checks if the lead hasn't been persisted yet (e.g., in unit tests)
        if (lead.id == null) return DedupLabel.NEW;

        // Check by PAN
        if (applicant.pan != null) {
            List<Object[]> matches = em.createNativeQuery(
                "SELECT a.lead_id FROM applicants a JOIN leads l ON l.id = a.lead_id " +
                "WHERE a.pan = ?1 AND l.id != ?2 AND l.status NOT IN ('CLOSED')"
            ).setParameter(1, applicant.pan).setParameter(2, lead.id).getResultList();

            if (!matches.isEmpty()) {
                recordDedupResult(lead, applicant.id, "INTERNAL", "COM111", matches.get(0)[0].toString(), "APPLICANT");
                return DedupLabel.POSSIBLE_EXISTING;
            }
        }

        // Check by GSTIN
        if (applicant.gstin != null) {
            List<Object[]> matches = em.createNativeQuery(
                "SELECT a.lead_id FROM applicants a JOIN leads l ON l.id = a.lead_id " +
                "WHERE a.gstin = ?1 AND l.id != ?2 AND l.status NOT IN ('CLOSED')"
            ).setParameter(1, applicant.gstin).setParameter(2, lead.id).getResultList();

            if (!matches.isEmpty()) {
                recordDedupResult(lead, applicant.id, "INTERNAL", "COM111", matches.get(0)[0].toString(), "APPLICANT");
                return DedupLabel.POSSIBLE_EXISTING;
            }
        }

        // Check by mobile (exact match — known active leads)
        if (applicant.mobile != null) {
            List<Object[]> mobileMatches = em.createNativeQuery(
                "SELECT a.lead_id FROM applicants a JOIN leads l ON l.id = a.lead_id " +
                "WHERE a.mobile = ?1 AND l.id != ?2 AND l.status NOT IN ('CLOSED') LIMIT 3"
            ).setParameter(1, applicant.mobile).setParameter(2, lead.id).getResultList();

            if (mobileMatches.size() > 1) {
                // Multiple mobile matches — potential conflict
                recordDedupResult(lead, applicant.id, "INTERNAL", "CONFLICT", null, "APPLICANT");
                return DedupLabel.CONFLICT;
            } else if (!mobileMatches.isEmpty()) {
                recordDedupResult(lead, applicant.id, "INTERNAL", "COM111", mobileMatches.get(0)[0].toString(), "APPLICANT");
                return DedupLabel.POSSIBLE_EXISTING;
            }
        }

        recordDedupResult(lead, applicant.id, "INTERNAL", "ALLOWED", null, null);
        return DedupLabel.NEW;
    }

    // -------------------------------------------------------
    // Business rule checks
    // -------------------------------------------------------

    /**
     * Rule LP4.10 (COM122): Male gender + HOUSE WIFE occupation is inconsistent.
     */
    private void checkGenderOccupation(Applicant applicant) {
        if ("M".equals(applicant.gender) && "HOUSE WIFE".equalsIgnoreCase(applicant.occupation)) {
            throw new BusinessException(ErrorCodes.GENDER_OCCUPATION_MISMATCH,
                "COM122: Male applicant cannot have occupation HOUSE WIFE.");
        }
    }

    /**
     * Rule LP4.11 (COM129): Individual applicant without DAN exemption must provide PAN.
     */
    private void checkPanMandatory(Lead lead, Applicant applicant) {
        boolean isIndividual = "INDIVIDUAL".equalsIgnoreCase(applicant.constitutionType)
            || (applicant.constitutionType == null && lead.leadType != null
                && lead.leadType.name().equals("INDIVIDUAL"));
        boolean hasDanExemption = "Y".equals(applicant.panExemptionFlag);

        if (isIndividual && !hasDanExemption && (applicant.pan == null || applicant.pan.isBlank())) {
            throw new BusinessException(ErrorCodes.PAN_MANDATORY,
                "COM129: PAN is mandatory for Individual applicants without a DAN exemption.");
        }
    }

    /**
     * Rule LP4.8: Customers with restricted risk categories must be blocked.
     */
    private boolean isRestrictedRiskCategory(String riskCategory) {
        return "RESTRICTED".equalsIgnoreCase(riskCategory)
            || "BLACKLISTED".equalsIgnoreCase(riskCategory)
            || "WATCHLIST".equalsIgnoreCase(riskCategory);
    }

    /**
     * Rule LP4.9: Check for an existing active prospect/enquiry on the same KYC identifiers.
     */
    private boolean checkEffectivelyFiled(Applicant applicant) {
        if (applicant.pan == null && applicant.mobile == null) return false;

        StringBuilder query = new StringBuilder(
            "SELECT COUNT(*) FROM leads l JOIN applicants a ON a.lead_id = l.id " +
            "WHERE l.status = 'PROMOTED' AND ("
        );
        boolean hasCondition = false;
        if (applicant.pan != null) {
            query.append("a.pan = '").append(applicant.pan).append("'");
            hasCondition = true;
        }
        if (applicant.mobile != null) {
            if (hasCondition) query.append(" OR ");
            query.append("a.mobile = '").append(applicant.mobile).append("'");
        }
        query.append(")");

        Long count = (Long) em.createNativeQuery(query.toString()).getSingleResult();
        return count > 0;
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------

    private void recordDedupResult(Lead lead, java.util.UUID applicantId,
                                    String checkType, String resultCode,
                                    String matchedEntityId, String matchedEntityType) {
        if (lead.id == null) return;  // Skip persistence for unpersisted leads (e.g., unit tests)
        LeadDedupResult result = new LeadDedupResult();
        result.lead = lead;
        result.applicantId = applicantId;
        result.checkType = checkType;
        result.resultCode = resultCode;
        result.matchedEntityId = matchedEntityId;
        result.matchedEntityType = matchedEntityType;
        result.persist();
    }

    /**
     * Returns the higher-severity label between current and candidate.
     * Severity: NEW < POSSIBLE_EXISTING < UNKNOWN < CONFLICT.
     */
    private DedupLabel promoteLabel(DedupLabel current, DedupLabel candidate) {
        int currentRank  = labelRank(current);
        int candidateRank = labelRank(candidate);
        return candidateRank > currentRank ? candidate : current;
    }

    private int labelRank(DedupLabel label) {
        return switch (label) {
            case NEW              -> 0;
            case POSSIBLE_EXISTING -> 1;
            case UNKNOWN          -> 2;
            case CONFLICT         -> 3;
        };
    }
}
