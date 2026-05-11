package com.sf.leasing.lead.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "prospect_assignments")
public class ProspectAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prospect_id", nullable = false)
    public Prospect prospect;

    @Column(name = "assigned_to_user_id", nullable = false, length = 50)
    public String assignedToUserId;

    @Column(name = "assigned_to_team", length = 50)
    public String assignedToTeam;

    @Column(name = "assigned_to_branch_code", length = 20)
    public String assignedToBranchCode;

    @Column(name = "hierarchy_level", nullable = false, length = 20)
    public String hierarchyLevel;

    @Column(name = "previous_user_id", length = 50)
    public String previousUserId;

    @Column(name = "previous_team", length = 50)
    public String previousTeam;

    @Column(name = "reason_code", nullable = false, length = 50)
    public String reasonCode;

    @Column(name = "remarks", nullable = false)
    public String remarks;

    @Column(name = "assigned_by", nullable = false, length = 50)
    public String assignedBy;

    @Column(name = "assigned_at", nullable = false)
    public LocalDateTime assignedAt = LocalDateTime.now();

    @Column(name = "sla_deadline")
    public LocalDateTime slaDeadline;

    @Column(name = "sla_breached")
    public boolean slaBreached = false;


}
