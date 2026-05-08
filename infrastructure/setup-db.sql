-- Setup leasing database and user for Phase 1
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'leasing_user') THEN
        CREATE USER leasing_user WITH PASSWORD 'leasing_pass';
    END IF;
END
$$;

SELECT 'leasing_user exists' AS status;

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'leasing_db') THEN
        PERFORM dblink_exec('dbname=' || current_database(), 'CREATE DATABASE leasing_db OWNER leasing_user');
    END IF;
END
$$;
