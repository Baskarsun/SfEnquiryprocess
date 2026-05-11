package com.sf.leasing.lead.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bulk_upload_jobs")
public class BulkUploadJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "job_type", nullable = false, length = 20)
    public String jobType;  // EXCEL | API

    @Column(name = "total_rows")
    public int totalRows = 0;

    @Column(name = "processed_rows")
    public int processedRows = 0;

    @Column(name = "success_rows")
    public int successRows = 0;

    @Column(name = "failed_rows")
    public int failedRows = 0;

    @Column(name = "status", length = 20)
    public String status = "PENDING";  // PENDING | IN_PROGRESS | COMPLETED | FAILED

    @Column(name = "created_by", nullable = false, length = 50)
    public String createdBy;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "completed_at")
    public LocalDateTime completedAt;
}
