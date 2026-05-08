-- ============================================================
-- Phase 5: Customer Creation, Lineage & Downstream Integration
-- ============================================================

-- -------------------------------------------------------
-- 1. Extend lineage table with full chain IDs
-- -------------------------------------------------------

ALTER TABLE lineage
    ADD COLUMN IF NOT EXISTS opportunity_uuid         UUID,
    ADD COLUMN IF NOT EXISTS opportunity_business_id  VARCHAR(30),
    ADD COLUMN IF NOT EXISTS quote_uuid               UUID,
    ADD COLUMN IF NOT EXISTS quote_business_id        VARCHAR(30),
    ADD COLUMN IF NOT EXISTS application_uuid         UUID,
    ADD COLUMN IF NOT EXISTS application_business_id  VARCHAR(30),
    ADD COLUMN IF NOT EXISTS customer_uuid            UUID,
    ADD COLUMN IF NOT EXISTS customer_id              VARCHAR(30);  -- CUST-YYYY-NNNNNN

-- -------------------------------------------------------
-- 2. Enterprise Customer master
-- -------------------------------------------------------

CREATE TABLE customers (
    id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id           VARCHAR(30)  UNIQUE NOT NULL,   -- CUST-YYYY-NNNNNN
    prospect_uuid         UUID         NOT NULL REFERENCES prospects(id),
    prospect_business_id  VARCHAR(30),
    application_uuid      UUID         NOT NULL REFERENCES applications(id),
    application_id        VARCHAR(30),

    -- Legal identity (sourced from validated Prospect)
    lead_type             VARCHAR(20)  NOT NULL,          -- INDIVIDUAL | COMMERCIAL
    legal_name            VARCHAR(500) NOT NULL,
    pan                   VARCHAR(20),
    gstin                 VARCHAR(30),
    registered_address    TEXT,
    registered_pincode    VARCHAR(10),
    ucic                  VARCHAR(50),

    -- Gate condition timestamps
    kyc_completed_at      TIMESTAMP,
    cam_approved_at       TIMESTAMP,
    sanction_id           VARCHAR(100),
    sanction_package      VARCHAR(200),

    -- Status
    status                VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE | INACTIVE | SUSPENDED

    -- Audit
    created_by            VARCHAR(50)  NOT NULL,
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by            VARCHAR(50),
    updated_at            TIMESTAMP
);

CREATE INDEX idx_customers_prospect ON customers(prospect_uuid);
CREATE INDEX idx_customers_application ON customers(application_uuid);

-- -------------------------------------------------------
-- 3. Customer role assignments (PP8.3)
-- -------------------------------------------------------

CREATE TABLE customer_role_assignments (
    id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_uuid         UUID         NOT NULL REFERENCES customers(id),
    customer_id           VARCHAR(30),
    role_type             VARCHAR(50)  NOT NULL,   -- LESSEE_INDIVIDUAL | LESSEE_CORPORATE | DEALER | VENDOR | DEPOSITOR
    checklist_complete    BOOLEAN      NOT NULL DEFAULT FALSE,
    checklist_items       TEXT,                    -- JSON array of completed checklist items
    status                VARCHAR(20)  NOT NULL DEFAULT 'PENDING',  -- PENDING | ACTIVE | INACTIVE
    remarks               VARCHAR(500),

    -- Audit
    assigned_by           VARCHAR(50)  NOT NULL,
    assigned_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by            VARCHAR(50),
    updated_at            TIMESTAMP
);

CREATE INDEX idx_role_assign_customer ON customer_role_assignments(customer_uuid);

-- -------------------------------------------------------
-- 4. Downstream event dispatch log (for observability)
-- -------------------------------------------------------

CREATE TABLE downstream_event_log (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type      VARCHAR(100) NOT NULL,
    entity_type     VARCHAR(50)  NOT NULL,   -- CUSTOMER | OPPORTUNITY | APPLICATION
    entity_id       VARCHAR(50)  NOT NULL,
    topic           VARCHAR(200),
    payload         TEXT,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PUBLISHED',  -- PUBLISHED | FAILED | RETRY
    error_message   TEXT,
    published_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_downstream_entity ON downstream_event_log(entity_type, entity_id);
CREATE INDEX idx_downstream_status ON downstream_event_log(status);

-- -------------------------------------------------------
-- 5. Sequence control: Customer ID
-- -------------------------------------------------------

INSERT INTO sequence_control (sequence_name, current_value, reset_year, reset_month, updated_at)
VALUES ('CUSTOMER_ID', 0, EXTRACT(YEAR FROM CURRENT_DATE), NULL, CURRENT_TIMESTAMP)
ON CONFLICT (sequence_name) DO NOTHING;
