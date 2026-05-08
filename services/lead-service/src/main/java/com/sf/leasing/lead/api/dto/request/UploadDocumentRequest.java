package com.sf.leasing.lead.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public class UploadDocumentRequest {

    /** PHOTO | DRIVING_LICENCE | PAN | PASSPORT | OTHER_KYC */
    @NotBlank(message = "Document type is mandatory (PP7.3)")
    public String documentType;

    /** MA | A1 | A2 */
    @NotBlank(message = "Applicant prefix is mandatory (PP7.3)")
    public String applicantPrefix;

    public String documentName;

    /** Base64-encoded document content */
    public String documentContent;

    public String uploadedBy;
}
