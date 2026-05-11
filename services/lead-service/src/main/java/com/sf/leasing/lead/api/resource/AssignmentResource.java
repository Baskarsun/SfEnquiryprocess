package com.sf.leasing.lead.api.resource;

import com.sf.leasing.lead.api.dto.request.AssignLeadRequest;
import com.sf.leasing.lead.domain.model.LeadAssignment;
import com.sf.leasing.lead.infrastructure.persistence.LeadAssignmentRepository;
import com.sf.leasing.lead.infrastructure.persistence.LeadRepository;
import com.sf.leasing.lead.service.AssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leads/{lrn}/assignments")
@Tag(name = "Assignment", description = "LP5: Lead Assignment & Re-assignment")
public class AssignmentResource {

    private final AssignmentService assignmentService;
    private final LeadRepository leadRepository;
    private final LeadAssignmentRepository assignmentRepository;

    public AssignmentResource(AssignmentService assignmentService,
                               LeadRepository leadRepository,
                               LeadAssignmentRepository assignmentRepository) {
        this.assignmentService = assignmentService;
        this.leadRepository = leadRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @PostMapping
    @Operation(summary = "Assign or re-assign a lead (LP5)")
    public ResponseEntity<?> assignLead(
            @PathVariable String lrn,
            @Valid @RequestBody AssignLeadRequest req,
            @RequestHeader("X-User-Id") String userId) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        assignmentService.assignLead(lrn, req, userId);
        return ResponseEntity.ok(Map.of("message", "Lead assigned successfully."));
    }

    @GetMapping
    @Operation(summary = "Get assignment history for a lead")
    public ResponseEntity<?> getAssignmentHistory(
            @PathVariable String lrn,
            @RequestHeader("X-User-Id") String userId) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return leadRepository.findByLrn(lrn)
            .map(lead -> {
                List<LeadAssignment> history = assignmentRepository.findByLeadId(lead.id);
                return ResponseEntity.ok(history);
            })
            .orElse(ResponseEntity.notFound().build());
    }
}
