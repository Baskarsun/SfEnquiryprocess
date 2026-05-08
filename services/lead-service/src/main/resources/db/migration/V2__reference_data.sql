-- ============================================================
-- Reference / seed data for Phase 1
-- ============================================================

-- Sequence seeds
INSERT INTO sequence_control (sequence_name, current_value, reset_year, reset_month)
VALUES
    ('LRN_LS',       0, EXTRACT(YEAR FROM CURRENT_DATE)::SMALLINT, EXTRACT(MONTH FROM CURRENT_DATE)::SMALLINT),
    ('TEMP_CUSTOMER', 0, EXTRACT(YEAR FROM CURRENT_DATE)::SMALLINT, NULL);

-- SLA configuration per hierarchy level (configurable thresholds)
INSERT INTO sla_config (hierarchy_level, threshold_hours, escalation_target)
VALUES
    ('CENTRAL',           4,  'OPERATIONS_OFFICER'),
    ('BRANCH_MANAGER',   24,  'RVP'),
    ('FIELD_OFFICER',    48,  'BRANCH_MANAGER'),
    ('CALL_CENTRE',      48,  'BRANCH_MANAGER'),
    ('EXCEPTION_QUEUE',  72,  'CPU');

-- Sample employees (dev / test)
INSERT INTO employees (employee_id, name, status, branch_code, role)
VALUES
    ('EMP001', 'CPU Admin',               'ACTIVE', 'CPU001', 'CPU_ADMIN'),
    ('EMP002', 'Test Branch Manager',     'ACTIVE', 'BRN001', 'BRANCH_MANAGER'),
    ('EMP003', 'Test Field Officer',      'ACTIVE', 'BRN001', 'FIELD_OFFICER'),
    ('EMP004', 'Test Call Centre Assoc',  'ACTIVE', 'CC001',  'CALL_CENTRE'),
    ('EMP005', 'Test FO Absent',          'ON_LEAVE','BRN001','FIELD_OFFICER');

-- Sample branch config
INSERT INTO branch_config (branch_code, branch_name, segment_code, max_distance_km, latitude, longitude, is_active,
                            business_open_date, accounting_open_date)
VALUES
    ('CPU001', 'Central Processing Unit', 'CPU', NULL,  19.0760, 72.8777, TRUE, CURRENT_DATE, CURRENT_DATE),
    ('BRN001', 'Mumbai Main Branch',      'GE',  50.0,  19.0760, 72.8777, TRUE, CURRENT_DATE, CURRENT_DATE),
    ('BRN002', 'Delhi Branch',            'GE',  50.0,  28.6139, 77.2090, TRUE, CURRENT_DATE, CURRENT_DATE),
    ('SME001', 'Mumbai SME Branch',       'SM',  NULL,  19.0760, 72.8777, TRUE, CURRENT_DATE, CURRENT_DATE),
    ('CC001',  'Call Centre',             'CC',  NULL,  19.0760, 72.8777, TRUE, CURRENT_DATE, CURRENT_DATE);

-- Sample device registrations (dev / test)
INSERT INTO device_registry (employee_id, imei, device_uuid, device_type, is_authorised)
VALUES
    ('EMP003', '123456789012345', 'device-uuid-fo-001',  'MOBILE', TRUE),
    ('EMP004', '987654321098765', 'device-uuid-cc-001',  'MOBILE', TRUE);

-- Sample DAN entries (dev / test)
INSERT INTO dan_registry (dan, form_code, exemption_unique_number, is_used)
VALUES
    ('DAN001TEST', 'FORM60', 'EXEMPT001', FALSE),
    ('DAN002USED', 'FORM60', 'EXEMPT002', TRUE);
