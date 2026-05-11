package com.sf.leasing.lead.domain.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Customer role assignment record.
 * PP8.3: Roles: LESSEE_INDIVIDUAL | LESSEE_CORPORATE | DEALER | VENDOR | DEPOSITOR.
 * Checklist must be complete before role activation.
 */
@Entity
@Table(name = "customer_role_assignments")
public class CustomerRoleAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "customer_uuid", nullable = false)
    public UUID customerUuid;

    @Column(name = "customer_id", length = 30)
    public String customerId;

    @Column(name = "role_type", nullable = false, length = 50)
    public String roleType;

    @Column(name = "checklist_complete", nullable = false)
    public boolean checklistComplete = false;

    @Column(name = "checklist_items")
    public String checklistItems;          // JSON array of completed checklist item codes

    @Column(name = "status", nullable = false, length = 20)
    public String status = "PENDING";      // PENDING | ACTIVE | INACTIVE

    @Column(name = "remarks", length = 500)
    public String remarks;

    // Audit
    @Column(name = "assigned_by", nullable = false, length = 50)
    public String assignedBy;

    @Column(name = "assigned_at", nullable = false)
    public LocalDateTime assignedAt = LocalDateTime.now();

    @Column(name = "updated_by", length = 50)
    public String updatedBy;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;
}
