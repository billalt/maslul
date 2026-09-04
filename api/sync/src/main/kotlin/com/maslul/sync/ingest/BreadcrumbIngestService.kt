package com.maslul.sync.ingest

import java.time.Instant
import java.util.UUID

// Batched, lossy ingestion for POST /v1/sync/breadcrumbs (spec §5). No idempotency key, no
// ordering guarantee, no shared code path/table/retry budget with EventIngestService - a
// gap in the GPS trace is cosmetic, a dropped stop outcome is a billing dispute.
interface BreadcrumbIngestService {

    fun ingest(tenantId: UUID, deviceId: UUID, points: List<IncomingBreadcrumb>)
}

data class IncomingBreadcrumb(
    val lat: Double,
    val lng: Double,
    val t: Instant,
    val speedMps: Double?,
    val headingDeg: Double?,
    val accuracyM: Double?,
)
