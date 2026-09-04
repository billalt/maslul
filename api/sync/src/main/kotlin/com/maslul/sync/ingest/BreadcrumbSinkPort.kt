package com.maslul.sync.ingest

import java.util.UUID

// sync is the calling module for breadcrumb storage; telemetry owns the breadcrumb table
// and should implement this once it exists (spec §4: modules talk through interfaces
// defined in the calling module, no cross-module entity imports) - same shape as
// dispatch's AssetAvailabilityPort, implemented by assets.
//
// Until telemetry is built (its own task - reviewed and deliberately deferred, not an
// oversight), the only implementation is LoggingBreadcrumbSinkPort, a discard-and-log
// no-op. That's an acceptable interim: breadcrumbs are the lossy channel by design (spec
// §5), unlike device_event, which is durable from this PR onward.
interface BreadcrumbSinkPort {

    fun accept(tenantId: UUID, deviceId: UUID, points: List<IncomingBreadcrumb>)
}
