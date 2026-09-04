package com.maslul.sync.ingest

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

// Placeholder BreadcrumbSinkPort until telemetry exists to implement it for real (spec §4
// assigns the breadcrumb table to telemetry, not sync). Breadcrumbs are the lossy channel
// by design (spec §5) - discarding them here costs nothing today. Replace this bean with a
// telemetry-backed implementation when that module is built; do not extend this one.
@Component
class LoggingBreadcrumbSinkPort : BreadcrumbSinkPort {

    private val log = LoggerFactory.getLogger(LoggingBreadcrumbSinkPort::class.java)

    override fun accept(tenantId: UUID, deviceId: UUID, points: List<IncomingBreadcrumb>) {
        log.debug(
            "Discarding {} breadcrumb point(s) for device {} (tenant {}) - no telemetry sink yet",
            points.size, deviceId, tenantId,
        )
    }
}
