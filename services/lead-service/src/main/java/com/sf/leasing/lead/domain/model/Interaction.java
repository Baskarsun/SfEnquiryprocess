package com.sf.leasing.lead.domain.model;

import com.sf.leasing.lead.domain.enums.InteractionType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "interactions")
public class Interaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", nullable = false)
    public Lead lead;

    @Enumerated(EnumType.STRING)
    @Column(name = "interaction_type", nullable = false, length = 30)
    public InteractionType interactionType;

    @Column(name = "interaction_timestamp", nullable = false)
    public LocalDateTime interactionTimestamp;

    @Column(name = "outcome_notes", nullable = false, columnDefinition = "TEXT")
    public String outcomeNotes;

    @Column(name = "contact_person", length = 200)
    public String contactPerson;

    @Column(name = "contact_designation", length = 100)
    public String contactDesignation;

    @Column(name = "mode", length = 30)
    public String mode;

    @Column(name = "next_action_date")
    public LocalDateTime nextActionDate;

    @Column(name = "next_action_mode", length = 30)
    public String nextActionMode;

    @Column(name = "next_contact_person", length = 200)
    public String nextContactPerson;

    @Column(name = "reminder_flag")
    public boolean reminderFlag = false;

    @Column(name = "created_by", nullable = false, length = 50)
    public String createdBy;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();
}
