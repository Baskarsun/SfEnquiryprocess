-- ============================================================
-- Phase 3: Prospect Creation & External Validation — Schema
-- ============================================================

-- Core prospect record
CREATE TABLE prospects (
    id                        UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    prospect_id               VARCHAR(30),              -- PR-YYYY-NNNNNN (set after validation)
    lead_lrn                  VARCHAR(30)  NOT NULL,
    lead_id                   UUID         NOT NULL REFERENCES leads(id),
    lead_type                 VARCHAR(20)  NOT NULL,    -- INDIVIDUAL | COMMERCIAL
    status                    VARCHAR(30)  NOT NULL DEFAULT 'DRAFT',
    -- KYC identifiers (copied from lead applicant at promotion time)
    pan                       VARCHAR(12),
    gstin                     VARCHAR(20),
    pan_validated             BOOLEAN      DEFAULT FALSE,
    gstin_validated           BOOLEAN      DEFAULT FALSE,
    -- Validated legal identity (populated from external validation)
    legal_name                VARCHAR(500),
    registered_address        TEXT,
    registered_pincode        VARCHAR(10),
    validation_reference      VARCHAR(100),
    validation_source         VARCHAR(50),
    validation_timestamp      TIMESTAMP,
    -- Manual validation override
    validation_override_reason VARCHAR(500),
    validation_override_by    VARCHAR(50),
    validation_override_at    TIMESTAMP,
    -- UCIC / dedup
    ucic                      VARCHAR(50),
    existing_customer_codes   TEXT,
    dedup_label               VARCHAR(30),
    -- Assignment
    assigned_branch_code      VARCHAR(20),
    assigned_team             VARCHAR(50),
    assigned_user_id          VARCHAR(50),
    assignment_hierarchy_level VARCHAR(20),
    -- Closure
    closure_reason_code       VARCHAR(50),
    closure_reason_text       VARCHAR(500),
    closed_at                 TIMESTAMP,
    closed_by                 VARCHAR(50),
    -- Audit
    created_by                VARCHAR(50)  NOT NULL,
    created_at                TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by                VARCHAR(50),
    updated_at                TIMESTAMP
);

-- Append-only prospect assignment audit
CREATE TABLE prospect_assignments (
    id                        UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    prospect_id               UUID        NOT NULL REFERENCES prospects(id),
    assigned_to_user_id       VARCHAR(50) NOT NULL,
    assigned_to_team          VARCHAR(50),
    assigned_to_branch_code   VARCHAR(20),
    hierarchy_level           VARCHAR(20) NOT NULL,
    previous_user_id          VARCHAR(50),
    previous_team             VARCHAR(50),
    reason_code               VARCHAR(50) NOT NULL,
    remarks                   TEXT        NOT NULL,
    assigned_by               VARCHAR(50) NOT NULL,
    assigned_at               TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sla_deadline              TIMESTAMP,
    sla_breached              BOOLEAN     DEFAULT FALSE
);

-- Append-only meeting log
CREATE TABLE meetings (
    id                        UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    meeting_id                VARCHAR(25) UNIQUE NOT NULL,  -- MTG-YYYY-NNNNN
    prospect_id               UUID        NOT NULL REFERENCES prospects(id),
    meeting_datetime          TIMESTAMP   NOT NULL,
    attendees                 TEXT,
    notes                     TEXT        NOT NULL,
    mode                      VARCHAR(30),             -- IN_PERSON | VIDEO | PHONE | EMAIL
    outcome                   VARCHAR(100),
    reminder_1day             BOOLEAN     DEFAULT FALSE,
    reminder_1hour            BOOLEAN     DEFAULT FALSE,
    original_meeting_id       VARCHAR(25),             -- populated when this is an amendment record
    created_by                VARCHAR(50) NOT NULL,
    created_at                TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Per-validation-run KYC validation history
CREATE TABLE prospect_kyc_validations (
    id                        UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    prospect_id               UUID        NOT NULL REFERENCES prospects(id),
    validation_type           VARCHAR(10) NOT NULL,    -- PAN | GSTIN
    identifier                VARCHAR(20) NOT NULL,
    status                    VARCHAR(20) NOT NULL,    -- SUCCESS | FAILED | OVERRIDDEN
    legal_name                VARCHAR(500),
    registered_address        TEXT,
    derived_pan               VARCHAR(12),             -- for GSTIN validation (chars 3-12)
    validation_reference      VARCHAR(100),
    validation_source         VARCHAR(50),
    validated_at              TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    override_reason_code      VARCHAR(50),
    override_reason_text      VARCHAR(500),
    override_by               VARCHAR(50),
    override_at               TIMESTAMP
);

-- Immutable lineage chain (Lead → Prospect; extended in Phase 5)
CREATE TABLE lineage (
    id                        UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_lrn                  VARCHAR(30) NOT NULL,
    lead_id                   UUID        NOT NULL REFERENCES leads(id),
    prospect_uuid             UUID        REFERENCES prospects(id),
    prospect_business_id      VARCHAR(30),             -- PR-YYYY-NNNNNN (set after validation)
    created_at                TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                TIMESTAMP
);

-- -------------------------------------------------------
-- Indexes
-- -------------------------------------------------------
CREATE INDEX idx_prospects_status           ON prospects(status);
CREATE INDEX idx_prospects_lead_lrn         ON prospects(lead_lrn);
CREATE INDEX idx_prospects_lead_id          ON prospects(lead_id);
CREATE INDEX idx_prospects_prospect_id      ON prospects(prospect_id);
CREATE INDEX idx_prospects_assigned_user    ON prospects(assigned_user_id);
CREATE INDEX idx_prospects_assigned_branch  ON prospects(assigned_branch_code);
CREATE INDEX idx_prospect_assign_pid        ON prospect_assignments(prospect_id);
CREATE INDEX idx_meetings_prospect_id       ON meetings(prospect_id);
CREATE INDEX idx_meetings_datetime          ON meetings(meeting_datetime);
CREATE INDEX idx_kyc_val_prospect_id        ON prospect_kyc_validations(prospect_id);
CREATE INDEX idx_lineage_lead_lrn           ON lineage(lead_lrn);
CREATE INDEX idx_lineage_prospect_uuid      ON lineage(prospect_uuid);

-- -------------------------------------------------------
-- Phase 3 sequence seeds
-- -------------------------------------------------------
INSERT INTO sequence_control (sequence_name, current_value, reset_year, reset_month)
VALUES
    ('PROSPECT_ID', 0, EXTRACT(YEAR FROM CURRENT_DATE)::SMALLINT, NULL),
    ('MEETING_ID',  0, EXTRACT(YEAR FROM CURRENT_DATE)::SMALLINT, NULL)
ON CONFLICT (sequence_name) DO NOTHING;
