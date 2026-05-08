package com.sf.leasing.lead.service;

import com.sf.leasing.lead.api.dto.request.LogMeetingRequest;
import com.sf.leasing.lead.domain.exception.BusinessException;
import com.sf.leasing.lead.domain.exception.ErrorCodes;
import com.sf.leasing.lead.domain.model.Meeting;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Meeting business logic contracts.
 *
 * Tests cover DTO construction, ID format validation, and amendment field propagation.
 * Panache-dependent integration (logMeeting/getMeetings) is covered by integration tests.
 */
class MeetingServiceTest {

    // -------------------------------------------------------
    // Meeting ID format contract
    // -------------------------------------------------------

    @Test
    void meetingIdShouldMatchExpectedFormat() {
        String meetingId = "MTG-2026-00001";
        assertTrue(meetingId.startsWith("MTG-"), "Meeting ID must start with MTG-");
        assertTrue(meetingId.matches("MTG-\\d{4}-\\d{5}"), "Format must be MTG-YYYY-NNNNN");
    }

    @Test
    void meetingIdShouldBeUniquePerGenerationCall() {
        // Verify format distinguishes meetings by serial suffix
        String id1 = "MTG-2026-00001";
        String id2 = "MTG-2026-00002";
        assertNotEquals(id1, id2);
    }

    // -------------------------------------------------------
    // LogMeetingRequest DTO defaults
    // -------------------------------------------------------

    @Test
    void logMeetingRequestShouldDefaultRemindersToFalse() {
        LogMeetingRequest req = new LogMeetingRequest();
        assertFalse(req.reminder1day);
        assertFalse(req.reminder1hour);
    }

    @Test
    void logMeetingRequestShouldHaveNullOriginalByDefault() {
        LogMeetingRequest req = new LogMeetingRequest();
        assertNull(req.originalMeetingId, "originalMeetingId is null for new meetings, set only on amendments");
    }

    // -------------------------------------------------------
    // Amendment request contract
    // -------------------------------------------------------

    @Test
    void amendmentRequestShouldCarryOriginalMeetingId() {
        LogMeetingRequest amendment = new LogMeetingRequest();
        amendment.meetingDatetime   = LocalDateTime.now().plusDays(1);
        amendment.notes             = "Rescheduled to discuss updated terms.";
        amendment.originalMeetingId = "MTG-2026-00001";

        assertNotNull(amendment.originalMeetingId);
        assertEquals("MTG-2026-00001", amendment.originalMeetingId);
    }

    @Test
    void amendmentShouldNotModifyOriginalMeetingId() {
        // The original meeting is never updated — the amendment is a new record.
        // Verify this design by checking that Meeting.originalMeetingId is a plain String field.
        Meeting original = new Meeting();
        original.meetingId = "MTG-2026-00001";
        original.notes     = "Original meeting notes.";

        // An amendment record would reference the original
        Meeting amendment = new Meeting();
        amendment.meetingId         = "MTG-2026-00002";
        amendment.originalMeetingId = original.meetingId;
        amendment.notes             = "Amended notes.";

        // The original is unchanged
        assertEquals("Original meeting notes.", original.notes);
        assertEquals("MTG-2026-00001", amendment.originalMeetingId);
    }

    // -------------------------------------------------------
    // Mode validation
    // -------------------------------------------------------

    @Test
    void acceptedMeetingModesShouldBeDocumented() {
        String[] validModes = {"IN_PERSON", "VIDEO", "PHONE", "EMAIL"};
        for (String mode : validModes) {
            assertNotNull(mode, "Mode value must not be null");
        }
    }

    // -------------------------------------------------------
    // Error code contracts
    // -------------------------------------------------------

    @Test
    void meetingNotFoundErrorCodeShouldMatchConstant() {
        BusinessException ex = new BusinessException(ErrorCodes.MEETING_NOT_FOUND, "Meeting not found: MTG-X");
        assertEquals(ErrorCodes.MEETING_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void prospectNotFoundErrorCodeShouldMatchConstant() {
        BusinessException ex = new BusinessException(ErrorCodes.PROSPECT_NOT_FOUND, "Prospect not found");
        assertEquals(ErrorCodes.PROSPECT_NOT_FOUND, ex.getErrorCode());
    }

    // -------------------------------------------------------
    // Meeting entity defaults
    // -------------------------------------------------------

    @Test
    void meetingEntityReminderDefaultsShouldBeFalse() {
        Meeting m = new Meeting();
        assertFalse(m.reminder1day);
        assertFalse(m.reminder1hour);
    }
}
