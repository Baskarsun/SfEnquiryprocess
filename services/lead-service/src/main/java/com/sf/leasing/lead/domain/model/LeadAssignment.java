package com.sf.leasing.lead.domain.model;

import com.sf.leasing.lead.domain.enums.HierarchyLevel;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lead_assignments")
public class LeadAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", nullable = false)
    public Lead lead;

    @Column(name = "assigned_to_user_id", nullable = false, length = 50)
    public String assignedToUserId;

    @Column(name = "assigned_to_team", length = 50)
    public String assignedToTeam;

    @Column(name = "assigned_to_branch_code", length = 20)
    public String assignedToBranchCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "hierarchy_level", nullable = false, length = 20)
    public HierarchyLevel hierarchyLevel;

    @Column(name = "previous_user_id", length = 50)
    public String previousUserId;

    @Column(name = "previous_team", length = 50)
    public String previousTeam;

    @Column(name = "reason_code", nullable = false, length = 50)
    public String reasonCode;

    @Column(name = "remarks", nullable = false, columnDefinition = "TEXT")
    public String remarks;

    @Column(name = "assigned_by", nullable = false, length = 50)
    public String assignedBy;

    @Column(name = "assigned_at", nullable = false)
    public LocalDateTime assignedAt = LocalDateTime.now();

    @Column(name = "sla_deadline")
    public LocalDateTime slaDeadline;

    @Column(name = "sla_breached")
    public boolean slaBreached = false;

    @Column(name = "sla_breached_at")
    public LocalDateTime slaBreachedAt;
}
