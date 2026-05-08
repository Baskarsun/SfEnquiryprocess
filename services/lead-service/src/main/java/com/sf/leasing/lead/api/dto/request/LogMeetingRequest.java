package com.sf.leasing.lead.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class LogMeetingRequest {

    @NotNull(message = "meetingDatetime is required.")
    public LocalDateTime meetingDatetime;

    public String attendees;

    @NotBlank(message = "notes are required.")
    public String notes;

    public String mode;               // IN_PERSON | VIDEO | PHONE | EMAIL

    public boolean reminder1day  = false;
    public boolean reminder1hour = false;

    public String originalMeetingId; // populated on amendment to reference the original meeting
}
