package com.sf.leasing.lead.api.resource;

import com.sf.leasing.lead.api.dto.request.CloseProspectRequest;
import com.sf.leasing.lead.api.dto.request.PromoteToProspectRequest;
import com.sf.leasing.lead.api.dto.response.ProspectPromotionResponse;
import com.sf.leasing.lead.api.dto.response.ProspectResponse;
import com.sf.leasing.lead.domain.enums.ProspectStatus;
import com.sf.leasing.lead.domain.exception.BusinessException;
import com.sf.leasing.lead.domain.exception.ErrorCodes;
import com.sf.leasing.lead.domain.model.Prospect;
import com.sf.leasing.lead.infrastructure.persistence.ProspectRepository;
import com.sf.leasing.lead.service.ProspectPromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Prospect", description = "PP1/LP8: Prospect lifecycle management")
public class ProspectResource {

    private final ProspectPromotionService promotionService;
    private final ProspectRepository prospectRepository;

    public ProspectResource(ProspectPromotionService promotionService,
                            ProspectRepository prospectRepository) {
        this.promotionService = promotionService;
        this.prospectRepository = prospectRepository;
    }

    /**
     * POST /api/v1/leads/{lrn}/promote
     * LP8 → PP1: Run 17-point qualification checklist and promote lead to DRAFT Prospect.
     */
    @PostMapping("/leads/{lrn}/promote")
    @Operation(summary = "Promote a qualified lead to a Prospect (LP8/PP1)")
    public ResponseEntity<?> promoteToProspect(
        @PathVariable("lrn") String lrn,
        @Valid @RequestBody PromoteToProspectRequest req,
        @RequestHeader("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String promotedBy = req != null && req.promotedBy != null ? req.promotedBy : userId;
        ProspectPromotionResponse response = promotionService.promote(lrn, promotedBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/prospects/{prospectId}
     * PP1: Retrieve prospect with PAN/GSTIN masking and lineage.
     * prospectId may be the UUID (system) or the business ID (PR-YYYY-NNNNNN).
     */
    @GetMapping("/prospects/{prospectId}")
    @Operation(summary = "Get prospect details with masked KYC identifiers (PP1)")
    public ResponseEntity<?> getProspect(
        @PathVariable("prospectId") String prospectId,
        @RequestHeader("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Prospect prospect = resolveProspect(prospectId);
        if (prospect == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("{\"error\":\"Prospect not found: " + prospectId + "\"}");
        }

        ProspectResponse body = ProspectResponse.from(prospect);
        return ResponseEntity.ok(body);
    }

    /**
     * GET /api/v1/leads/{lrn}/prospect
     * PP1: Retrieve prospect linked to a Lead via LRN (lineage view).
     */
    @GetMapping("/leads/{lrn}/prospect")
    @Operation(summary = "Get prospect linked to a lead (lineage view, PP1)")
    public ResponseEntity<?> getProspectByLrn(
        @PathVariable("lrn") String lrn,
        @RequestHeader("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Prospect prospect = prospectRepository.findByLeadLrn(lrn).orElse(null);
        if (prospect == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("{\"error\":\"No prospect found for LRN: " + lrn + "\"}");
        }

        return ResponseEntity.ok(ProspectResponse.from(prospect));
    }

    /**
     * POST /api/v1/prospects/{prospectId}/close
     * Close a prospect with mandatory reason code.
     */
    @PostMapping("/prospects/{prospectId}/close")
    @Transactional
    @Operation(summary = "Close a prospect with reason (PP1)")
    public ResponseEntity<?> closeProspect(
        @PathVariable("prospectId") String prospectId,
        @Valid @RequestBody CloseProspectRequest req,
        @RequestHeader("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Prospect prospect = resolveProspect(prospectId);
        if (prospect == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("{\"error\":\"Prospect not found: " + prospectId + "\"}");
        }
        if (prospect.isClosed()) {
            throw new BusinessException(ErrorCodes.PROSPECT_ALREADY_CLOSED,
                "Prospect is already closed.");
        }

        prospect.status            = ProspectStatus.CLOSED;
        prospect.closureReasonCode = req.closureReasonCode;
        prospect.closureReasonText = req.closureReasonText;
        prospect.closedAt          = LocalDateTime.now();
        prospect.closedBy          = req.closedBy != null ? req.closedBy : userId;
        prospect.updatedBy         = userId;
        prospect.updatedAt         = LocalDateTime.now();

        prospectRepository.save(prospect);

        return ResponseEntity.ok("{\"message\":\"Prospect closed.\"}");
    }

    // -------------------------------------------------------

    private Prospect resolveProspect(String id) {
        // Try business ID first (PR-YYYY-NNNNNN)
        if (id.startsWith("PR-")) {
            return prospectRepository.findByProspectId(id).orElse(null);
        }
        // Try UUID
        try {
            return prospectRepository.findById(UUID.fromString(id)).orElse(null);
        } catch (IllegalArgumentException e) {
            return prospectRepository.findByProspectId(id).orElse(null);
        }
    }
}
