package com.sf.leasing.lead.domain.model;

import com.sf.leasing.lead.domain.enums.CamStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cam_workflows")
public class CamWorkflow extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "application_id", nullable = false, unique = true)
    public UUID applicationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "cam_status", nullable = false, length = 30)
    public CamStatus camStatus = CamStatus.NOT_STARTED;

    @Column(name = "sanction_id", length = 100)
    public String sanctionId;

    @Column(name = "sanction_package", length = 200)
    public String sanctionPackage;

    @Column(name = "initiated_at")
    public LocalDateTime initiatedAt;

    @Column(name = "initiated_by", length = 50)
    public String initiatedBy;

    @Column(name = "approved_at")
    public LocalDateTime approvedAt;

    @Column(name = "approved_by", length = 50)
    public String approvedBy;

    @Column(name = "declined_at")
    public LocalDateTime declinedAt;

    @Column(name = "declined_reason", length = 500)
    public String declinedReason;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    public static CamWorkflow findByApplication(UUID applicationId) {
        return find("applicationId", applicationId).firstResult();
    }
}
