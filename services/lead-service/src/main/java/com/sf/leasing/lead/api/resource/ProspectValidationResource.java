package com.sf.leasing.lead.api.resource;

import com.sf.leasing.lead.api.dto.request.OverrideKycValidationRequest;
import com.sf.leasing.lead.api.dto.request.TriggerKycValidationRequest;
import com.sf.leasing.lead.api.dto.response.ProspectResponse;
import com.sf.leasing.lead.domain.model.Prospect;
import com.sf.leasing.lead.domain.model.ProspectKycValidation;
import com.sf.leasing.lead.infrastructure.persistence.ProspectKycValidationRepository;
import com.sf.leasing.lead.service.ProspectValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/prospects/{prospectId}/kyc")
@Tag(name = "ProspectKYC", description = "PP3: External KYC validation and override")
public class ProspectValidationResource {

    private final ProspectValidationService validationService;
    private final ProspectKycValidationRepository prospectKycValidationRepository;

    public ProspectValidationResource(ProspectValidationService validationService,
                                      ProspectKycValidationRepository prospectKycValidationRepository) {
        this.validationService = validationService;
        this.prospectKycValidationRepository = prospectKycValidationRepository;
    }

    /**
     * POST /api/v1/prospects/{prospectId}/kyc/validate
     * PP3.1: Trigger external PAN or GSTIN validation.
     * On success → Prospect ID generated, status → VALIDATED.
     * On failure → exception queue with 24-hr SLA.
     */
    @PostMapping("/validate")
    @Operation(summary = "Trigger KYC validation for PAN or GSTIN (PP3)")
    public ResponseEntity<?> triggerValidation(
        @PathVariable("prospectId") UUID prospectId,
        @Valid @RequestBody TriggerKycValidationRequest req,
        @RequestHeader("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Prospect updated = validationService.triggerValidation(
            prospectId, req.validationType, req.triggeredBy);
        return ResponseEntity.ok(ProspectResponse.from(updated));
    }

    /**
     * POST /api/v1/prospects/{prospectId}/kyc/override
     * PP3.2: Manual override by authorised officer when external validation is inconclusive.
     * Override reason and officer ID are mandatory audit fields.
     */
    @PostMapping("/override")
    @Operation(summary = "Manual KYC override by authorised officer (PP3)")
    public ResponseEntity<?> overrideValidation(
        @PathVariable("prospectId") UUID prospectId,
        @Valid @RequestBody OverrideKycValidationRequest req,
        @RequestHeader("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Prospect updated = validationService.override(
            prospectId, req.validationType,
            req.overrideReasonCode, req.overrideReasonText,
            req.overrideBy != null ? req.overrideBy : userId);
        return ResponseEntity.ok(ProspectResponse.from(updated));
    }

    /**
     * GET /api/v1/prospects/{prospectId}/kyc/history
     * Returns all KYC validation attempts (immutable audit log).
     */
    @GetMapping("/history")
    @Operation(summary = "Get KYC validation history for a prospect (PP3)")
    public ResponseEntity<?> getKycHistory(
        @PathVariable("prospectId") UUID prospectId,
        @RequestHeader("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<ProspectKycValidation> history = prospectKycValidationRepository.findByProspectId(prospectId);
        return ResponseEntity.ok(history);
    }
}
