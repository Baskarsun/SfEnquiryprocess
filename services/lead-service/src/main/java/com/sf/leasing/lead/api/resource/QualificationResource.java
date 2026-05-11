package com.sf.leasing.lead.api.resource;

import com.sf.leasing.lead.api.dto.request.CloseLeadRequest;
import com.sf.leasing.lead.api.dto.request.RunDedupRequest;
import com.sf.leasing.lead.domain.enums.DedupLabel;
import com.sf.leasing.lead.domain.model.Lead;
import com.sf.leasing.lead.domain.model.LeadDedupResult;
import com.sf.leasing.lead.infrastructure.persistence.LeadDedupResultRepository;
import com.sf.leasing.lead.infrastructure.persistence.LeadRepository;
import com.sf.leasing.lead.service.DeduplicationService;
import com.sf.leasing.lead.service.LeadQualificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * LP4 + LP8: Deduplication and Lead Qualification endpoints.
 *
 * POST /api/v1/leads/{lrn}/dedup          — run dedup checks
 * GET  /api/v1/leads/{lrn}/dedup          — view dedup results
 * POST /api/v1/leads/{lrn}/validate       — pre-promotion validation (LP8)
 * POST /api/v1/leads/{lrn}/close          — close lead with mandatory reason
 */
@RestController
@RequestMapping("/api/v1/leads")
@Tag(name = "Lead Qualification", description = "LP4 deduplication and LP8 pre-promotion validation")
public class QualificationResource {

    private final DeduplicationService deduplicationService;
    private final LeadQualificationService qualificationService;
    private final LeadRepository leadRepository;
    private final LeadDedupResultRepository leadDedupResultRepository;

    public QualificationResource(DeduplicationService deduplicationService,
                                  LeadQualificationService qualificationService,
                                  LeadRepository leadRepository,
                                  LeadDedupResultRepository leadDedupResultRepository) {
        this.deduplicationService = deduplicationService;
        this.qualificationService = qualificationService;
        this.leadRepository = leadRepository;
        this.leadDedupResultRepository = leadDedupResultRepository;
    }

    /**
     * POST /api/v1/leads/{lrn}/dedup
     * LP4: Run identity deduplication on an existing lead.
     */
    @PostMapping("/{lrn}/dedup")
    @Operation(summary = "Run deduplication checks on a lead (LP4)")
    public ResponseEntity<?> runDedup(
        @PathVariable("lrn") String lrn,
        @RequestBody RunDedupRequest req,
        @RequestHeader("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Lead lead = leadRepository.findByLrn(lrn).orElse(null);
        if (lead == null) return ResponseEntity.notFound().build();

        boolean includeExternal = req == null || req.includeExternalChecks;
        DedupLabel label = deduplicationService.runDedupChecks(lead, includeExternal);

        return ResponseEntity.ok(Map.of("lrn", lrn, "dedupLabel", label.name()));
    }

    /**
     * GET /api/v1/leads/{lrn}/dedup
     * View all recorded dedup results for a lead.
     */
    @GetMapping("/{lrn}/dedup")
    @Operation(summary = "Retrieve deduplication results for a lead (LP4)")
    public ResponseEntity<?> getDedupResults(
        @PathVariable("lrn") String lrn,
        @RequestHeader("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Lead lead = leadRepository.findByLrn(lrn).orElse(null);
        if (lead == null) return ResponseEntity.notFound().build();

        List<LeadDedupResult> results = leadDedupResultRepository.findByLeadId(lead.id);
        return ResponseEntity.ok(results);
    }

    /**
     * POST /api/v1/leads/{lrn}/validate
     * LP8: Run pre-promotion validation checklist.
     * Returns 200 with any warnings if all checks pass.
     * Returns 422 with error detail if any check fails.
     */
    @PostMapping("/{lrn}/validate")
    @Operation(summary = "Run LP8 pre-promotion validation (LP8)")
    public ResponseEntity<?> validateForPromotion(
        @PathVariable("lrn") String lrn,
        @RequestHeader("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<String> warnings = qualificationService.validateForPromotion(lrn);
        return ResponseEntity.ok(Map.of(
            "lrn", lrn,
            "status", "ELIGIBLE",
            "warnings", warnings
        ));
    }

    /**
     * POST /api/v1/leads/{lrn}/close
     * LP8 closure sub-flow: ClosureReason mandatory; record locked post-closure.
     */
    @PostMapping("/{lrn}/close")
    @Operation(summary = "Close a lead with mandatory closure reason (LP8)")
    public ResponseEntity<?> closeLead(
        @PathVariable("lrn") String lrn,
        @Valid @RequestBody CloseLeadRequest req,
        @RequestHeader("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        qualificationService.closeLead(lrn, req, userId);
        return ResponseEntity.ok(Map.of("lrn", lrn, "status", "CLOSED"));
    }
}
