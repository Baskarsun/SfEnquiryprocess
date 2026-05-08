package com.sf.leasing.lead.api.resource;

import com.sf.leasing.lead.api.dto.request.LogMeetingRequest;
import com.sf.leasing.lead.domain.model.Meeting;
import com.sf.leasing.lead.service.MeetingService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/prospects/{prospectId}/meetings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Meeting", description = "PP4: Meeting & Interaction Management")
public class MeetingResource {

    @Inject
    MeetingService meetingService;

    /**
     * POST /api/v1/prospects/{prospectId}/meetings
     * PP4: Log a new meeting/interaction. Generates a unique MTG-YYYY-NNNNN ID.
     * Meetings are immutable once created.
     */
    @POST
    @Operation(summary = "Log a new meeting for a prospect (PP4)")
    public Response logMeeting(
        @PathParam("prospectId") UUID prospectId,
        @Valid LogMeetingRequest req,
        @HeaderParam("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        Meeting meeting = meetingService.logMeeting(prospectId, req, userId);
        return Response.status(Response.Status.CREATED).entity(meeting).build();
    }

    /**
     * POST /api/v1/prospects/{prospectId}/meetings/{meetingId}/amend
     * PP4: Create an amendment — a new Meeting record with originalMeetingId set.
     * The original meeting record is never modified.
     */
    @POST
    @Path("/{meetingId}/amend")
    @Operation(summary = "Amend a meeting (creates new record, original preserved) (PP4)")
    public Response amendMeeting(
        @PathParam("prospectId") UUID prospectId,
        @PathParam("meetingId") String meetingId,
        @Valid LogMeetingRequest req,
        @HeaderParam("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        Meeting amended = meetingService.amendMeeting(prospectId, meetingId, req, userId);
        return Response.status(Response.Status.CREATED).entity(amended).build();
    }

    /**
     * GET /api/v1/prospects/{prospectId}/meetings
     * PP4: Retrieve all meetings for a prospect (ordered by meeting_datetime ASC).
     */
    @GET
    @Operation(summary = "Get all meetings for a prospect (PP4)")
    public Response getMeetings(
        @PathParam("prospectId") UUID prospectId,
        @HeaderParam("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        List<Meeting> meetings = meetingService.getMeetings(prospectId);
        return Response.ok(meetings).build();
    }
}
