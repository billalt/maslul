package com.maslul.sync.ingest

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class DefaultBreadcrumbIngestService(
    private val breadcrumbSinkPort: BreadcrumbSinkPort,
) : BreadcrumbIngestService {

    override fun ingest(tenantId: UUID, deviceId: UUID, points: List<IncomingBreadcrumb>) {
        breadcrumbSinkPort.accept(tenantId, deviceId, points)
    }
}
