-- Device-originated, append-only, idempotent by event_id (spec §5). status distinguishes
-- events applied in order (ACCEPTED) from ones held because they arrived ahead of a gap in
-- device_seq (QUARANTINED) - never discarded, never applied out of order.
CREATE TABLE device_event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenant (id),
    device_id UUID NOT NULL REFERENCES device (id),
    event_id UUID NOT NULL,
    device_seq BIGINT NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    device_time TIMESTAMPTZ NOT NULL,
    monotonic_ms BIGINT NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(16) NOT NULL,
    -- Set once, when the row is first inserted as QUARANTINED; untouched on later promotion
    -- to ACCEPTED - the forensic trail for a disputed stop outcome.
    quarantined_at TIMESTAMPTZ,
    -- Stamped once, on first ingestion (quarantined or accepted) - a replayed batch never
    -- changes this, which is how the idempotency test proves identical state.
    server_received_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- event_id, not device_seq, is the idempotency key (spec §5) - devices replay batches
    -- constantly because they lose connectivity mid-POST and cannot tell whether the write
    -- landed. Deliberately NOT also unique on (tenant_id, device_id, device_seq): a device
    -- that reinstalls or resets its counter can legitimately reuse a device_seq value
    -- already on file for that device_id under a brand new event_id, and that must be
    -- accepted, not rejected on a constraint violation.
    UNIQUE (tenant_id, event_id)
);

CREATE INDEX idx_device_event_tenant_id ON device_event (tenant_id);
-- Ordered per-device scans (e.g. findAllByTenantIdAndDeviceIdOrderByDeviceSeqAsc).
CREATE INDEX idx_device_event_device_seq ON device_event (device_id, device_seq);
-- Cascade-promotes quarantined events once the gap ahead of them fills - see
-- DefaultEventIngestService.
CREATE INDEX idx_device_event_device_status_seq ON device_event (device_id, status, device_seq);

-- One row per device. last_device_seq is the watermark (spec §5): the highest device_seq
-- durably and contiguously persisted, with no gap before it. The device deletes its queue
-- up to this value and nothing more.
CREATE TABLE sync_cursor (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenant (id),
    device_id UUID NOT NULL REFERENCES device (id),
    last_device_seq BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, device_id)
);

CREATE INDEX idx_sync_cursor_tenant_id ON sync_cursor (tenant_id);
