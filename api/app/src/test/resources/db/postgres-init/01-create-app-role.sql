-- The app (Flyway included) must NOT connect as a Postgres superuser: superusers always
-- bypass row-level security regardless of FORCE ROW LEVEL SECURITY, which would silently
-- defeat every tenant_isolation policy. This script runs (via docker-entrypoint-initdb.d
-- locally, via Testcontainers withInitScript in tests) connected to the target database
-- as the bootstrap superuser, and hands that database's public schema to a plain role.
CREATE ROLE maslul WITH LOGIN PASSWORD 'maslul';
ALTER SCHEMA public OWNER TO maslul;
