package com.sf.leasing.lead.api.dto.response;

import com.sf.leasing.lead.domain.model.Application;
import com.sf.leasing.lead.domain.model.ApplicationDocument;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ApplicationResponse {

    public UUID    id;
    public String  applicationId;
    public String  prospectBusinessId;
    public String  leaseType;
    public boolean individual;
    public String  status;
    public String  kycStatus;
    public String  camStatus;
    public String  sanctionId;
    public String  sanctionPackage;
    public String  fraudStatus;
    public boolean cibilSubmitted;
    public boolean eligibleForApplication;
    public boolean modificationBlocked;
    public String  modificationBlockReason;
    public String  createdBy;
    public LocalDateTime createdAt;
    public String  updatedBy;
    public LocalDateTime updatedAt;

    public List<DocumentSummary> documents;

    public static ApplicationResponse from(Application a) {
        ApplicationResponse r = new ApplicationResponse();
        r.id                     = a.id;
        r.applicationId          = a.applicationId;
        r.prospectBusinessId     = a.prospectBusinessId;
        r.leaseType              = a.leaseType;
        r.individual             = a.individual;
        r.status                 = a.status != null ? a.status.name() : null;
        r.kycStatus              = a.kycStatus;
        r.camStatus              = a.camStatus != null ? a.camStatus.name() : null;
        r.sanctionId             = a.sanctionId;
        r.sanctionPackage        = a.sanctionPackage;
        r.fraudStatus            = a.fraudStatus;
        r.cibilSubmitted         = a.cibilSubmitted;
        r.eligibleForApplication = a.eligibleForApplication;
        r.modificationBlocked    = a.modificationBlocked;
        r.modificationBlockReason = a.modificationBlockReason;
        r.createdBy              = a.createdBy;
        r.createdAt              = a.createdAt;
        r.updatedBy              = a.updatedBy;
        r.updatedAt              = a.updatedAt;
        r.documents = a.documents != null
            ? a.documents.stream().map(DocumentSummary::from).collect(Collectors.toList())
            : Collections.emptyList();
        return r;
    }

    public static class DocumentSummary {
        public UUID   id;
        public String documentType;
        public String applicantPrefix;
        public String documentName;
        public String dmsDocumentIndex;
        public String dmsEnvironment;
        public String uploadStatus;
        public LocalDateTime uploadedAt;

        public static DocumentSummary from(ApplicationDocument d) {
            DocumentSummary s = new DocumentSummary();
            s.id              = d.id;
            s.documentType    = d.documentType;
            s.applicantPrefix = d.applicantPrefix;
            s.documentName    = d.documentName;
            s.dmsDocumentIndex = d.dmsDocumentIndex;
            s.dmsEnvironment  = d.dmsEnvironment;
            s.uploadStatus    = d.uploadStatus;
            s.uploadedAt      = d.uploadedAt;
            return s;
        }
    }
}
