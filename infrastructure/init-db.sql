-- Run as: psql -U postgres -h 127.0.0.1 -f init-db.sql
SELECT 'Checking leasing_user...' AS step;

DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'leasing_user') THEN
    CREATE USER leasing_user WITH PASSWORD 'leasing_pass';
    RAISE NOTICE 'Created leasing_user';
  ELSE
    ALTER USER leasing_user WITH PASSWORD 'leasing_pass';
    RAISE NOTICE 'leasing_user already exists — password reset';
  END IF;
END
$$;

SELECT 'Checking leasing_db...' AS step;

SELECT pg_catalog.set_config('search_path', '', false);

DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'leasing_db') THEN
    RAISE NOTICE 'leasing_db does not exist yet — create it next';
  ELSE
    RAISE NOTICE 'leasing_db already exists';
  END IF;
END
$$;

GRANT ALL PRIVILEGES ON DATABASE postgres TO leasing_user;
SELECT 'Done.' AS step;
