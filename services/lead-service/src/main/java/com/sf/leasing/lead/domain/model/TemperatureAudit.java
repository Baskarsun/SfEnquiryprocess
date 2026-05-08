package com.sf.leasing.lead.domain.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "temperature_audit")
public class TemperatureAudit extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", nullable = false)
    public Lead lead;

    @Column(name = "previous_temperature", length = 10)
    public String previousTemperature;

    @Column(name = "new_temperature", nullable = false, length = 10)
    public String newTemperature;

    @Column(name = "reason_code", length = 50)
    public String reasonCode;

    @Column(name = "reason_text", length = 500)
    public String reasonText;

    @Column(name = "suggested_by", nullable = false, length = 10)
    public String suggestedBy = "SYSTEM";

    @Column(name = "changed_by", length = 50)
    public String changedBy;

    @Column(name = "changed_at", nullable = false)
    public LocalDateTime changedAt = LocalDateTime.now();

    public static List<TemperatureAudit> findByLeadId(UUID leadId) {
        return find("lead.id", leadId).list();
    }
}
