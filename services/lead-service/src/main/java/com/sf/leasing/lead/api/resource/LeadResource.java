package com.sf.leasing.lead.api.resource;

import com.sf.leasing.lead.api.dto.request.CreateLeadRequest;
import com.sf.leasing.lead.api.dto.response.CreateLeadResponse;
import com.sf.leasing.lead.domain.enums.Channel;
import com.sf.leasing.lead.domain.enums.LeadStatus;
import com.sf.leasing.lead.domain.enums.LeadTemperature;
import com.sf.leasing.lead.domain.model.Lead;
import com.sf.leasing.lead.infrastructure.persistence.LeadRepository;
import com.sf.leasing.lead.service.LeadCreationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/leads")
@Tag(name = "Lead Management", description = "LP1-LP2: Lead creation and retrieval")
public class LeadResource {

    private static final int MAX_PAGE_SIZE = 100;

    private final LeadCreationService leadCreationService;
    private final LeadRepository leadRepository;

    public LeadResource(LeadCreationService leadCreationService,
                        LeadRepository leadRepository) {
        this.leadCreationService = leadCreationService;
        this.leadRepository = leadRepository;
    }

    @PostMapping
    @Operation(summary = "Create a new leasing lead (LP1 + LP2)")
    public ResponseEntity<CreateLeadResponse> createLead(
            @Valid @RequestBody CreateLeadRequest req,
            @RequestHeader("X-User-Id")                          String userId,
            @RequestHeader("X-Device-Id")                        String primaryDeviceId,
            @RequestHeader(value = "X-Device-Id-Alt", required = false) String secondaryDeviceId,
            @RequestHeader(value = "X-Channel", defaultValue = "DESKTOP") String channelHeader,
            @RequestHeader(value = "X-GPS-Lat", required = false) Double latitude,
            @RequestHeader(value = "X-GPS-Lon", required = false) Double longitude) {

        leadCreationService.validateUserAndDevice(userId, primaryDeviceId, secondaryDeviceId);
        Channel channel = parseChannel(channelHeader);
        CreateLeadResponse result = leadCreationService.createLead(req, userId, channel, latitude, longitude);

        return result.routedToExceptionQueue
            ? ResponseEntity.accepted().body(result)
            : ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/{lrn}")
    @Operation(summary = "Get lead by LRN")
    public ResponseEntity<Lead> getLeadByLrn(
            @PathVariable String lrn,
            @RequestHeader("X-User-Id") String userId) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return leadRepository.findByLrn(lrn)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "Search leads by status, assigned user, or branch")
    public ResponseEntity<?> searchLeads(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String assignedTo,
            @RequestParam(required = false) String branchCode,
            @RequestParam(required = false) String temperature,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("X-User-Id") String userId) {

        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (page < 0) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "page must be >= 0"));
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            return ResponseEntity.badRequest()
                .body(java.util.Map.of("error", "size must be between 1 and " + MAX_PAGE_SIZE));
        }

        LeadStatus parsedStatus = null;
        if (status != null) {
            try {
                parsedStatus = LeadStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", "Invalid status value. Allowed: NEW, ASSIGNED, IN_PROGRESS, PROMOTED, CLOSED"));
            }
        }

        LeadTemperature parsedTemp = null;
        if (temperature != null) {
            try {
                parsedTemp = LeadTemperature.valueOf(temperature.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", "Invalid temperature value. Allowed: HOT, WARM, COLD"));
            }
        }

        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE));
        Page<Lead> results = leadRepository.search(parsedStatus, assignedTo, branchCode, parsedTemp, pageable);
        return ResponseEntity.ok(results);
    }

    private Channel parseChannel(String header) {
        if (header == null) return Channel.DESKTOP;
        try {
            return Channel.valueOf(header.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Channel.DESKTOP;
        }
    }
}
