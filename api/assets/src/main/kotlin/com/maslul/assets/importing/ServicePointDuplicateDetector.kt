package com.maslul.assets.importing

import com.maslul.assets.entity.ServicePoint
import java.util.UUID

interface ServicePointDuplicateDetector {
    // Dedup by proximity + external_ref, per spec. Exact match strategy TBD at implementation time.
    fun findDuplicate(tenantId: UUID, candidate: ImportedServicePoint): ServicePoint?
}
