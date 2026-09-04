-- tenant_id NOT NULL + a btree index already exist on every tenant-scoped table
-- (V2 for identity, V3 for assets). This migration adds the second enforcement layer:
-- Postgres row-level security, matching the TenantContext filter on the request path.
--
-- FORCE is required on every table: the app connects as the table owner (no separate
-- least-privilege role exists yet), and RLS policies do not apply to the owner unless
-- FORCE ROW LEVEL SECURITY is set.
--
-- current_setting(..., true) only returns NULL the very first time a custom GUC is ever
-- referenced on a session. Once ANY transaction on a pooled connection has called
-- set_config('app.tenant_id', ..., true) (SET LOCAL semantics), the value it reverts to
-- on commit/rollback is '' (empty string), not NULL/unset - Postgres treats a
-- once-touched custom GUC as defined with an empty-string default, not as absent again.
-- On a pooled connection reused across requests, that means the SECOND transaction with
-- no tenant in scope sees '' instead of NULL, and ''::uuid throws instead of yielding a
-- row-filtering NULL - turning "fail closed" into a hard 500 instead of zero rows.
-- NULLIF(..., '') folds that empty string back to NULL before the cast either way.
ALTER TABLE app_user ENABLE ROW LEVEL SECURITY;
ALTER TABLE app_user FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON app_user
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE device ENABLE ROW LEVEL SECURITY;
ALTER TABLE device FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON device
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE zone ENABLE ROW LEVEL SECURITY;
ALTER TABLE zone FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON zone
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE site ENABLE ROW LEVEL SECURITY;
ALTER TABLE site FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON site
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE vehicle ENABLE ROW LEVEL SECURITY;
ALTER TABLE vehicle FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON vehicle
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE service_point ENABLE ROW LEVEL SECURITY;
ALTER TABLE service_point FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON service_point
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE container ENABLE ROW LEVEL SECURITY;
ALTER TABLE container FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON container
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);
