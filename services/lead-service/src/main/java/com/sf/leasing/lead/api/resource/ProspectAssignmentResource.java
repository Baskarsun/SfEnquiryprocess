package com.sf.leasing.lead.api.resource;

import com.sf.leasing.lead.api.dto.request.AssignProspectRequest;
import com.sf.leasing.lead.domain.model.ProspectAssignment;
import com.sf.leasing.lead.service.ProspectAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/prospects/{prospectId}/assignments")
@Tag(name = "ProspectAssignment", description = "PP2: Prospect Assignment Workflow")
public class ProspectAssignmentResource {

    private final ProspectAssignmentService assignmentService;

    public ProspectAssignmentResource(ProspectAssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    /**
     * POST /api/v1/prospects/{prospectId}/assignments
     * PP2: Assign or re-assign a prospect.
     * Assignment chain: CPU → Branch Manager → Associate FO.
     * Each assignment creates an immutable audit record.
     */
    @PostMapping
    @Operation(summary = "Assign or re-assign a prospect (PP2)")
    public ResponseEntity<?> assignProspect(
        @PathVariable("prospectId") UUID prospectId,
        @Valid @RequestBody AssignProspectRequest req,
        @RequestHeader("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        ProspectAssignment assignment = assignmentService.assign(prospectId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(assignment);
    }

    /**
     * GET /api/v1/prospects/{prospectId}/assignments
     * PP2: Retrieve full immutable assignment audit history.
     */
    @GetMapping
    @Operation(summary = "Get assignment history for a prospect (PP2)")
    public ResponseEntity<?> getAssignmentHistory(
        @PathVariable("prospectId") UUID prospectId,
        @RequestHeader("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<ProspectAssignment> history = assignmentService.getAssignmentHistory(prospectId);
        return ResponseEntity.ok(history);
    }
}
