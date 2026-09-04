-- Needed for import duplicate detection (spec: dedup by proximity + external_ref).
-- Container already has external_ref; ServicePoint didn't.
ALTER TABLE service_point ADD COLUMN external_ref VARCHAR(64);

CREATE INDEX idx_service_point_external_ref ON service_point (tenant_id, external_ref);
