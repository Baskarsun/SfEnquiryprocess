-- ============================================================
-- Phase 4: Opportunity, Quote & Application Origination
-- ============================================================

-- Asset taxonomy master (read-only reference; seeded in V6 reference data)
CREATE TABLE asset_taxonomy (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    category          VARCHAR(100) NOT NULL,
    asset_class       VARCHAR(100) NOT NULL,
    make              VARCHAR(100),
    model             VARCHAR(200),
    is_active         BOOLEAN      DEFAULT TRUE,
    UNIQUE (category, asset_class, make, model)
);

-- Quote master (rack-rate slabs by taxonomy + lease type)
CREATE TABLE quote_master (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_category    VARCHAR(100) NOT NULL,
    asset_class       VARCHAR(100) NOT NULL,
    lease_type        VARCHAR(50)  NOT NULL,
    min_cost          DECIMAL(15,2),
    max_cost          DECIMAL(15,2),
    rate_percent      DECIMAL(6,4) NOT NULL,
    effective_from    DATE         NOT NULL,
    effective_to      DATE,
    is_active         BOOLEAN      DEFAULT TRUE
);

-- Document checklist configuration (stage + asset_class + lease_type gated)
CREATE TABLE document_checklist_config (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_class       VARCHAR(100),
    lease_type        VARCHAR(50),
    applicant_type    VARCHAR(20)  NOT NULL,   -- INDIVIDUAL | COMMERCIAL
    document_type     VARCHAR(50)  NOT NULL,
    is_mandatory      BOOLEAN      DEFAULT TRUE,
    stage             VARCHAR(50)  NOT NULL DEFAULT 'KYC',
    is_active         BOOLEAN      DEFAULT TRUE,
    UNIQUE (asset_class, lease_type, applicant_type, document_type, stage)
);

-- Opportunities (one per LoB per Prospect)
CREATE TABLE opportunities (
    id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    opportunity_id        VARCHAR(30)  UNIQUE NOT NULL,   -- OPP-YYYY-NNNNNN
    prospect_uuid         UUID         NOT NULL REFERENCES prospects(id),
    prospect_business_id  VARCHAR(30),
    asset_category        VARCHAR(100) NOT NULL,
    asset_class           VARCHAR(100) NOT NULL,
    lob_tag               VARCHAR(50)  NOT NULL,
    status                VARCHAR(30)  NOT NULL DEFAULT 'OPEN',
    -- Audit
    created_by            VARCHAR(50)  NOT NULL,
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by            VARCHAR(50),
    updated_at            TIMESTAMP
);

-- Quotes (version-tracked per Opportunity)
CREATE TABLE quotes (
    id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    quote_id              VARCHAR(30)  UNIQUE NOT NULL,   -- QT-YYYY-NNNNNN
    opportunity_id        UUID         NOT NULL REFERENCES opportunities(id),
    opportunity_business_id VARCHAR(30),
    quote_type            VARCHAR(20)  NOT NULL,          -- RACK_RATE | CUSTOMISED
    version               INTEGER      NOT NULL DEFAULT 1,
    status                VARCHAR(30)  NOT NULL DEFAULT 'DRAFT',
    -- Asset & pricing inputs
    asset_cost            DECIMAL(15,2),
    product_model_price   DECIMAL(15,2),
    finance_amount        DECIMAL(15,2),
    lease_type            VARCHAR(50),
    asset_make            VARCHAR(100),
    asset_model           VARCHAR(200),
    asset_year            INTEGER,
    -- Rack rate / deviation
    rack_rate_percent     DECIMAL(6,4),
    pricing_deviation     BOOLEAN      DEFAULT FALSE,
    advisory_message      VARCHAR(500),
    -- Approval
    approval_required     BOOLEAN      DEFAULT FALSE,
    approved_by           VARCHAR(50),
    approved_at           TIMESTAMP,
    approval_remarks      VARCHAR(500),
    rejection_reason      VARCHAR(500),
    -- Sharing & lock
    shared_at             TIMESTAMP,
    shared_by             VARCHAR(50),
    locked_at             TIMESTAMP,
    locked_by             VARCHAR(50),
    -- Unlock
    unlock_requested_by   VARCHAR(50),
    unlock_requested_at   TIMESTAMP,
    unlock_approved_by    VARCHAR(50),
    unlock_approved_at    TIMESTAMP,
    -- Appraisal
    appraisal_category    VARCHAR(100),
    -- Audit
    created_by            VARCHAR(50)  NOT NULL,
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by            VARCHAR(50),
    updated_at            TIMESTAMP
);

-- Applications (one per Quote)
CREATE TABLE applications (
    id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id        VARCHAR(30)  UNIQUE NOT NULL,   -- APP-YYYY-NNNNNN
    quote_id              UUID         NOT NULL REFERENCES quotes(id),
    opportunity_id        UUID         NOT NULL REFERENCES opportunities(id),
    prospect_uuid         UUID         NOT NULL REFERENCES prospects(id),
    prospect_business_id  VARCHAR(30),
    is_individual         BOOLEAN      NOT NULL DEFAULT TRUE,
    lease_type            VARCHAR(50),
    -- Status machine
    status                VARCHAR(50)  NOT NULL DEFAULT 'INITIATED',
    kyc_status            VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    cam_status            VARCHAR(30)  NOT NULL DEFAULT 'NOT_STARTED',
    sanction_id           VARCHAR(100),
    sanction_package      VARCHAR(200),
    -- Fraud screening
    fraud_status          VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    fraud_message         TEXT,
    fraud_screened_at     TIMESTAMP,
    -- CIBIL
    cibil_submitted       BOOLEAN      DEFAULT FALSE,
    cibil_reference       VARCHAR(100),
    cibil_requested_at    TIMESTAMP,
    -- Caution screening
    caution_screened      BOOLEAN      DEFAULT FALSE,
    caution_blocked       BOOLEAN      DEFAULT FALSE,
    caution_screened_at   TIMESTAMP,
    -- Eligibility flags
    eligible_for_application BOOLEAN   DEFAULT FALSE,
    sanction_bypass       BOOLEAN      DEFAULT FALSE,    -- existing customer within validity
    welcome_comm_sent     BOOLEAN      DEFAULT FALSE,
    -- Modification controls
    modification_blocked  BOOLEAN      DEFAULT FALSE,
    modification_block_reason VARCHAR(10),              -- LN3713 | LN3785
    -- Audit
    created_by            VARCHAR(50)  NOT NULL,
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by            VARCHAR(50),
    updated_at            TIMESTAMP
);

-- Application documents (KYC upload records)
CREATE TABLE application_documents (
    id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id        UUID         NOT NULL REFERENCES applications(id),
    document_type         VARCHAR(50)  NOT NULL,   -- PHOTO|DRIVING_LICENCE|PAN|PASSPORT|OTHER_KYC
    applicant_prefix      VARCHAR(10)  NOT NULL,   -- MA | A1 | A2
    document_name         VARCHAR(200),
    dms_document_index    VARCHAR(100),
    dms_environment       VARCHAR(5),              -- T | B | L
    upload_status         VARCHAR(30)  NOT NULL DEFAULT 'UPLOADED',
    uploaded_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    uploaded_by           VARCHAR(50)  NOT NULL
);

-- CAM workflow tracking (parallel to KYC)
CREATE TABLE cam_workflows (
    id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id        UUID         NOT NULL UNIQUE REFERENCES applications(id),
    cam_status            VARCHAR(30)  NOT NULL DEFAULT 'NOT_STARTED',
    sanction_id           VARCHAR(100),
    sanction_package      VARCHAR(200),
    initiated_at          TIMESTAMP,
    initiated_by          VARCHAR(50),
    approved_at           TIMESTAMP,
    approved_by           VARCHAR(50),
    declined_at           TIMESTAMP,
    declined_reason       VARCHAR(500),
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP
);

-- -------------------------------------------------------
-- Indexes
-- -------------------------------------------------------
CREATE INDEX idx_opportunities_prospect    ON opportunities(prospect_uuid);
CREATE INDEX idx_opportunities_opp_id      ON opportunities(opportunity_id);
CREATE INDEX idx_opportunities_lob         ON opportunities(prospect_uuid, lob_tag);
CREATE INDEX idx_quotes_opportunity        ON quotes(opportunity_id);
CREATE INDEX idx_quotes_status             ON quotes(status);
CREATE INDEX idx_applications_prospect     ON applications(prospect_uuid);
CREATE INDEX idx_applications_quote        ON applications(quote_id);
CREATE INDEX idx_applications_status       ON applications(status);
CREATE INDEX idx_app_docs_application      ON application_documents(application_id);
CREATE INDEX idx_cam_application           ON cam_workflows(application_id);

-- -------------------------------------------------------
-- Phase 4 sequence seeds
-- -------------------------------------------------------
INSERT INTO sequence_control (sequence_name, current_value, reset_year, reset_month)
VALUES
    ('OPPORTUNITY_ID', 0, EXTRACT(YEAR FROM CURRENT_DATE)::SMALLINT, NULL),
    ('QUOTE_ID',       0, EXTRACT(YEAR FROM CURRENT_DATE)::SMALLINT, NULL),
    ('APPLICATION_ID', 0, EXTRACT(YEAR FROM CURRENT_DATE)::SMALLINT, NULL)
ON CONFLICT (sequence_name) DO NOTHING;

-- -------------------------------------------------------
-- Seed document checklist defaults
-- -------------------------------------------------------
INSERT INTO document_checklist_config
    (asset_class, lease_type, applicant_type, document_type, is_mandatory, stage)
VALUES
    (NULL, NULL, 'INDIVIDUAL',  'PHOTO',            TRUE,  'KYC'),
    (NULL, NULL, 'INDIVIDUAL',  'PAN',              TRUE,  'KYC'),
    (NULL, NULL, 'INDIVIDUAL',  'DRIVING_LICENCE',  FALSE, 'KYC'),
    (NULL, NULL, 'INDIVIDUAL',  'PASSPORT',         FALSE, 'KYC'),
    (NULL, NULL, 'INDIVIDUAL',  'OTHER_KYC',        FALSE, 'KYC'),
    (NULL, NULL, 'COMMERCIAL',  'PAN',              TRUE,  'KYC'),
    (NULL, NULL, 'COMMERCIAL',  'OTHER_KYC',        TRUE,  'KYC')
ON CONFLICT DO NOTHING;
