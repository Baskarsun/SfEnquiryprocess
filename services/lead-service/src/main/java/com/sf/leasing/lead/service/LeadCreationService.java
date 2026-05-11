package com.sf.leasing.lead.service;

import com.sf.leasing.lead.api.dto.request.ApplicantRequest;
import com.sf.leasing.lead.api.dto.request.CreateLeadRequest;
import com.sf.leasing.lead.api.dto.response.CreateLeadResponse;
import com.sf.leasing.lead.domain.enums.*;
import com.sf.leasing.lead.domain.exception.BusinessException;
import com.sf.leasing.lead.domain.exception.ErrorCodes;
import com.sf.leasing.lead.domain.model.*;
import com.sf.leasing.lead.infrastructure.locking.RedisSequenceGenerator;
import com.sf.leasing.lead.infrastructure.messaging.LeadEventProducer;
import com.sf.leasing.lead.infrastructure.persistence.ApplicantRepository;
import com.sf.leasing.lead.infrastructure.persistence.LeadRepository;
import com.sf.leasing.lead.infrastructure.validation.AadhaarVerhoeffValidator;
import com.sf.leasing.lead.infrastructure.validation.IdentityFormatValidator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements LP1 (auth validation) and LP2 (base lead creation) business rules.
 */
@Service
public class LeadCreationService {

    private static final Logger LOG = LoggerFactory.getLogger(LeadCreationService.class);
    private static final String LOB_PREFIX = "LS";
    private static final String CPU_TEAM   = "CPU";
    private static final String CPU_USER   = "EMP001";

    private final RedisSequenceGenerator sequenceGenerator;
    private final ExceptionQueueService exceptionQueueService;
    private final DeduplicationService deduplicationService;
    private final GeographicValidationService geographicValidationService;
    private final LeadEventProducer eventProducer;
    private final LeadRepository leadRepository;
    private final ApplicantRepository applicantRepository;

    @PersistenceContext
    private EntityManager em;

    public LeadCreationService(RedisSequenceGenerator sequenceGenerator,
                                ExceptionQueueService exceptionQueueService,
                                DeduplicationService deduplicationService,
                                GeographicValidationService geographicValidationService,
                                LeadEventProducer eventProducer,
                                LeadRepository leadRepository,
                                ApplicantRepository applicantRepository) {
        this.sequenceGenerator = sequenceGenerator;
        this.exceptionQueueService = exceptionQueueService;
        this.deduplicationService = deduplicationService;
        this.geographicValidationService = geographicValidationService;
        this.eventProducer = eventProducer;
        this.leadRepository = leadRepository;
        this.applicantRepository = applicantRepository;
    }

    // -------------------------------------------------------
    // LP1: Authentication validation
    // -------------------------------------------------------

    public void validateUserAndDevice(String userId, String primaryDeviceId, String secondaryDeviceId) {
        if (userId == null || userId.isBlank()) {
            throw new BusinessException(ErrorCodes.INVALID_USER, "User authentication is required.");
        }

        Object[] employee = findActiveEmployee(userId);
        if (employee == null) {
            throw new BusinessException(ErrorCodes.INVALID_USER,
                "GL461: Invalid User — no active employee record found for user: " + userId);
        }

        String effectiveDeviceId = primaryDeviceId;
        if (effectiveDeviceId == null || effectiveDeviceId.isBlank()) {
            effectiveDeviceId = secondaryDeviceId;
        }
        if (effectiveDeviceId != null && !effectiveDeviceId.isBlank()) {
            validateDevice(userId, effectiveDeviceId);
        }
    }

    private Object[] findActiveEmployee(String userId) {
        List<Object[]> results = em.createNativeQuery(
            "SELECT employee_id, status FROM employees WHERE employee_id = ?1 AND status = 'ACTIVE'"
        ).setParameter(1, userId).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    private void validateDevice(String userId, String deviceId) {
        Long count = (Long) em.createNativeQuery(
            "SELECT COUNT(*) FROM device_registry WHERE employee_id = ?1 AND (imei = ?2 OR device_uuid = ?2) AND is_authorised = TRUE"
        ).setParameter(1, userId).setParameter(2, deviceId).getSingleResult();
        if (count == 0) {
            LOG.warn("Device {} not registered for user {} — proceeding (legacy mode)", deviceId, userId);
        }
    }

    // -------------------------------------------------------
    // LP2: Base Lead Record Creation
    // -------------------------------------------------------

    @Transactional
    public CreateLeadResponse createLead(CreateLeadRequest req, String userId, Channel channel,
                                         Double latitude, Double longitude) {
        if (req == null) {
            throw new BusinessException(ErrorCodes.LEAD_INPUT_REQUIRED, "Lead Input Values Is Required.");
        }

        if (req.leadType == null) {
            throw new BusinessException(ErrorCodes.LEAD_INPUT_REQUIRED, "Lead Type is required.");
        }

        if (req.leadType == LeadType.COMMERCIAL) {
            if (isBlank(req.companyKnownAs) || isBlank(req.contactPerson)) {
                throw new BusinessException(ErrorCodes.LEAD_INPUT_REQUIRED,
                    "Company name and Contact Person are mandatory for Commercial leads.");
            }
        } else {
            if (req.applicants == null || req.applicants.isEmpty() || isBlank(req.applicants.get(0).applicantName)) {
                throw new BusinessException(ErrorCodes.LEAD_INPUT_REQUIRED,
                    "Applicant Name is mandatory for Individual leads.");
            }
        }

        if (req.sourceCategory == null) {
            throw new BusinessException(ErrorCodes.LEAD_INPUT_REQUIRED, "Source Category is mandatory.");
        }
        if (isBlank(req.sourceName)) {
            throw new BusinessException(ErrorCodes.LEAD_INPUT_REQUIRED, "Source Name is mandatory.");
        }

        ApplicantRequest mainApplicant = req.applicants != null && !req.applicants.isEmpty()
            ? req.applicants.get(0) : null;

        String mobile  = mainApplicant != null ? mainApplicant.mobile : null;
        String email   = mainApplicant != null ? mainApplicant.email : null;
        String pan     = mainApplicant != null ? mainApplicant.pan : null;
        String gstin   = mainApplicant != null ? mainApplicant.gstin : null;
        String address = mainApplicant != null ? mainApplicant.addressLine1 : null;

        if (!IdentityFormatValidator.hasMinimumIdentifier(mobile, email, pan, gstin, address)) {
            exceptionQueueService.routeToExceptionQueue(
                channel.name(), null, ErrorCodes.MIN_IDENTIFIER_MISSING,
                "Name + at least one identifier (Mobile/Email/PAN/GSTIN/Address) is required.",
                "mobile, email, pan, gstin, address", toJson(req)
            );
            return CreateLeadResponse.routedToExceptionQueue();
        }

        if (mobile != null && !mobile.isBlank() && !IdentityFormatValidator.isValidMobile(mobile)) {
            throw new BusinessException(ErrorCodes.LEAD_INPUT_REQUIRED, "Invalid mobile number format.");
        }

        if (pan != null && !pan.isBlank() && !IdentityFormatValidator.isValidPan(pan)) {
            throw new BusinessException(ErrorCodes.LEAD_INPUT_REQUIRED, "Invalid PAN format. Expected: AAAAA9999A");
        }

        if (mainApplicant != null && !isBlank(mainApplicant.dan)) {
            validateDan(mainApplicant.dan);
        }

        List<Applicant> applicants = buildApplicants(req);

        for (ApplicantRequest ar : safe(req.applicants)) {
            if (!isBlank(ar.aadhaar)) {
                if (!AadhaarVerhoeffValidator.isValid(ar.aadhaar)) {
                    throw new BusinessException(ErrorCodes.INVALID_AADHAAR,
                        "Invalid Aadhaar Number: " + ar.aadhaar);
                }
            }
        }

        String tempCustNo = sequenceGenerator.generateTempCustomerNumber();
        String lrn        = sequenceGenerator.generateLrn(LOB_PREFIX);

        Lead lead = new Lead();
        lead.lrn                       = lrn;
        lead.tempCustomerNumber        = tempCustNo;
        lead.leadType                  = req.leadType;
        lead.sourceCategory            = req.sourceCategory;
        lead.sourceName                = req.sourceName;
        lead.companyKnownAs            = req.companyKnownAs;
        lead.contactPerson             = req.contactPerson;
        lead.status                    = LeadStatus.NEW;
        lead.temperature               = LeadTemperature.COLD;
        lead.temperatureSuggestedBy    = "SYSTEM";
        lead.dedupLabel                = DedupLabel.UNKNOWN;
        lead.channel                   = channel;
        lead.assignedTeam              = CPU_TEAM;
        lead.assignedUserId            = CPU_USER;
        lead.assignmentHierarchyLevel  = HierarchyLevel.CENTRAL;
        lead.createdBy                 = userId;
        lead.createdAt                 = LocalDateTime.now();

        if (channel == Channel.MOBILE && latitude != null && longitude != null) {
            lead.latitude          = latitude;
            lead.longitude         = longitude;
            lead.geotagCapturedAt  = LocalDateTime.now();
        }

        leadRepository.save(lead);

        for (Applicant applicant : applicants) {
            applicant.lead = lead;
            applicantRepository.save(applicant);
        }
        lead.applicants = applicants;

        boolean geoWarning = false;
        if (mainApplicant != null && !isBlank(mainApplicant.pincode)) {
            boolean geoValid = geographicValidationService.validatePincodeBranch(
                mainApplicant.pincode, CPU_TEAM, channel);
            if (!geoValid) {
                geoWarning = true;
            }
        }

        deduplicationService.runDedupChecks(lead, channel != Channel.BULK);

        eventProducer.publishLeadCreated(lrn, tempCustNo, userId, channel.name());

        LOG.info("Lead created: LRN={} TempCustNo={} by user={} channel={}", lrn, tempCustNo, userId, channel);
        CreateLeadResponse resp = CreateLeadResponse.success(lrn, tempCustNo);
        if (geoWarning) resp.warning = "LN3955: Applicant pincode is outside the branch service area (save allowed on mobile).";
        return resp;
    }

    // -------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------

    private List<Applicant> buildApplicants(CreateLeadRequest req) {
        List<Applicant> result = new ArrayList<>();
        List<ApplicantRequest> applicantReqs = safe(req.applicants);
        for (int i = 0; i < applicantReqs.size(); i++) {
            ApplicantRequest ar = applicantReqs.get(i);
            Applicant a = new Applicant();
            a.applicantLabel    = i == 0 ? "MAIN APPLICANT" : "ADDL APPLICANT - " + i;
            a.applicantName     = ar.applicantName;
            a.gender            = normalizeGender(ar.gender, ar.constitutionType);
            a.dateOfBirth       = ar.dateOfBirth;
            a.constitutionType  = ar.constitutionType;
            a.residentialType   = ar.residentialType != null ? ar.residentialType : "RESIDENT";
            a.mobile            = ar.mobile;
            a.email             = ar.email;
            a.pan               = ar.pan != null ? ar.pan.toUpperCase().trim() : null;
            a.gstin             = ar.gstin != null ? ar.gstin.toUpperCase().trim() : null;
            a.passportNumber    = ar.passportNumber;
            a.passportValidityDate = ar.passportValidityDate;
            a.voterId           = ar.voterId;
            a.drivingLicence    = ar.drivingLicence;
            a.dan               = ar.dan;
            a.panExemptionFlag  = isBlank(ar.dan) ? "N" : "Y";
            a.occupation        = ar.occupation;
            a.addressLine1      = ar.addressLine1;
            a.addressLine2      = ar.addressLine2;
            a.pincode           = ar.pincode;
            a.locationName      = ar.locationName;
            a.city              = ar.city;
            a.state             = ar.state;
            a.createdAt         = LocalDateTime.now();
            result.add(a);
        }
        return result;
    }

    private String normalizeGender(String raw, String constitutionType) {
        if ("NON_INDIVIDUAL".equalsIgnoreCase(constitutionType)) return null;
        if ("male".equalsIgnoreCase(raw)) return "M";
        return "F";
    }

    private void validateDan(String dan) {
        List<Object[]> rows = em.createNativeQuery(
            "SELECT is_used FROM dan_registry WHERE dan = ?1"
        ).setParameter(1, dan).getResultList();

        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCodes.INVALID_DAN, "LN4468: Invalid DAN — not found in registry.");
        }
        boolean isUsed = (boolean) rows.get(0)[0];
        if (isUsed) {
            throw new BusinessException(ErrorCodes.DAN_ALREADY_USED, "LN4470: DAN has already been utilised.");
        }
    }

    private String toJson(Object obj) {
        try {
            return obj.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> safe(List<T> list) {
        return list != null ? list : List.of();
    }
}
