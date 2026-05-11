package com.sf.leasing.lead.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "meetings")
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "meeting_id", unique = true, nullable = false, length = 25)
    public String meetingId;           // MTG-YYYY-NNNNN

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prospect_id", nullable = false)
    public Prospect prospect;

    @Column(name = "meeting_datetime", nullable = false)
    public LocalDateTime meetingDatetime;

    @Column(name = "attendees")
    public String attendees;

    @Column(name = "notes", nullable = false)
    public String notes;

    @Column(name = "mode", length = 30)
    public String mode;               // IN_PERSON | VIDEO | PHONE | EMAIL

    @Column(name = "outcome", length = 100)
    public String outcome;

    @Column(name = "reminder_1day")
    public boolean reminder1day = false;

    @Column(name = "reminder_1hour")
    public boolean reminder1hour = false;

    @Column(name = "original_meeting_id", length = 25)
    public String originalMeetingId;  // set when this record is an amendment

    @Column(name = "created_by", nullable = false, length = 50)
    public String createdBy;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();


}
