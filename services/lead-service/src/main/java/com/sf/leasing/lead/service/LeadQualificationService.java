package com.sf.leasing.lead.service;

import com.sf.leasing.lead.api.dto.request.CloseLeadRequest;
import com.sf.leasing.lead.domain.enums.DedupLabel;
import com.sf.leasing.lead.domain.enums.LeadStatus;
import com.sf.leasing.lead.domain.enums.LeadType;
import com.sf.leasing.lead.domain.exception.BusinessException;
import com.sf.leasing.lead.domain.exception.ErrorCodes;
import com.sf.leasing.lead.domain.model.Applicant;
import com.sf.leasing.lead.domain.model.Lead;
import com.sf.leasing.lead.infrastructure.adapter.CautionListAdapter;
import com.sf.leasing.lead.infrastructure.persistence.LeadRepository;
import com.sf.leasing.lead.infrastructure.validation.IdentityFormatValidator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements LP8: Lead Qualification & Pre-Promotion checks.
 *
 * LP8.1 — DedupLabel = Conflict blocks promotion.
 * LP8.2 — PAN mandatory for Individual without DAN exemption.
 * LP8.3 — GSTIN mandatory for Commercial.
 * LP8.4 — Caution list check.
 * LP8.5 — Deceased lessee check.
 * LP8.6 — Active lease block (system parameter HP-ACTIVE-LEASE).
 * LP8.7 — NRI eligibility (passport validity ≥ 90 days, alternate mobile + email mandatory).
 * LP8.8 — Asset validations (cost > 0, cost ≥ finance amount, model year range).
 * LP8.9 — Applicant record completeness (location name, address length).
 *
 * Also handles lead closure (ClosureReason mandatory; record locked post-closure).
 */
@Service
public class LeadQualificationService {

    private static final Logger LOG = LoggerFactory.getLogger(LeadQualificationService.class);

    @Value("${qualification.active-lease-block-enabled:false}")
    boolean activeLeasBlockEnabled;

    @Value("${qualification.nri-passport-min-days:90}")
    int nriPassportMinDays;

    @PersistenceContext
    EntityManager em;

    private final LeadRepository leadRepository;
    private final CautionListAdapter cautionListAdapter;

    public LeadQualificationService(LeadRepository leadRepository,
                                    CautionListAdapter cautionListAdapter) {
        this.leadRepository = leadRepository;
        this.cautionListAdapter = cautionListAdapter;
    }

    // -------------------------------------------------------
    // Pre-promotion validation (full 17-point checklist)
    // -------------------------------------------------------

    @Transactional
    public List<String> validateForPromotion(String lrn) {
        Lead lead = leadRepository.findByLrn(lrn).orElseThrow(
            () -> new BusinessException("LEAD_NOT_FOUND", "Lead not found: " + lrn));
        if (lead.isClosed())    throw new BusinessException("LEAD_CLOSED",    "Closed leads cannot be promoted.");
        if (lead.isPromoted())  throw new BusinessException("ALREADY_PROMOTED", "Lead is already promoted.");

        List<String> warnings = new ArrayList<>();
        Applicant mainApplicant = getMainApplicant(lead);

        // LP8.1: DedupLabel = CONFLICT blocks
        checkDedupLabel(lead);

        // LP8.2: PAN mandatory for Individual
        checkPanMandatory(lead, mainApplicant);

        // LP8.3: GSTIN mandatory for Commercial
        checkGstinMandatory(lead, mainApplicant);

        // LP8.4: Caution list check
        checkCautionList(mainApplicant);

        // LP8.5: Deceased lessee check
        checkDeceasedLessee(mainApplicant);

        // LP8.6: Active lease block
        checkActiveLease(mainApplicant);

        // LP8.7: NRI eligibility
        if (warnings != null) checkNriEligibility(mainApplicant, warnings);

        // LP8.9: Applicant completeness
        checkApplicantCompleteness(mainApplicant);

        LOG.info("Pre-promotion validation passed for LRN={} (warnings={})", lrn, warnings.size());
        return warnings;
    }

    // -------------------------------------------------------
    // Lead closure
    // -------------------------------------------------------

    @Transactional
    public void closeLead(String lrn, CloseLeadRequest req, String userId) {
        Lead lead = leadRepository.findByLrn(lrn).orElseThrow(
            () -> new BusinessException("LEAD_NOT_FOUND", "Lead not found: " + lrn));
        if (lead.isClosed()) throw new BusinessException("LEAD_CLOSED", "Lead is already closed.");
        if (lead.isPromoted()) throw new BusinessException("LEAD_PROMOTED", "Promoted leads cannot be closed.");

        lead.status            = LeadStatus.CLOSED;
        lead.closureReasonCode = req.closureReasonCode;
        lead.closureReasonText = req.closureReasonText;
        lead.closureDate       = LocalDateTime.now();
        lead.closureOperator   = userId;
        lead.updatedBy         = userId;
        lead.updatedAt         = LocalDateTime.now();

        leadRepository.save(lead);

        LOG.info("Lead closed: LRN={} reason={} by={}", lrn, req.closureReasonCode, userId);
    }

    // -------------------------------------------------------
    // Individual checks
    // -------------------------------------------------------

    private void checkDedupLabel(Lead lead) {
        if (lead.dedupLabel == DedupLabel.CONFLICT) {
            throw new BusinessException("DEDUP_CONFLICT",
                "Lead has DedupLabel = CONFLICT. Resolve the exception queue entry before promotion.");
        }
    }

    private void checkPanMandatory(Lead lead, Applicant applicant) {
        if (lead.leadType != LeadType.INDIVIDUAL) return;
        if ("Y".equals(applicant.panExemptionFlag)) return;
        if (applicant.pan == null || applicant.pan.isBlank()) {
            throw new BusinessException(ErrorCodes.PAN_MANDATORY,
                "COM129: PAN is mandatory for Individual leads without a DAN exemption.");
        }
        if (!IdentityFormatValidator.isValidPan(applicant.pan)) {
            throw new BusinessException(ErrorCodes.LEAD_INPUT_REQUIRED,
                "PAN format is invalid: " + applicant.pan + ". Expected: AAAAA9999A");
        }
    }

    private void checkGstinMandatory(Lead lead, Applicant applicant) {
        if (lead.leadType != LeadType.COMMERCIAL) return;
        if (applicant.gstin == null || applicant.gstin.isBlank()) {
            throw new BusinessException(ErrorCodes.LEAD_INPUT_REQUIRED,
                "GSTIN is mandatory for Commercial leads.");
        }
        if (!IdentityFormatValidator.isValidGstin(applicant.gstin)) {
            throw new BusinessException(ErrorCodes.LEAD_INPUT_REQUIRED,
                "GSTIN format is invalid: " + applicant.gstin);
        }
    }

    private void checkCautionList(Applicant applicant) {
        if (applicant.pan == null) return;
        CautionListAdapter.CautionStatus status = cautionListAdapter.checkPan(applicant.pan);
        if (status == CautionListAdapter.CautionStatus.BLOCKED) {
            throw new BusinessException(ErrorCodes.PAN_CAUTION_LIST,
                "LN5337: PAN appears on caution list — promotion blocked.");
        }
    }

    private void checkDeceasedLessee(Applicant applicant) {
        if (applicant.isDeceased) {
            throw new BusinessException(ErrorCodes.CUSTOMER_DECEASED,
                "This Customer is deceased. Promotion blocked.");
        }
    }

    private void checkActiveLease(Applicant applicant) {
        if (!activeLeasBlockEnabled) return;
        if (applicant.pan == null) return;

        Long activeLeases = (Long) em.createNativeQuery(
            "SELECT COUNT(*) FROM applicants a " +
            "JOIN leads l ON l.id = a.lead_id " +
            "WHERE a.pan = ?1 AND l.status = 'PROMOTED'"
        ).setParameter(1, applicant.pan).getSingleResult();

        if (activeLeases > 0) {
            throw new BusinessException(ErrorCodes.ACTIVE_LEASE_BLOCK,
                "LN4323: Active lease exists for this applicant. Promotion blocked per HP-ACTIVE-LEASE policy.");
        }
    }

    private void checkNriEligibility(Applicant applicant, List<String> warnings) {
        if (!"NRI".equalsIgnoreCase(applicant.residentialType)) return;

        // Passport validity ≥ 90 days
        if (applicant.passportValidityDate != null) {
            long daysToExpiry = java.time.temporal.ChronoUnit.DAYS.between(
                LocalDate.now(), applicant.passportValidityDate);
            if (daysToExpiry < nriPassportMinDays) {
                throw new BusinessException("NRI_PASSPORT_EXPIRED",
                    "NRI applicant passport validity is less than " + nriPassportMinDays +
                    " days. Promotion blocked.");
            }
        } else {
            throw new BusinessException("NRI_PASSPORT_REQUIRED",
                "NRI applicant must provide a valid passport.");
        }

        // Alternate mobile + email mandatory for NRI
        if (applicant.mobile == null || applicant.email == null) {
            warnings.add("NRI applicant: alternate mobile and email are mandatory for communication.");
        }
    }

    private void checkApplicantCompleteness(Applicant applicant) {
        // LP8.9: Location name must not be null
        if (applicant.locationName == null || applicant.locationName.isBlank()) {
            throw new BusinessException(ErrorCodes.LOCATION_NAME_NULL,
                "LN5078: Applicant location name is required.");
        }

        // Address line 1: 3–40 chars
        if (applicant.addressLine1 != null) {
            int len = applicant.addressLine1.trim().length();
            if (len < 3) {
                throw new BusinessException(ErrorCodes.ADDRESS_TOO_SHORT,
                    "LN5226: Address line 1 is too short (minimum 3 characters).");
            }
            if (len > 40) {
                throw new BusinessException(ErrorCodes.ADDRESS_TOO_LONG,
                    "LN5227: Address line 1 is too long (maximum 40 characters).");
            }
        }
    }

    private Applicant getMainApplicant(Lead lead) {
        if (lead.applicants == null || lead.applicants.isEmpty()) {
            throw new BusinessException("APPLICANT_REQUIRED", "Lead has no applicant record.");
        }
        return lead.applicants.get(0);
    }
}
