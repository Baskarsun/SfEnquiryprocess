package com.sf.leasing.lead.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "exception_queue")
public class ExceptionQueueRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "source_type", nullable = false, length = 30)
    public String sourceType;  // MANUAL | BULK | API

    @Column(name = "source_identifier", length = 200)
    public String sourceIdentifier;

    @Column(name = "reason_code", nullable = false, length = 50)
    public String reasonCode;

    @Column(name = "reason_description", nullable = false, columnDefinition = "TEXT")
    public String reasonDescription;

    @Column(name = "fields_in_error", columnDefinition = "TEXT")
    public String fieldsInError;

    @Column(name = "raw_data", columnDefinition = "JSONB")
    public String rawData;

    @Column(name = "status", length = 20)
    public String status = "OPEN";  // OPEN | RESOLVED | ESCALATED

    @Column(name = "owned_by", length = 50)
    public String ownedBy = "CPU";

    @Column(name = "cure_sla_deadline")
    public LocalDateTime cureSlaDeadline;

    @Column(name = "resolved_by", length = 50)
    public String resolvedBy;

    @Column(name = "resolved_at")
    public LocalDateTime resolvedAt;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    public String resolutionNotes;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();
}
