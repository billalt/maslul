-- Needed for import duplicate detection (spec: dedup by proximity + external_ref).
-- Container already has external_ref; ServicePoint didn't.
ALTER TABLE service_point ADD COLUMN external_ref VARCHAR(64);

-- Partial: many service points will have no external_ref (hand-drawn, or a source system
-- that doesn't assign asset numbers), and those must not collide with each other.
CREATE UNIQUE INDEX idx_service_point_external_ref ON service_point (tenant_id, external_ref)
    WHERE external_ref IS NOT NULL;
