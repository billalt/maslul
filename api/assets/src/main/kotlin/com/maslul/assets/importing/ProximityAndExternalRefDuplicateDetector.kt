package com.maslul.assets.importing

import com.maslul.assets.entity.ServicePoint
import com.maslul.assets.repository.ServicePointRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ProximityAndExternalRefDuplicateDetector(
    private val servicePointRepository: ServicePointRepository,
) : ServicePointDuplicateDetector {

    override fun findDuplicate(tenantId: UUID, candidate: ImportedServicePoint, radiusMeters: Double): ServicePoint? {
        TODO("not yet implemented - awaiting review of the skeleton")
    }
}
