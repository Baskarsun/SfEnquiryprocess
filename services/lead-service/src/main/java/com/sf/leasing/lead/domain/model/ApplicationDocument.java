package com.sf.leasing.lead.domain.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "application_documents")
public class ApplicationDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "application_id", nullable = false)
    public UUID applicationId;

    // PHOTO | DRIVING_LICENCE | PAN | PASSPORT | OTHER_KYC
    @Column(name = "document_type", nullable = false, length = 50)
    public String documentType;

    // MA | A1 | A2
    @Column(name = "applicant_prefix", nullable = false, length = 10)
    public String applicantPrefix;

    @Column(name = "document_name", length = 200)
    public String documentName;

    @Column(name = "dms_document_index", length = 100)
    public String dmsDocumentIndex;

    // T | B | L
    @Column(name = "dms_environment", length = 5)
    public String dmsEnvironment;

    @Column(name = "upload_status", nullable = false, length = 30)
    public String uploadStatus = "UPLOADED";

    @Column(name = "uploaded_at", nullable = false)
    public LocalDateTime uploadedAt = LocalDateTime.now();

    @Column(name = "uploaded_by", nullable = false, length = 50)
    public String uploadedBy;


}
