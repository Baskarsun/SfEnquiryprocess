package com.sf.leasing.lead.service;

import com.sf.leasing.lead.api.dto.request.LogMeetingRequest;
import com.sf.leasing.lead.domain.exception.BusinessException;
import com.sf.leasing.lead.domain.exception.ErrorCodes;
import com.sf.leasing.lead.domain.model.Meeting;
import com.sf.leasing.lead.domain.model.Prospect;
import com.sf.leasing.lead.infrastructure.locking.RedisSequenceGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Implements PP4: Meeting & Interaction Management.
 *
 * Meetings are immutable once logged. Amendments create a new Meeting record
 * with the original_meeting_id reference set — the original is never modified.
 * Each meeting receives a system-generated MTG-YYYY-NNNNN identifier.
 */
@ApplicationScoped
public class MeetingService {

    private static final Logger LOG = Logger.getLogger(MeetingService.class);

    @Inject
    RedisSequenceGenerator sequenceGenerator;

    @Transactional
    public Meeting logMeeting(UUID prospectId, LogMeetingRequest req, String createdBy) {
        Prospect prospect = Prospect.findById(prospectId);
        if (prospect == null) {
            throw new BusinessException(ErrorCodes.PROSPECT_NOT_FOUND, "Prospect not found: " + prospectId);
        }
        if (prospect.isClosed()) {
            throw new BusinessException(ErrorCodes.PROSPECT_ALREADY_CLOSED,
                "Cannot log a meeting for a closed prospect.");
        }

        String meetingId = sequenceGenerator.generateMeetingId();

        Meeting meeting = new Meeting();
        meeting.meetingId        = meetingId;
        meeting.prospect         = prospect;
        meeting.meetingDatetime  = req.meetingDatetime;
        meeting.attendees        = req.attendees;
        meeting.notes            = req.notes;
        meeting.mode             = req.mode;
        meeting.reminder1day     = req.reminder1day;
        meeting.reminder1hour    = req.reminder1hour;
        meeting.originalMeetingId = req.originalMeetingId;  // null for original, set for amendments
        meeting.createdBy        = createdBy;
        meeting.createdAt        = LocalDateTime.now();
        meeting.persist();

        LOG.infof("Meeting logged: %s for Prospect=%s by=%s", meetingId, prospectId, createdBy);
        return meeting;
    }

    /**
     * Amendment: creates a new Meeting record referencing the original, leaving the original untouched.
     */
    @Transactional
    public Meeting amendMeeting(UUID prospectId, String originalMeetingId, LogMeetingRequest req, String amendedBy) {
        Meeting original = Meeting.find("meetingId", originalMeetingId).firstResult();
        if (original == null) {
            throw new BusinessException(ErrorCodes.MEETING_NOT_FOUND, "Meeting not found: " + originalMeetingId);
        }

        req.originalMeetingId = originalMeetingId;
        return logMeeting(prospectId, req, amendedBy);
    }

    public List<Meeting> getMeetings(UUID prospectId) {
        Prospect prospect = Prospect.findById(prospectId);
        if (prospect == null) {
            throw new BusinessException(ErrorCodes.PROSPECT_NOT_FOUND, "Prospect not found: " + prospectId);
        }
        return Meeting.findByProspectId(prospectId);
    }
}
