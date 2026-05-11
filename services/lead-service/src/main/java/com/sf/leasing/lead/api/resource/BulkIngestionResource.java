package com.sf.leasing.lead.api.resource;

import com.sf.leasing.lead.api.dto.request.BulkIngestRequest;
import com.sf.leasing.lead.api.dto.response.BulkIngestionResponse;
import com.sf.leasing.lead.service.BulkIngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * LP3: Bulk Upload & API Ingestion endpoints.
 *
 * POST /api/v1/bulk/excel   — multipart Excel file upload
 * POST /api/v1/bulk/api     — JSON array of lead records
 * GET  /api/v1/bulk/{jobId} — job status / summary
 */
@RestController
@RequestMapping("/api/v1/bulk")
@Tag(name = "Bulk Ingestion", description = "LP3: Excel upload and API bulk lead ingestion")
public class BulkIngestionResource {

    private final BulkIngestionService bulkIngestionService;

    public BulkIngestionResource(BulkIngestionService bulkIngestionService) {
        this.bulkIngestionService = bulkIngestionService;
    }

    /**
     * POST /api/v1/bulk/excel
     * LP3.1: Upload an Excel file (.xlsx) using the approved template.
     * Multipart form field name: "file"
     */
    @PostMapping("/excel")
    @Operation(summary = "Bulk upload leads from an Excel file (LP3.1)")
    public ResponseEntity<?> uploadExcel(
        @RequestParam("file") MultipartFile file,
        @RequestHeader("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("{\"error\":\"Excel file is required\"}");
        }

        try {
            BulkIngestionResponse result = bulkIngestionService.ingestFromExcel(file.getInputStream(), userId);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("{\"error\":\"Failed to read uploaded file\"}");
        }
    }

    /**
     * POST /api/v1/bulk/api
     * LP3.2: Ingest leads via authenticated API with JSON payload.
     */
    @PostMapping("/api")
    @Operation(summary = "Bulk ingest leads via API (LP3.2)")
    public ResponseEntity<?> ingestViaApi(
        @RequestBody List<BulkIngestRequest> records,
        @RequestHeader("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (records == null || records.isEmpty()) {
            return ResponseEntity.badRequest().body("{\"error\":\"At least one record is required\"}");
        }

        BulkIngestionResponse result = bulkIngestionService.ingestFromApi(records, userId);
        return ResponseEntity.ok(result);
    }
}
