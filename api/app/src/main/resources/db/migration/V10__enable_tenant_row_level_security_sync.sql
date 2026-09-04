-- Second enforcement layer for the sync tables (spec §4; CLAUDE.md multi-tenancy).
-- See V4's header comment for why FORCE and NULLIF(..., '') are both required.
ALTER TABLE device_event ENABLE ROW LEVEL SECURITY;
ALTER TABLE device_event FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON device_event
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE sync_cursor ENABLE ROW LEVEL SECURITY;
ALTER TABLE sync_cursor FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON sync_cursor
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);
