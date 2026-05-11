package com.sf.leasing.lead.domain.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Append-only log of downstream Kafka events published from the customer creation gate.
 * Used for observability and replay capability.
 */
@Entity
@Table(name = "downstream_event_log")
public class DownstreamEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "event_type", nullable = false, length = 100)
    public String eventType;

    @Column(name = "entity_type", nullable = false, length = 50)
    public String entityType;          // CUSTOMER | OPPORTUNITY | APPLICATION

    @Column(name = "entity_id", nullable = false, length = 50)
    public String entityId;

    @Column(name = "topic", length = 200)
    public String topic;

    @Column(name = "payload")
    public String payload;

    @Column(name = "status", nullable = false, length = 20)
    public String status = "PUBLISHED"; // PUBLISHED | FAILED | RETRY

    @Column(name = "error_message")
    public String errorMessage;

    @Column(name = "published_at", nullable = false)
    public LocalDateTime publishedAt = LocalDateTime.now();
}
