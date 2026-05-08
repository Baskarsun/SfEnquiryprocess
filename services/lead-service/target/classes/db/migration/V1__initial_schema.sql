-- ============================================================
-- Phase 1: Lead Management Service — Initial Schema
-- ============================================================

-- Sequence control table (Redis-backed lock uses this as persistent counter)
CREATE TABLE sequence_control (
    sequence_name   VARCHAR(100) PRIMARY KEY,
    current_value   BIGINT       NOT NULL DEFAULT 0,
    reset_year      SMALLINT,
    reset_month     SMALLINT,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Employee / User registry
CREATE TABLE employees (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id     VARCHAR(50)  UNIQUE NOT NULL,
    name            VARCHAR(200) NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE | INACTIVE | ON_LEAVE
    branch_code     VARCHAR(20),
    role            VARCHAR(50),
    email           VARCHAR(200),
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Device registry (primary IMEI / secondary UUID)
CREATE TABLE device_registry (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id     VARCHAR(50)  NOT NULL REFERENCES employees(employee_id),
    imei            VARCHAR(50),
    device_uuid     VARCHAR(100),
    device_type     VARCHAR(20),   -- MOBILE | TABLET
    is_authorised   BOOLEAN      DEFAULT TRUE,
    registered_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- DAN (Declaration Account Number) registry — PAN exemption
CREATE TABLE dan_registry (
    id                      UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    dan                     VARCHAR(50) UNIQUE NOT NULL,
    form_code               VARCHAR(20),
    exemption_unique_number VARCHAR(50),
    is_used                 BOOLEAN     DEFAULT FALSE,
    used_by_lead_id         UUID,
    created_at              TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Branch configuration (service area, segment, geo-coordinates)
CREATE TABLE branch_config (
    branch_code         VARCHAR(20)    PRIMARY KEY,
    branch_name         VARCHAR(200)   NOT NULL,
    segment_code        VARCHAR(10),           -- 'SM' = SME (geo-validation exempt)
    max_distance_km     DECIMAL(10,2),
    latitude            DECIMAL(10,8),
    longitude           DECIMAL(11,8),
    is_active           BOOLEAN        DEFAULT TRUE,
    business_open_date  DATE,
    accounting_open_date DATE,
    close_date          DATE
);

-- Pincode → Branch service area mapping
CREATE TABLE pincode_branch_mapping (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    pincode             VARCHAR(10) NOT NULL,
    branch_code         VARCHAR(20) NOT NULL REFERENCES branch_config(branch_code),
    is_explicit_override BOOLEAN    DEFAULT FALSE,
    UNIQUE (pincode, branch_code)
);

-- SLA configuration per hierarchy level
CREATE TABLE sla_config (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    hierarchy_level     VARCHAR(30) NOT NULL UNIQUE,
    threshold_hours     INTEGER     NOT NULL,
    escalation_target   VARCHAR(50) NOT NULL
);

-- -------------------------------------------------------
-- Core lead tables
-- -------------------------------------------------------

CREATE TABLE leads (
    id                          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    lrn                         VARCHAR(30)  UNIQUE NOT NULL,
    temp_customer_number        VARCHAR(25)  UNIQUE NOT NULL,
    lead_type                   VARCHAR(20)  NOT NULL,           -- INDIVIDUAL | COMMERCIAL
    source_category             VARCHAR(30)  NOT NULL,
    source_name                 VARCHAR(200) NOT NULL,
    company_known_as            VARCHAR(200),
    contact_person              VARCHAR(200),
    status                      VARCHAR(30)  NOT NULL DEFAULT 'NEW',
    temperature                 VARCHAR(10)  DEFAULT 'COLD',
    temperature_suggested_by    VARCHAR(10)  DEFAULT 'SYSTEM',
    temperature_reason_code     VARCHAR(50),
    temperature_reason_text     VARCHAR(500),
    dedup_label                 VARCHAR(30)  DEFAULT 'UNKNOWN',
    ucic                        VARCHAR(50),
    existing_customer_codes     TEXT,
    assigned_branch_code        VARCHAR(20),
    assigned_team               VARCHAR(50),
    assigned_user_id            VARCHAR(50),
    assignment_hierarchy_level  VARCHAR(20)  DEFAULT 'CENTRAL',
    call_attempts               INTEGER      DEFAULT 0,
    message_attempts            INTEGER      DEFAULT 0,
    email_attempts              INTEGER      DEFAULT 0,
    closure_reason_code         VARCHAR(50),
    closure_reason_text         VARCHAR(500),
    closure_date                TIMESTAMP,
    closure_operator            VARCHAR(50),
    latitude                    DECIMAL(10,8),
    longitude                   DECIMAL(11,8),
    geotag_captured_at          TIMESTAMP,
    channel                     VARCHAR(20)  NOT NULL,          -- MOBILE | DESKTOP | API | BULK
    sms_indicator               VARCHAR(1)   DEFAULT 'N',
    promoted_at                 TIMESTAMP,
    prospect_id                 VARCHAR(30),
    created_by                  VARCHAR(50)  NOT NULL,
    created_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by                  VARCHAR(50),
    updated_at                  TIMESTAMP
);

CREATE TABLE applicants (
    id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id               UUID         NOT NULL REFERENCES leads(id),
    applicant_label       VARCHAR(50)  NOT NULL,   -- MAIN APPLICANT | ADDL APPLICANT - N
    applicant_name        VARCHAR(200),
    gender                VARCHAR(1),               -- M | F (null for Non-Individual)
    date_of_birth         DATE,
    constitution_type     VARCHAR(30),              -- INDIVIDUAL | NON_INDIVIDUAL
    residential_type      VARCHAR(20)  DEFAULT 'RESIDENT',
    mobile                VARCHAR(15),
    email                 VARCHAR(200),
    pan                   VARCHAR(12),
    gstin                 VARCHAR(20),
    aadhaar_encrypted     VARCHAR(500),
    passport_number       VARCHAR(20),
    passport_validity_date DATE,
    voter_id              VARCHAR(30),
    driving_licence       VARCHAR(30),
    dan                   VARCHAR(50),
    pan_exemption_flag    VARCHAR(1)   DEFAULT 'N',
    occupation            VARCHAR(100),
    address_line1         VARCHAR(200),
    address_line2         VARCHAR(200),
    pincode               VARCHAR(10),
    location_name         VARCHAR(200),
    city                  VARCHAR(100),
    state                 VARCHAR(100),
    ucic_mapped           VARCHAR(50),
    dedup_result_code     VARCHAR(10),
    risk_category         VARCHAR(30),
    is_deceased           BOOLEAN      DEFAULT FALSE,
    sms_sent              BOOLEAN      DEFAULT FALSE,
    applicant_status      VARCHAR(30)  DEFAULT 'ACTIVE',
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Append-only assignment audit log — never UPDATE or DELETE
CREATE TABLE lead_assignments (
    id                      UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id                 UUID         NOT NULL REFERENCES leads(id),
    assigned_to_user_id     VARCHAR(50)  NOT NULL,
    assigned_to_team        VARCHAR(50),
    assigned_to_branch_code VARCHAR(20),
    hierarchy_level         VARCHAR(20)  NOT NULL,
    previous_user_id        VARCHAR(50),
    previous_team           VARCHAR(50),
    reason_code             VARCHAR(50)  NOT NULL,
    remarks                 TEXT         NOT NULL,
    assigned_by             VARCHAR(50)  NOT NULL,
    assigned_at             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sla_deadline            TIMESTAMP,
    sla_breached            BOOLEAN      DEFAULT FALSE,
    sla_breached_at         TIMESTAMP
);

-- Append-only interaction log — never UPDATE or DELETE
CREATE TABLE interactions (
    id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id               UUID         NOT NULL REFERENCES leads(id),
    interaction_type      VARCHAR(30)  NOT NULL,
    interaction_timestamp TIMESTAMP    NOT NULL,
    outcome_notes         TEXT         NOT NULL,
    contact_person        VARCHAR(200),
    contact_designation   VARCHAR(100),
    mode                  VARCHAR(30),
    next_action_date      TIMESTAMP,
    next_action_mode      VARCHAR(30),
    next_contact_person   VARCHAR(200),
    reminder_flag         BOOLEAN      DEFAULT FALSE,
    created_by            VARCHAR(50)  NOT NULL,
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Exception queue
CREATE TABLE exception_queue (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    source_type         VARCHAR(30)  NOT NULL,    -- MANUAL | BULK | API
    source_identifier   VARCHAR(200),
    reason_code         VARCHAR(50)  NOT NULL,
    reason_description  TEXT         NOT NULL,
    fields_in_error     TEXT,
    raw_data            JSONB,
    status              VARCHAR(20)  DEFAULT 'OPEN',
    owned_by            VARCHAR(50)  DEFAULT 'CPU',
    cure_sla_deadline   TIMESTAMP,
    resolved_by         VARCHAR(50),
    resolved_at         TIMESTAMP,
    resolution_notes    TEXT,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- -------------------------------------------------------
-- Indexes
-- -------------------------------------------------------
CREATE INDEX idx_leads_status           ON leads(status);
CREATE INDEX idx_leads_temperature      ON leads(temperature);
CREATE INDEX idx_leads_assigned_user    ON leads(assigned_user_id);
CREATE INDEX idx_leads_assigned_branch  ON leads(assigned_branch_code);
CREATE INDEX idx_leads_dedup_label      ON leads(dedup_label);
CREATE INDEX idx_leads_created_at       ON leads(created_at);
CREATE INDEX idx_applicants_lead_id     ON applicants(lead_id);
CREATE INDEX idx_applicants_pan         ON applicants(pan);
CREATE INDEX idx_applicants_mobile      ON applicants(mobile);
CREATE INDEX idx_applicants_gstin       ON applicants(gstin);
CREATE INDEX idx_interactions_lead_id   ON interactions(lead_id);
CREATE INDEX idx_assignments_lead_id    ON lead_assignments(lead_id);
CREATE INDEX idx_exception_status       ON exception_queue(status);
CREATE INDEX idx_exception_created_at   ON exception_queue(created_at);
