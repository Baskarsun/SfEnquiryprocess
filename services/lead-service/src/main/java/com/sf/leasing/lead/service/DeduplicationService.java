package com.sf.leasing.lead.service;

import com.sf.leasing.lead.domain.enums.DedupLabel;
import com.sf.leasing.lead.domain.exception.BusinessException;
import com.sf.leasing.lead.domain.exception.ErrorCodes;
import com.sf.leasing.lead.domain.model.*;
import com.sf.leasing.lead.infrastructure.adapter.CautionListAdapter;
import com.sf.leasing.lead.infrastructure.adapter.PanDedupAdapter;
import com.sf.leasing.lead.infrastructure.adapter.UcicMappingAdapter;
import com.sf.leasing.lead.infrastructure.persistence.LeadDedupResultRepository;
import com.sf.leasing.lead.infrastructure.persistence.LeadRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implements LP4: Identity Capture & Deduplication.
 */
@Service
public class DeduplicationService {

    private static final Logger LOG = LoggerFactory.getLogger(DeduplicationService.class);

    private final ExceptionQueueService exceptionQueueService;
    private final CautionListAdapter cautionListAdapter;
    private final UcicMappingAdapter ucicMappingAdapter;
    private final PanDedupAdapter panDedupAdapter;
    private final LeadDedupResultRepository dedupResultRepository;

    @PersistenceContext
    private EntityManager em;

    public DeduplicationService(ExceptionQueueService exceptionQueueService,
                                 CautionListAdapter cautionListAdapter,
                                 UcicMappingAdapter ucicMappingAdapter,
                                 PanDedupAdapter panDedupAdapter,
                                 LeadDedupResultRepository dedupResultRepository) {
        this.exceptionQueueService = exceptionQueueService;
        this.cautionListAdapter = cautionListAdapter;
        this.ucicMappingAdapter = ucicMappingAdapter;
        this.panDedupAdapter = panDedupAdapter;
        this.dedupResultRepository = dedupResultRepository;
    }

    @Transactional
    public DedupLabel runDedupChecks(Lead lead, boolean includeExternalChecks) {
        List<Applicant> applicants = lead.applicants;
        if (applicants == null || applicants.isEmpty()) {
            lead.dedupLabel = DedupLabel.UNKNOWN;
            return DedupLabel.UNKNOWN;
        }

        Applicant mainApplicant = applicants.get(0);

        checkGenderOccupation(mainApplicant);
        checkPanMandatory(lead, mainApplicant);

        DedupLabel resolvedLabel = DedupLabel.NEW;
        String resolvedUcic = null;
        String resolvedCustomerCodes = null;

        for (Applicant applicant : applicants) {
            if (includeExternalChecks && applicant.pan != null) {
                CautionListAdapter.CautionStatus cautionStatus = cautionListAdapter.checkPan(applicant.pan);
                recordDedupResult(lead, applicant.id, "CAUTION_LIST", cautionStatus.name(), null, "EXTERNAL");

                if (cautionStatus == CautionListAdapter.CautionStatus.BLOCKED) {
                    throw new BusinessException(ErrorCodes.PAN_CAUTION_LIST,
                        "LN5337: PAN appears on caution list — lead creation blocked.");
                }
            }

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

                    if (ucicResult.riskCategory != null && isRestrictedRiskCategory(ucicResult.riskCategory)) {
                        throw new BusinessException("RISK_CATEGORY_RESTRICTED",
                            "Existing customer has a restricted risk category. Lead creation blocked.");
                    }
                }
            }

            if (includeExternalChecks && applicant.pan != null) {
                PanDedupAdapter.PanDedupResult panDedup = panDedupAdapter.check(applicant.pan);
                recordDedupResult(lead, applicant.id, "EXTERNAL_PAN", panDedup.code.name(),
                    panDedup.matchedEntityId, "EXTERNAL");

                switch (panDedup.code) {
                    case COM110:
                        throw new BusinessException(ErrorCodes.CONFIRMED_DUPLICATE,
                            "COM110: Confirmed duplicate — lead creation blocked.");
                    case COM111:
                        resolvedLabel = promoteLabel(resolvedLabel, DedupLabel.POSSIBLE_EXISTING);
                        LOG.warn("COM111: Potential duplicate for LRN={} PAN={}", lead.lrn, applicant.pan);
                        break;
                    case COM66:
                        LOG.info("COM66: Soft dedup warning for LRN={} PAN={}", lead.lrn, applicant.pan);
                        break;
                    default:
                        break;
                }
            }

            DedupLabel internalLabel = runInternalDedup(lead, applicant);
            resolvedLabel = promoteLabel(resolvedLabel, internalLabel);
        }

        boolean effectivelyFiled = checkEffectivelyFiled(mainApplicant);
        if (effectivelyFiled) {
            LOG.info("Lead {} has an effectively filed prospect on same KYC identifiers", lead.lrn);
        }

        lead.dedupLabel = resolvedLabel;
        if (resolvedUcic != null) {
            lead.ucic = resolvedUcic;
            lead.existingCustomerCodes = resolvedCustomerCodes;
        }

        if (resolvedLabel == DedupLabel.CONFLICT) {
            exceptionQueueService.routeToExceptionQueue(
                lead.channel.name(), lead.lrn, "DEDUP_CONFLICT",
                "DedupLabel resolved to CONFLICT — manual review required.",
                "pan, mobile, email, gstin", "{\"lrn\":\"" + lead.lrn + "\"}"
            );
            LOG.warn("Lead {} routed to exception queue: DedupLabel=CONFLICT", lead.lrn);
        }

        LOG.info("Dedup complete: LRN={} DedupLabel={} UCIC={}", lead.lrn, resolvedLabel, resolvedUcic);
        return resolvedLabel;
    }

    private DedupLabel runInternalDedup(Lead lead, Applicant applicant) {
        if (lead.id == null) return DedupLabel.NEW;

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

        if (applicant.mobile != null) {
            List<Object[]> mobileMatches = em.createNativeQuery(
                "SELECT a.lead_id FROM applicants a JOIN leads l ON l.id = a.lead_id " +
                "WHERE a.mobile = ?1 AND l.id != ?2 AND l.status NOT IN ('CLOSED') LIMIT 3"
            ).setParameter(1, applicant.mobile).setParameter(2, lead.id).getResultList();

            if (mobileMatches.size() > 1) {
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

    private void checkGenderOccupation(Applicant applicant) {
        if ("M".equals(applicant.gender) && "HOUSE WIFE".equalsIgnoreCase(applicant.occupation)) {
            throw new BusinessException(ErrorCodes.GENDER_OCCUPATION_MISMATCH,
                "COM122: Male applicant cannot have occupation HOUSE WIFE.");
        }
    }

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

    private boolean isRestrictedRiskCategory(String riskCategory) {
        return "RESTRICTED".equalsIgnoreCase(riskCategory)
            || "BLACKLISTED".equalsIgnoreCase(riskCategory)
            || "WATCHLIST".equalsIgnoreCase(riskCategory);
    }

    /**
     * Rule LP4.9: Check for existing active prospect/enquiry on same KYC identifiers.
     * Uses parameterized queries to prevent SQL injection.
     */
    private boolean checkEffectivelyFiled(Applicant applicant) {
        if (applicant.pan == null && applicant.mobile == null) return false;

        if (applicant.pan != null && applicant.mobile != null) {
            Long count = (Long) em.createNativeQuery(
                "SELECT COUNT(*) FROM leads l JOIN applicants a ON a.lead_id = l.id " +
                "WHERE l.status = 'PROMOTED' AND (a.pan = ?1 OR a.mobile = ?2)"
            ).setParameter(1, applicant.pan).setParameter(2, applicant.mobile).getSingleResult();
            return count > 0;
        } else if (applicant.pan != null) {
            Long count = (Long) em.createNativeQuery(
                "SELECT COUNT(*) FROM leads l JOIN applicants a ON a.lead_id = l.id " +
                "WHERE l.status = 'PROMOTED' AND a.pan = ?1"
            ).setParameter(1, applicant.pan).getSingleResult();
            return count > 0;
        } else {
            Long count = (Long) em.createNativeQuery(
                "SELECT COUNT(*) FROM leads l JOIN applicants a ON a.lead_id = l.id " +
                "WHERE l.status = 'PROMOTED' AND a.mobile = ?1"
            ).setParameter(1, applicant.mobile).getSingleResult();
            return count > 0;
        }
    }

    private void recordDedupResult(Lead lead, java.util.UUID applicantId,
                                    String checkType, String resultCode,
                                    String matchedEntityId, String matchedEntityType) {
        if (lead.id == null) return;
        LeadDedupResult result = new LeadDedupResult();
        result.lead = lead;
        result.applicantId = applicantId;
        result.checkType = checkType;
        result.resultCode = resultCode;
        result.matchedEntityId = matchedEntityId;
        result.matchedEntityType = matchedEntityType;
        dedupResultRepository.save(result);
    }

    private DedupLabel promoteLabel(DedupLabel current, DedupLabel candidate) {
        return labelRank(candidate) > labelRank(current) ? candidate : current;
    }

    private int labelRank(DedupLabel label) {
        return switch (label) {
            case NEW               -> 0;
            case POSSIBLE_EXISTING -> 1;
            case UNKNOWN           -> 2;
            case CONFLICT          -> 3;
        };
    }
}
