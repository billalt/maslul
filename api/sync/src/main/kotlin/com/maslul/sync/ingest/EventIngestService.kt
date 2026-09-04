package com.maslul.sync.ingest

import com.maslul.sync.entity.DeviceEventType
import java.time.Instant
import java.util.UUID

// Batched, idempotent ingestion for POST /v1/sync/events (spec §5). Never shares a code
// path, table, or retry budget with breadcrumb ingestion - see BreadcrumbIngestService.
interface EventIngestService {

    // Upserts by event_id, applies device_seq ordering, and quarantines gaps instead of
    // applying out of order. Never discards a previously accepted event. Returns the
    // watermark after processing this batch: the highest device_seq durably and
    // contiguously persisted for this device.
    fun ingest(tenantId: UUID, deviceId: UUID, events: List<IncomingEvent>): Long
}

data class IncomingEvent(
    val eventId: UUID,
    val deviceSeq: Long,
    val deviceTime: Instant,
    val monotonicMs: Long,
    val type: DeviceEventType,
    // Raw JSON text, stored as-is in the jsonb payload column.
    val payload: String,
)
