CREATE TABLE zone (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenant (id),
    name VARCHAR(255) NOT NULL,
    boundary geometry(Geometry, 4326),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_zone_tenant_id ON zone (tenant_id);
CREATE INDEX idx_zone_boundary ON zone USING GIST (boundary);

CREATE TABLE site (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenant (id),
    name VARCHAR(255) NOT NULL,
    type VARCHAR(32) NOT NULL,
    location geometry(Point, 4326) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_site_tenant_id ON site (tenant_id);
CREATE INDEX idx_site_location ON site USING GIST (location);

CREATE TABLE vehicle (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenant (id),
    plate VARCHAR(32) NOT NULL,
    type VARCHAR(64) NOT NULL,
    capacity_m3 NUMERIC(8, 2) NOT NULL,
    capacity_kg NUMERIC(10, 2) NOT NULL,
    height_cm INTEGER NOT NULL,
    width_cm INTEGER NOT NULL,
    body_type VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, plate)
);

CREATE INDEX idx_vehicle_tenant_id ON vehicle (tenant_id);

-- One table serves both point collection (geometry = Point) and segment collection
-- (geometry = LineString); geometry is untyped so both shapes fit the same column,
-- and the check constraint below keeps type/geometry/side consistent. See spec §3.
CREATE TABLE service_point (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenant (id),
    type VARCHAR(16) NOT NULL,
    geometry geometry(Geometry, 4326) NOT NULL,
    side VARCHAR(8),
    address VARCHAR(255) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_service_point_geometry_type CHECK (
        (type = 'POINT' AND GeometryType(geometry) = 'POINT') OR
        (type = 'SEGMENT' AND GeometryType(geometry) = 'LINESTRING')
    ),
    CONSTRAINT chk_service_point_side CHECK (
        (type = 'SEGMENT' AND side IS NOT NULL) OR
        (type = 'POINT' AND side IS NULL)
    )
);

CREATE INDEX idx_service_point_tenant_id ON service_point (tenant_id);
CREATE INDEX idx_service_point_geometry ON service_point USING GIST (geometry);

CREATE TABLE container (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenant (id),
    service_point_id UUID NOT NULL REFERENCES service_point (id),
    external_ref VARCHAR(64),
    rfid_tag VARCHAR(64),
    waste_stream VARCHAR(32) NOT NULL,
    volume_liters INTEGER NOT NULL,
    container_type VARCHAR(32) NOT NULL,
    required_body_type VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    access_notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_container_tenant_id ON container (tenant_id);
CREATE INDEX idx_container_service_point_id ON container (service_point_id);
