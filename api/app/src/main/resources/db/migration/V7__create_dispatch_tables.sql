CREATE TABLE route_template (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenant (id),
    name VARCHAR(255) NOT NULL,
    zone_id UUID REFERENCES zone (id),
    active BOOLEAN NOT NULL DEFAULT true,
    -- Human-readable, machine-ignored (e.g. "Sundays") - the calendar engine that would
    -- interpret a real schedule rule is spec §9 future work, not this slice.
    schedule_hint VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_route_template_tenant_id ON route_template (tenant_id);
CREATE INDEX idx_route_template_zone_id ON route_template (zone_id);

-- The template's ordered stop list (spec §3: "reusable ordered list of ServicePoints").
-- Materializing a RouteInstance copies this sequence verbatim - no optimizer in V1 (spec §0).
CREATE TABLE route_template_stop (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenant (id),
    route_template_id UUID NOT NULL REFERENCES route_template (id),
    service_point_id UUID NOT NULL REFERENCES service_point (id),
    sequence INTEGER NOT NULL,
    planned_duration_s INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (route_template_id, sequence)
);

CREATE INDEX idx_route_template_stop_tenant_id ON route_template_stop (tenant_id);
CREATE INDEX idx_route_template_stop_route_template_id ON route_template_stop (route_template_id);
CREATE INDEX idx_route_template_stop_service_point_id ON route_template_stop (service_point_id);

CREATE TABLE shift (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenant (id),
    vehicle_id UUID NOT NULL REFERENCES vehicle (id),
    driver_id UUID NOT NULL REFERENCES app_user (id),
    shift_date DATE NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PLANNED',
    started_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    odometer_start INTEGER,
    odometer_end INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_shift_tenant_id ON shift (tenant_id);
CREATE INDEX idx_shift_vehicle_id ON shift (vehicle_id);
CREATE INDEX idx_shift_driver_id ON shift (driver_id);
CREATE INDEX idx_shift_shift_date ON shift (shift_date);

-- version increments on every republish after a mid-shift edit (spec §5, replanning
-- conflicts). Materialization always creates version 1.
CREATE TABLE route_instance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenant (id),
    route_template_id UUID NOT NULL REFERENCES route_template (id),
    shift_id UUID NOT NULL REFERENCES shift (id),
    service_date DATE NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_route_instance_tenant_id ON route_instance (tenant_id);
CREATE INDEX idx_route_instance_route_template_id ON route_instance (route_template_id);
CREATE INDEX idx_route_instance_shift_id ON route_instance (shift_id);

-- planned_* and actual_* columns live on the same row deliberately (spec §3) - every
-- plan-vs-actual report is a comparison of those two column groups on one Stop.
CREATE TABLE stop (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenant (id),
    route_instance_id UUID NOT NULL REFERENCES route_instance (id),
    sequence INTEGER NOT NULL,
    service_point_id UUID NOT NULL REFERENCES service_point (id),
    planned_arrival TIMESTAMPTZ,
    planned_duration_s INTEGER,
    actual_arrival TIMESTAMPTZ,
    actual_departure TIMESTAMPTZ,
    outcome VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    outcome_reason VARCHAR(32),
    recorded_by VARCHAR(16),
    -- Set at materialization time when the template stop had a known data problem (decided
    -- during dispatch skeleton review: materialize and flag, never skip or fail the whole
    -- route). SERVICE_POINT_INACTIVE takes precedence over BODY_TYPE_MISMATCH when both
    -- apply - see DefaultRouteInstanceMaterializationService.
    materialization_issue VARCHAR(32),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (route_instance_id, sequence),
    -- Every non-COLLECTED outcome needs a reason (spec §3) - PENDING is the not-yet-visited
    -- default and is exempt.
    CONSTRAINT chk_stop_outcome_reason CHECK (
        outcome IN ('PENDING', 'COLLECTED') OR outcome_reason IS NOT NULL
    )
);

CREATE INDEX idx_stop_tenant_id ON stop (tenant_id);
CREATE INDEX idx_stop_route_instance_id ON stop (route_instance_id);
CREATE INDEX idx_stop_service_point_id ON stop (service_point_id);

-- media_ids[] in spec §3, one row per photo rather than an array column - photos are a
-- first-class artifact (spec §3), not an attachment.
CREATE TABLE stop_media (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenant (id),
    stop_id UUID NOT NULL REFERENCES stop (id),
    media_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_stop_media_tenant_id ON stop_media (tenant_id);
CREATE INDEX idx_stop_media_stop_id ON stop_media (stop_id);
