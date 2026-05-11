package com.sf.leasing.lead.api.resource;

import com.sf.leasing.lead.api.dto.request.LogInteractionRequest;
import com.sf.leasing.lead.domain.model.Interaction;
import com.sf.leasing.lead.infrastructure.persistence.InteractionRepository;
import com.sf.leasing.lead.infrastructure.persistence.LeadRepository;
import com.sf.leasing.lead.service.InteractionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/leads/{lrn}/interactions")
@Tag(name = "Interactions", description = "LP6: Interaction Logging & Next Action Scheduling")
public class InteractionResource {

    private final InteractionService interactionService;
    private final LeadRepository leadRepository;
    private final InteractionRepository interactionRepository;

    public InteractionResource(InteractionService interactionService,
                                LeadRepository leadRepository,
                                InteractionRepository interactionRepository) {
        this.interactionService = interactionService;
        this.leadRepository = leadRepository;
        this.interactionRepository = interactionRepository;
    }

    @PostMapping
    @Operation(summary = "Log an interaction for a lead (LP6)")
    public ResponseEntity<Interaction> logInteraction(
            @PathVariable String lrn,
            @Valid @RequestBody LogInteractionRequest req,
            @RequestHeader("X-User-Id") String userId) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Interaction interaction = interactionService.logInteraction(lrn, req, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(interaction);
    }

    @GetMapping
    @Operation(summary = "Get interaction history for a lead")
    public ResponseEntity<?> getInteractionHistory(
            @PathVariable String lrn,
            @RequestHeader("X-User-Id") String userId) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return leadRepository.findByLrn(lrn)
            .map(lead -> {
                List<Interaction> history = interactionRepository.findByLeadIdOrderByInteractionTimestampAsc(lead.id);
                return ResponseEntity.ok(history);
            })
            .orElse(ResponseEntity.notFound().build());
    }
}
