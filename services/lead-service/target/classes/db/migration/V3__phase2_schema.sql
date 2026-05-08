-- ============================================================
-- Phase 2: Lead Intelligence & Bulk Ingestion — Schema Additions
-- ============================================================

-- Temperature change audit (append-only)
CREATE TABLE temperature_audit (
    id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id              UUID         NOT NULL REFERENCES leads(id),
    previous_temperature VARCHAR(10),
    new_temperature      VARCHAR(10)  NOT NULL,
    reason_code          VARCHAR(50),
    reason_text          VARCHAR(500),
    suggested_by         VARCHAR(10)  NOT NULL DEFAULT 'SYSTEM',  -- SYSTEM | MANUAL
    changed_by           VARCHAR(50),
    changed_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Bulk upload job tracking
CREATE TABLE bulk_upload_jobs (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    job_type       VARCHAR(20) NOT NULL,        -- EXCEL | API
    total_rows     INTEGER     DEFAULT 0,
    processed_rows INTEGER     DEFAULT 0,
    success_rows   INTEGER     DEFAULT 0,
    failed_rows    INTEGER     DEFAULT 0,
    status         VARCHAR(20) DEFAULT 'PENDING', -- PENDING | IN_PROGRESS | COMPLETED | FAILED
    created_by     VARCHAR(50) NOT NULL,
    created_at     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at   TIMESTAMP
);

-- Per-row result within a bulk job
CREATE TABLE bulk_upload_rows (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id         UUID        NOT NULL REFERENCES bulk_upload_jobs(id),
    row_number     INTEGER     NOT NULL,
    lrn            VARCHAR(30),
    status         VARCHAR(20) NOT NULL,         -- SUCCESS | FAILED
    error_code     VARCHAR(50),
    error_message  TEXT,
    raw_data       TEXT,
    processed_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Deduplication check results per applicant
CREATE TABLE lead_dedup_results (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id             UUID        NOT NULL REFERENCES leads(id),
    applicant_id        UUID        REFERENCES applicants(id),
    check_type          VARCHAR(30) NOT NULL,  -- INTERNAL | EXTERNAL_PAN | UCIC | CAUTION_LIST
    result_code         VARCHAR(20) NOT NULL,  -- COM110 | COM111 | COM66 | ALLOWED | UNKNOWN
    matched_entity_id   VARCHAR(50),
    matched_entity_type VARCHAR(30),           -- LEAD | APPLICANT | EXTERNAL
    checked_at          TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- SLA breach event log (for dashboard reporting)
CREATE TABLE sla_breach_log (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id          UUID        NOT NULL REFERENCES leads(id),
    assignment_id    UUID        REFERENCES lead_assignments(id),
    breach_type      VARCHAR(50) NOT NULL,   -- CPU_FIRST_ASSIGN | BRANCH_OFFICER | FIRST_INTERACTION | EXCEPTION_CURE
    assigned_user_id VARCHAR(50),
    hierarchy_level  VARCHAR(30),
    sla_deadline     TIMESTAMP,
    breached_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notified         BOOLEAN     DEFAULT FALSE
);

-- -------------------------------------------------------
-- Indexes
-- -------------------------------------------------------
CREATE INDEX idx_temp_audit_lead_id    ON temperature_audit(lead_id);
CREATE INDEX idx_temp_audit_changed_at ON temperature_audit(changed_at);
CREATE INDEX idx_bulk_job_status       ON bulk_upload_jobs(status);
CREATE INDEX idx_bulk_row_job_id       ON bulk_upload_rows(job_id);
CREATE INDEX idx_dedup_lead_id         ON lead_dedup_results(lead_id);
CREATE INDEX idx_sla_breach_lead_id    ON sla_breach_log(lead_id);
CREATE INDEX idx_sla_breach_type       ON sla_breach_log(breach_type);

-- -------------------------------------------------------
-- Phase 2 sequence seeds
-- -------------------------------------------------------
INSERT INTO sequence_control (sequence_name, current_value, reset_year, reset_month)
VALUES ('BULK_JOB', 0, EXTRACT(YEAR FROM CURRENT_DATE)::SMALLINT, NULL)
ON CONFLICT (sequence_name) DO NOTHING;

-- -------------------------------------------------------
-- Pincode centroid reference table (for Haversine distance)
-- -------------------------------------------------------
CREATE TABLE pincode_coordinates (
    pincode   VARCHAR(10) PRIMARY KEY,
    latitude  DECIMAL(10,8) NOT NULL,
    longitude DECIMAL(11,8) NOT NULL
);

-- Sample pincode coordinates (dev/test)
INSERT INTO pincode_coordinates (pincode, latitude, longitude) VALUES
    ('400001', 18.9322, 72.8264),  -- Mumbai
    ('400051', 19.0596, 72.8295),  -- Bandra
    ('110001', 28.6448, 77.2167),  -- New Delhi
    ('560001', 12.9716, 77.5946),  -- Bangalore
    ('600001', 13.0827, 80.2707);  -- Chennai
