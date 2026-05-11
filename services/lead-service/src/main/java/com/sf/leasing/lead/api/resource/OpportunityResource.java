package com.sf.leasing.lead.api.resource;

import com.sf.leasing.lead.api.dto.request.CreateOpportunityRequest;
import com.sf.leasing.lead.api.dto.response.OpportunityResponse;
import com.sf.leasing.lead.service.OpportunityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Opportunity", description = "PP5: Opportunity management")
public class OpportunityResource {

    private final OpportunityService opportunityService;

    public OpportunityResource(OpportunityService opportunityService) {
        this.opportunityService = opportunityService;
    }

    /**
     * POST /api/v1/prospects/{prospectId}/opportunities
     * PP5: Create a new Opportunity for a Prospect.
     */
    @PostMapping("/prospects/{prospectId}/opportunities")
    @Operation(summary = "Create opportunity for prospect (PP5.1–PP5.4)")
    public ResponseEntity<OpportunityResponse> createOpportunity(
        @PathVariable("prospectId") String prospectId,
        @Valid @RequestBody CreateOpportunityRequest req,
        @RequestHeader("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(401).build();
        }
        OpportunityResponse opp = opportunityService.createOpportunity(prospectId, req, userId);
        return ResponseEntity.status(201).body(opp);
    }

    /**
     * GET /api/v1/prospects/{prospectId}/opportunities
     * PP5: List all opportunities for a Prospect.
     */
    @GetMapping("/prospects/{prospectId}/opportunities")
    @Operation(summary = "List opportunities for prospect")
    public ResponseEntity<List<OpportunityResponse>> listOpportunities(
        @PathVariable("prospectId") String prospectId,
        @RequestHeader("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(401).build();
        }
        List<OpportunityResponse> list = opportunityService.listByProspect(prospectId);
        return ResponseEntity.ok(list);
    }

    /**
     * GET /api/v1/opportunities/{opportunityId}
     * PP5: Get a specific Opportunity by ID.
     */
    @GetMapping("/opportunities/{opportunityId}")
    @Operation(summary = "Get opportunity by ID")
    public ResponseEntity<OpportunityResponse> getOpportunity(
        @PathVariable("opportunityId") String opportunityId,
        @RequestHeader("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(401).build();
        }
        OpportunityResponse opp = opportunityService.getByOpportunityId(opportunityId);
        return ResponseEntity.ok(opp);
    }
}
