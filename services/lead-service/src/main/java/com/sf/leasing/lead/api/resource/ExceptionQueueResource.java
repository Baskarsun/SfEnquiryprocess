package com.sf.leasing.lead.api.resource;

import com.sf.leasing.lead.domain.model.ExceptionQueueRecord;
import com.sf.leasing.lead.infrastructure.persistence.ExceptionQueueRepository;
import com.sf.leasing.lead.service.ExceptionQueueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/exception-queue")
@Tag(name = "Exception Queue", description = "LP3.4 / EX-04: Exception queue management (CPU view)")
public class ExceptionQueueResource {

    private final ExceptionQueueService exceptionQueueService;
    private final ExceptionQueueRepository exceptionQueueRepository;

    public ExceptionQueueResource(ExceptionQueueService exceptionQueueService,
                                   ExceptionQueueRepository exceptionQueueRepository) {
        this.exceptionQueueService = exceptionQueueService;
        this.exceptionQueueRepository = exceptionQueueRepository;
    }

    @GetMapping
    @Operation(summary = "List exception queue entries")
    public ResponseEntity<?> listExceptions(
            @RequestParam(defaultValue = "OPEN") String status,
            @RequestParam(defaultValue = "0")    int page,
            @RequestParam(defaultValue = "50")   int size,
            @RequestHeader("X-User-Id") String userId) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        List<ExceptionQueueRecord> records = exceptionQueueRepository
            .findAll(pageable)
            .filter(r -> status.equals(r.status))
            .toList();
        return ResponseEntity.ok(records);
    }

    @PatchMapping("/{id}/resolve")
    @Operation(summary = "Resolve an exception queue entry")
    public ResponseEntity<?> resolveException(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body,
            @RequestHeader("X-User-Id") String userId) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String notes = body != null ? body.getOrDefault("resolutionNotes", "") : "";
        exceptionQueueService.resolveException(id, userId, notes);
        return ResponseEntity.ok(Map.of("message", "Exception resolved."));
    }
}
