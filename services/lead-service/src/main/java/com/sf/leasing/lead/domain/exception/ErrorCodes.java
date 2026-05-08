package com.sf.leasing.lead.domain.exception;

public final class ErrorCodes {

    private ErrorCodes() {}

    // Authentication (LP1)
    public static final String INVALID_USER              = "GL461";

    // Number generation (LP2)
    public static final String DOCUMENT_NUMBER_IN_USE   = "D283";

    // Input validation (LP2)
    public static final String LEAD_INPUT_REQUIRED       = "LEAD_INPUT_REQUIRED";
    public static final String CONTRACT_TYPE_REQUIRED    = "CONTRACT_TYPE_REQUIRED";
    public static final String MIN_IDENTIFIER_MISSING    = "MIN_IDENTIFIER_MISSING";

    // DAN / PAN exemption (LP2)
    public static final String INVALID_DAN               = "LN4468";
    public static final String DAN_ALREADY_USED          = "LN4470";

    // Geography (LP2, PP3)
    public static final String GEOGRAPHIC_VALIDATION     = "LN3955";

    // KYC format (LP4)
    public static final String INVALID_AADHAAR           = "AADHAAR_INVALID";
    public static final String PAN_CAUTION_LIST          = "LN5337";

    // Deduplication (LP4)
    public static final String CONFIRMED_DUPLICATE       = "COM110";
    public static final String POTENTIAL_DUPLICATE       = "COM111";
    public static final String SOFT_DEDUP_WARNING        = "COM66";

    // Data consistency (LP4)
    public static final String GENDER_OCCUPATION_MISMATCH = "COM122";
    public static final String PAN_MANDATORY             = "COM129";

    // Eligibility (LP8)
    public static final String CUSTOMER_DECEASED         = "LN3574";
    public static final String ACTIVE_LEASE_BLOCK        = "LN4323";

    // Applicant record (LP8)
    public static final String LOCATION_NAME_NULL        = "LN5078";
    public static final String ADDRESS_TOO_SHORT         = "LN5226";
    public static final String ADDRESS_TOO_LONG          = "LN5227";

    // Asset (LP8, PP6)
    public static final String ASSET_COST_ZERO           = "LN3574_ASSET";
    public static final String ASSET_COST_LESS_THAN_LEASE = "ASSET_COST_LEASE";

    // Branch (PP3)
    public static final String INVALID_BRANCH            = "LN480";

    // Aged pending (LP2)
    public static final String AGED_PENDING_BLOCK        = "LN4924";

    // Modification blocks (PP7)
    public static final String MODIFICATION_CONTRACT_IN_PROGRESS = "LN3713";
    public static final String MODIFICATION_FRAUD_INVESTIGATION  = "LN3785";

    // Phase 3 — Prospect Promotion (LP8 / PP1)
    public static final String LEAD_NOT_FOUND              = "LEAD_NOT_FOUND";
    public static final String LEAD_NOT_PROMOTABLE         = "LEAD_NOT_PROMOTABLE";
    public static final String LEAD_ALREADY_PROMOTED       = "LEAD_ALREADY_PROMOTED";
    public static final String PROSPECT_NOT_FOUND          = "PROSPECT_NOT_FOUND";
    public static final String PROSPECT_ALREADY_CLOSED     = "PROSPECT_ALREADY_CLOSED";

    // Phase 3 — KYC Validation (PP3)
    public static final String INVALID_VALIDATION_TYPE     = "PP3_INVALID_TYPE";
    public static final String KYC_VALIDATION_FAILED       = "PP3_KYC_FAILED";
    public static final String PAN_NOT_SET                 = "PP3_PAN_NOT_SET";
    public static final String GSTIN_NOT_SET               = "PP3_GSTIN_NOT_SET";
    public static final String PROSPECT_NOT_DRAFT          = "PP3_NOT_DRAFT";
    public static final String PROSPECT_ID_ALREADY_SET     = "PP3_ID_ALREADY_SET";

    // Phase 3 — Meeting (PP4)
    public static final String MEETING_NOT_FOUND           = "MEETING_NOT_FOUND";
    public static final String MEETING_NOTES_REQUIRED      = "MEETING_NOTES_REQUIRED";
    public static final String MEETING_DATETIME_REQUIRED   = "MEETING_DATETIME_REQUIRED";

    // Phase 4 — Prospect state (PP5 pre-condition)
    public static final String PROSPECT_NOT_ACTIVE         = "PP5_PROSPECT_NOT_ACTIVE";

    // Phase 4 — Opportunity (PP5)
    public static final String OPPORTUNITY_NOT_FOUND       = "OPPORTUNITY_NOT_FOUND";
    public static final String OPPORTUNITY_LOB_DUPLICATE   = "PP5_LOB_DUPLICATE";
    public static final String OPPORTUNITY_CATEGORY_DUPLICATE = "PP5_CATEGORY_DUPLICATE";

    // Phase 4 — Quote (PP6)
    public static final String QUOTE_NOT_FOUND             = "QUOTE_NOT_FOUND";
    public static final String QUOTE_NOT_PENDING_APPROVAL  = "PP6_NOT_PENDING_APPROVAL";
    public static final String QUOTE_REQUIRES_APPROVAL     = "PP6_REQUIRES_APPROVAL";
    public static final String QUOTE_INVALID_TRANSITION    = "PP6_INVALID_TRANSITION";
    public static final String QUOTE_APPRAISAL_REQUIRED    = "PP6_APPRAISAL_REQUIRED";
    public static final String QUOTE_NOT_LOCKED            = "PP6_NOT_LOCKED";
    public static final String QUOTE_UNLOCK_NOT_REQUESTED  = "PP6_UNLOCK_NOT_REQUESTED";
    public static final String QUOTE_INVALID_LOCK_ACTION   = "PP6_INVALID_LOCK_ACTION";
    public static final String QUOTE_INVALID_DECISION      = "PP6_INVALID_DECISION";
    public static final String QUOTE_NOT_LOCKED_FOR_APPLICATION = "PP6_NOT_LOCKED_APP";

    // Phase 4 — Application (PP7)
    public static final String APPLICATION_NOT_FOUND       = "APPLICATION_NOT_FOUND";
    public static final String APPLICATION_ALREADY_EXISTS  = "PP7_APP_ALREADY_EXISTS";
    public static final String DOCUMENT_TYPE_INVALID       = "PP7_DOC_TYPE_INVALID";
    public static final String APPLICANT_PREFIX_INVALID    = "PP7_PREFIX_INVALID";
    public static final String DOCUMENT_CONTENT_INVALID    = "PP7_DOC_CONTENT_INVALID";

    // Phase 4 — CAM (PP7.8)
    public static final String CAM_ALREADY_INITIATED       = "PP7_CAM_ALREADY_INITIATED";
    public static final String CAM_NOT_IN_PROGRESS         = "PP7_CAM_NOT_IN_PROGRESS";
    public static final String CAM_NOT_FOUND               = "PP7_CAM_NOT_FOUND";
    public static final String CAM_INVALID_DECISION        = "PP7_CAM_INVALID_DECISION";

    // Phase 5 — Customer Creation Gate (PP8.1)
    public static final String CUSTOMER_GATE_KYC_INCOMPLETE  = "PP8_KYC_INCOMPLETE";
    public static final String CUSTOMER_GATE_CAM_NOT_APPROVED = "PP8_CAM_NOT_APPROVED";
    public static final String CUSTOMER_ALREADY_EXISTS       = "PP8_CUSTOMER_EXISTS";
    public static final String CUSTOMER_NOT_FOUND            = "CUSTOMER_NOT_FOUND";

    // Phase 5 — Role Assignment (PP8.3)
    public static final String ROLE_TYPE_INVALID             = "PP8_ROLE_INVALID";
    public static final String ROLE_CHECKLIST_INCOMPLETE     = "PP8_CHECKLIST_INCOMPLETE";
    public static final String ROLE_ALREADY_ASSIGNED         = "PP8_ROLE_DUPLICATE";

    // Phase 5 — Lineage (PP8.2)
    public static final String LINEAGE_NOT_FOUND             = "PP8_LINEAGE_NOT_FOUND";
}
