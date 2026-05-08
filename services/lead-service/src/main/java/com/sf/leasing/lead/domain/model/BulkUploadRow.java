package com.sf.leasing.lead.domain.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bulk_upload_rows")
public class BulkUploadRow extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    public BulkUploadJob job;

    @Column(name = "row_number", nullable = false)
    public int rowNumber;

    @Column(name = "lrn", length = 30)
    public String lrn;

    @Column(name = "status", nullable = false, length = 20)
    public String status;  // SUCCESS | FAILED

    @Column(name = "error_code", length = 50)
    public String errorCode;

    @Column(name = "error_message")
    public String errorMessage;

    @Column(name = "raw_data")
    public String rawData;

    @Column(name = "processed_at", nullable = false)
    public LocalDateTime processedAt = LocalDateTime.now();
}
