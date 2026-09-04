-- Second enforcement layer for the dispatch tables (spec §4; CLAUDE.md multi-tenancy).
-- See V4's header comment for why FORCE and NULLIF(..., '') are both required.
ALTER TABLE route_template ENABLE ROW LEVEL SECURITY;
ALTER TABLE route_template FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON route_template
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE route_template_stop ENABLE ROW LEVEL SECURITY;
ALTER TABLE route_template_stop FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON route_template_stop
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE shift ENABLE ROW LEVEL SECURITY;
ALTER TABLE shift FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON shift
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE route_instance ENABLE ROW LEVEL SECURITY;
ALTER TABLE route_instance FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON route_instance
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE stop ENABLE ROW LEVEL SECURITY;
ALTER TABLE stop FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON stop
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);

ALTER TABLE stop_media ENABLE ROW LEVEL SECURITY;
ALTER TABLE stop_media FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON stop_media
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::uuid);
