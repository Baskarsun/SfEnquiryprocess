package com.sf.leasing.lead.api.resource;

import com.sf.leasing.lead.api.dto.request.LogMeetingRequest;
import com.sf.leasing.lead.domain.model.Meeting;
import com.sf.leasing.lead.service.MeetingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/prospects/{prospectId}/meetings")
@Tag(name = "Meeting", description = "PP4: Meeting & Interaction Management")
public class MeetingResource {

    private final MeetingService meetingService;

    public MeetingResource(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    /**
     * POST /api/v1/prospects/{prospectId}/meetings
     * PP4: Log a new meeting/interaction. Generates a unique MTG-YYYY-NNNNN ID.
     * Meetings are immutable once created.
     */
    @PostMapping
    @Operation(summary = "Log a new meeting for a prospect (PP4)")
    public ResponseEntity<?> logMeeting(
        @PathVariable("prospectId") UUID prospectId,
        @Valid @RequestBody LogMeetingRequest req,
        @RequestHeader("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Meeting meeting = meetingService.logMeeting(prospectId, req, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(meeting);
    }

    /**
     * POST /api/v1/prospects/{prospectId}/meetings/{meetingId}/amend
     * PP4: Create an amendment — a new Meeting record with originalMeetingId set.
     * The original meeting record is never modified.
     */
    @PostMapping("/{meetingId}/amend")
    @Operation(summary = "Amend a meeting (creates new record, original preserved) (PP4)")
    public ResponseEntity<?> amendMeeting(
        @PathVariable("prospectId") UUID prospectId,
        @PathVariable("meetingId") String meetingId,
        @Valid @RequestBody LogMeetingRequest req,
        @RequestHeader("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Meeting amended = meetingService.amendMeeting(prospectId, meetingId, req, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(amended);
    }

    /**
     * GET /api/v1/prospects/{prospectId}/meetings
     * PP4: Retrieve all meetings for a prospect (ordered by meeting_datetime ASC).
     */
    @GetMapping
    @Operation(summary = "Get all meetings for a prospect (PP4)")
    public ResponseEntity<?> getMeetings(
        @PathVariable("prospectId") UUID prospectId,
        @RequestHeader("X-User-Id") String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<Meeting> meetings = meetingService.getMeetings(prospectId);
        return ResponseEntity.ok(meetings);
    }
}
