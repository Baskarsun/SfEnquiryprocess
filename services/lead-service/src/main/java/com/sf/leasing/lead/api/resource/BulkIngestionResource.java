package com.sf.leasing.lead.api.resource;

import com.sf.leasing.lead.api.dto.request.BulkIngestRequest;
import com.sf.leasing.lead.api.dto.response.BulkIngestionResponse;
import com.sf.leasing.lead.service.BulkIngestionService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.MultipartForm;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;

import java.io.InputStream;
import java.util.List;

/**
 * LP3: Bulk Upload & API Ingestion endpoints.
 *
 * POST /api/v1/bulk/excel   — multipart Excel file upload
 * POST /api/v1/bulk/api     — JSON array of lead records
 * GET  /api/v1/bulk/{jobId} — job status / summary
 */
@Path("/api/v1/bulk")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Bulk Ingestion", description = "LP3: Excel upload and API bulk lead ingestion")
public class BulkIngestionResource {

    @Inject
    BulkIngestionService bulkIngestionService;

    /**
     * POST /api/v1/bulk/excel
     * LP3.1: Upload an Excel file (.xlsx) using the approved template.
     * Multipart form field name: "file"
     */
    @POST
    @Path("/excel")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Bulk upload leads from an Excel file (LP3.1)")
    public Response uploadExcel(
        @MultipartForm ExcelUploadForm form,
        @HeaderParam("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        if (form.file == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\":\"Excel file is required\"}")
                .build();
        }

        BulkIngestionResponse result = bulkIngestionService.ingestFromExcel(form.file, userId);
        return Response.ok(result).build();
    }

    /**
     * POST /api/v1/bulk/api
     * LP3.2: Ingest leads via authenticated API with JSON payload.
     */
    @POST
    @Path("/api")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Bulk ingest leads via API (LP3.2)")
    public Response ingestViaApi(
        List<BulkIngestRequest> records,
        @HeaderParam("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        if (records == null || records.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\":\"At least one record is required\"}")
                .build();
        }

        BulkIngestionResponse result = bulkIngestionService.ingestFromApi(records, userId);
        return Response.ok(result).build();
    }

    // -------------------------------------------------------
    // Multipart form holder
    // -------------------------------------------------------

    public static class ExcelUploadForm {
        @RestForm("file")
        @PartType(MediaType.APPLICATION_OCTET_STREAM)
        public InputStream file;
    }
}
