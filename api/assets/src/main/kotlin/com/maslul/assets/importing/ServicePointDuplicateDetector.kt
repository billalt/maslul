package com.maslul.assets.importing

import com.maslul.assets.entity.ServicePoint
import java.util.UUID

interface ServicePointDuplicateDetector {
    // external_ref match wins outright and skips the spatial check entirely; proximity
    // (within radiusMeters) is the fallback for rows with no ref or no ref match.
    fun findDuplicate(tenantId: UUID, candidate: ImportedServicePoint, radiusMeters: Double): ServicePoint?
}
