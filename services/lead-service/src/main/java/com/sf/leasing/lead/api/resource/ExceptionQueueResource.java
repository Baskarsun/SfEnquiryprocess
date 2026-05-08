package com.sf.leasing.lead.api.resource;

import com.sf.leasing.lead.domain.model.ExceptionQueueRecord;
import com.sf.leasing.lead.service.ExceptionQueueService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/api/v1/exception-queue")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Exception Queue", description = "LP3.4 / EX-04: Exception queue management (CPU view)")
public class ExceptionQueueResource {

    @Inject
    ExceptionQueueService exceptionQueueService;

    @GET
    @Operation(summary = "List exception queue entries")
    public Response listExceptions(
        @QueryParam("status")  @DefaultValue("OPEN") String status,
        @QueryParam("page")    @DefaultValue("0")    int page,
        @QueryParam("size")    @DefaultValue("50")   int size,
        @HeaderParam("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        List<ExceptionQueueRecord> records = ExceptionQueueRecord.find("status = ?1 ORDER BY createdAt ASC", status)
            .page(page, size)
            .list();
        return Response.ok(records).build();
    }

    @PATCH
    @Path("/{id}/resolve")
    @Operation(summary = "Resolve an exception queue entry")
    public Response resolveException(
        @PathParam("id") UUID id,
        Map<String, String> body,
        @HeaderParam("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String notes = body != null ? body.getOrDefault("resolutionNotes", "") : "";
        exceptionQueueService.resolveException(id, userId, notes);
        return Response.ok().entity("{\"message\":\"Exception resolved.\"}").build();
    }
}
