package com.sf.leasing.lead.service;

import com.sf.leasing.lead.domain.enums.ProspectStatus;
import com.sf.leasing.lead.domain.exception.BusinessException;
import com.sf.leasing.lead.domain.exception.ErrorCodes;
import com.sf.leasing.lead.domain.model.Lineage;
import com.sf.leasing.lead.domain.model.Prospect;
import com.sf.leasing.lead.domain.model.ProspectKycValidation;
import com.sf.leasing.lead.infrastructure.adapter.GstinValidationAdapter;
import com.sf.leasing.lead.infrastructure.adapter.PanValidationAdapter;
import com.sf.leasing.lead.infrastructure.locking.RedisSequenceGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Implements PP3: External KYC Validation for Prospects.
 *
 * Key business rules:
 * - Prospect.prospectId (PR-YYYY-NNNNNN) is null until this service generates it on first SUCCESS.
 * - Failure routes to the exception queue with a 24-hour SLA.
 * - Each validation run produces an immutable ProspectKycValidation record.
 * - Manual override produces an OVERRIDDEN record and follows the same ID-generation path as SUCCESS.
 * - Lineage.prospectBusinessId is updated once the Prospect ID is assigned.
 */
@ApplicationScoped
public class ProspectValidationService {

    private static final Logger LOG = Logger.getLogger(ProspectValidationService.class);

    @ConfigProperty(name = "prospect.validation-failure-sla-hours", defaultValue = "24")
    int validationFailureSlaHours;

    @Inject
    PanValidationAdapter panAdapter;

    @Inject
    GstinValidationAdapter gstinAdapter;

    @Inject
    RedisSequenceGenerator sequenceGenerator;

    @Inject
    ExceptionQueueService exceptionQueueService;

    @Inject
    NotificationService notificationService;

    // -------------------------------------------------------
    // PP3.1 — Trigger external KYC validation
    // -------------------------------------------------------

    @Transactional
    public Prospect triggerValidation(UUID prospectId, String rawType, String triggeredBy) {
        // Validate type before DB lookup so the caller gets the right error code
        String type = (rawType != null ? rawType : "").toUpperCase().trim();
        validateType(type);

        Prospect prospect = Prospect.findById(prospectId);
        if (prospect == null) {
            throw new BusinessException(ErrorCodes.PROSPECT_NOT_FOUND, "Prospect not found: " + prospectId);
        }
        if (prospect.isClosed()) {
            throw new BusinessException(ErrorCodes.PROSPECT_ALREADY_CLOSED, "Cannot validate a closed prospect.");
        }

        if ("PAN".equals(type)) {
            return validatePan(prospect, triggeredBy);
        } else {
            return validateGstin(prospect, triggeredBy);
        }
    }

    // -------------------------------------------------------
    // PP3.2 — Manual override (authorised officer)
    // -------------------------------------------------------

    @Transactional
    public Prospect override(UUID prospectId, String rawType, String overrideReasonCode,
                             String overrideReasonText, String overrideBy) {
        String validationType = (rawType != null ? rawType : "").toUpperCase().trim();
        validateType(validationType);

        Prospect prospect = Prospect.findById(prospectId);
        if (prospect == null) {
            throw new BusinessException(ErrorCodes.PROSPECT_NOT_FOUND, "Prospect not found: " + prospectId);
        }
        if (prospect.isClosed()) {
            throw new BusinessException(ErrorCodes.PROSPECT_ALREADY_CLOSED, "Cannot override a closed prospect.");
        }

        // Record OVERRIDDEN validation entry
        ProspectKycValidation kycRecord = new ProspectKycValidation();
        kycRecord.prospect             = prospect;
        kycRecord.validationType       = validationType;
        kycRecord.identifier           = "PAN".equals(validationType) ? prospect.pan : prospect.gstin;
        kycRecord.status               = "OVERRIDDEN";
        kycRecord.overrideReasonCode   = overrideReasonCode;
        kycRecord.overrideReasonText   = overrideReasonText;
        kycRecord.overrideBy           = overrideBy;
        kycRecord.overrideAt           = LocalDateTime.now();
        kycRecord.validatedAt          = LocalDateTime.now();
        kycRecord.persist();

        // Mark the specific KYC flag as validated
        applyValidationFlag(prospect, validationType, null, null, null);

        // Generate Prospect ID if not yet assigned (PP3.1 gating)
        assignProspectIdIfAbsent(prospect);

        // Update override audit on Prospect record
        prospect.validationOverrideReason  = overrideReasonCode + ": " + overrideReasonText;
        prospect.validationOverrideBy      = overrideBy;
        prospect.validationOverrideAt      = LocalDateTime.now();
        prospect.updatedBy                 = overrideBy;
        prospect.updatedAt                 = LocalDateTime.now();

        notificationService.notifyKycOverride(prospect.prospectId, validationType, overrideBy);
        LOG.infof("KYC override recorded: Prospect=%s type=%s by=%s", prospect.prospectId, validationType, overrideBy);

        return prospect;
    }

    // -------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------

    private Prospect validatePan(Prospect prospect, String triggeredBy) {
        if (prospect.pan == null || prospect.pan.isBlank()) {
            throw new BusinessException(ErrorCodes.PAN_NOT_SET, "Prospect has no PAN to validate.");
        }

        PanValidationAdapter.PanValidationResult result = panAdapter.validate(prospect.pan);

        ProspectKycValidation kycRecord = new ProspectKycValidation();
        kycRecord.prospect             = prospect;
        kycRecord.validationType       = "PAN";
        kycRecord.identifier           = prospect.pan;
        kycRecord.validationSource     = "PAN_API";
        kycRecord.validatedAt          = LocalDateTime.now();

        if (result.valid()) {
            kycRecord.status            = "SUCCESS";
            kycRecord.legalName         = result.legalName();
            kycRecord.registeredAddress = result.registeredAddress();
            kycRecord.persist();

            applyValidationFlag(prospect, "PAN", result.legalName(), result.registeredAddress(), null);
            assignProspectIdIfAbsent(prospect);

            notificationService.notifyKycValidationSuccess(prospect.id.toString(), prospect.prospectId);
            LOG.infof("PAN validated: Prospect=%s pan=%s", prospect.id, prospect.pan);
        } else {
            kycRecord.status = "FAILED";
            kycRecord.persist();

            routeToExceptionQueue(prospect, "PAN", result.errorMessage());
            notificationService.notifyKycValidationFailure(
                prospect.id.toString(), "PAN", result.errorMessage());
            LOG.warnf("PAN validation failed: Prospect=%s reason=%s", prospect.id, result.errorMessage());
        }

        return prospect;
    }

    private Prospect validateGstin(Prospect prospect, String triggeredBy) {
        if (prospect.gstin == null || prospect.gstin.isBlank()) {
            throw new BusinessException(ErrorCodes.GSTIN_NOT_SET, "Prospect has no GSTIN to validate.");
        }

        GstinValidationAdapter.GstinValidationResult result = gstinAdapter.validate(prospect.gstin);

        ProspectKycValidation kycRecord = new ProspectKycValidation();
        kycRecord.prospect             = prospect;
        kycRecord.validationType       = "GSTIN";
        kycRecord.identifier           = prospect.gstin;
        kycRecord.validationSource     = "GSTIN_API";
        kycRecord.validatedAt          = LocalDateTime.now();

        if (result.valid()) {
            kycRecord.status            = "SUCCESS";
            kycRecord.legalName         = result.legalEntityName();
            kycRecord.registeredAddress = result.registeredAddress();
            kycRecord.derivedPan        = result.derivedPan();
            kycRecord.persist();

            applyValidationFlag(prospect, "GSTIN", result.legalEntityName(), result.registeredAddress(), result.derivedPan());
            assignProspectIdIfAbsent(prospect);

            notificationService.notifyKycValidationSuccess(prospect.id.toString(), prospect.prospectId);
            LOG.infof("GSTIN validated: Prospect=%s gstin=%s", prospect.id, prospect.gstin);
        } else {
            kycRecord.status = "FAILED";
            kycRecord.persist();

            routeToExceptionQueue(prospect, "GSTIN", result.errorMessage());
            notificationService.notifyKycValidationFailure(
                prospect.id.toString(), "GSTIN", result.errorMessage());
            LOG.warnf("GSTIN validation failed: Prospect=%s reason=%s", prospect.id, result.errorMessage());
        }

        return prospect;
    }

    private void applyValidationFlag(Prospect prospect, String type,
                                     String legalName, String address, String derivedPan) {
        if ("PAN".equals(type)) {
            prospect.panValidated          = true;
            if (legalName != null)  prospect.legalName         = legalName;
            if (address   != null)  prospect.registeredAddress = address;
        } else {
            prospect.gstinValidated        = true;
            if (legalName != null)  prospect.legalName         = legalName;
            if (address   != null)  prospect.registeredAddress = address;
            if (derivedPan != null) prospect.pan               = derivedPan; // cross-reference PAN from GSTIN
        }

        // Transition to VALIDATED when at least one check passes
        if (prospect.status == ProspectStatus.DRAFT) {
            prospect.status             = ProspectStatus.VALIDATED;
            prospect.validationTimestamp = LocalDateTime.now();
        }

        prospect.updatedAt = LocalDateTime.now();
    }

    /** PP3.1 gating: generate PR-YYYY-NNNNNN only once. */
    private void assignProspectIdIfAbsent(Prospect prospect) {
        if (prospect.prospectId != null) return;

        String newId = sequenceGenerator.generateProspectId();
        prospect.prospectId = newId;

        // Update Lineage with the business ID
        Lineage lineage = Lineage.findByProspectUuid(prospect.id);
        if (lineage != null) {
            lineage.prospectBusinessId = newId;
            lineage.updatedAt          = LocalDateTime.now();
        }

        LOG.infof("Prospect ID assigned: %s → %s", prospect.id, newId);
    }

    private void routeToExceptionQueue(Prospect prospect, String validationType, String reason) {
        exceptionQueueService.routeToExceptionQueue(
            "PROSPECT_VALIDATION",
            prospect.id.toString(),
            ErrorCodes.KYC_VALIDATION_FAILED,
            validationType + " validation failed: " + reason,
            "prospect_id",
            "{\"prospectId\":\"" + prospect.id + "\",\"type\":\"" + validationType + "\"}"
        );
    }

    private void validateType(String type) {
        if (!"PAN".equals(type) && !"GSTIN".equals(type)) {
            throw new BusinessException(ErrorCodes.INVALID_VALIDATION_TYPE,
                "validationType must be PAN or GSTIN, got: " + type);
        }
    }
}
