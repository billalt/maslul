package com.maslul.assets.materialization

import com.maslul.assets.entity.ServicePointStatus
import com.maslul.assets.repository.ContainerRepository
import com.maslul.assets.repository.ServicePointRepository
import com.maslul.assets.repository.VehicleRepository
import com.maslul.dispatch.materialization.AssetAvailabilityPort
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class AssetsAvailabilityAdapter(
    private val servicePointRepository: ServicePointRepository,
    private val containerRepository: ContainerRepository,
    private val vehicleRepository: VehicleRepository,
) : AssetAvailabilityPort {

    override fun isServicePointActive(tenantId: UUID, servicePointId: UUID): Boolean =
        servicePointRepository.findByTenantIdAndId(tenantId, servicePointId)?.status == ServicePointStatus.ACTIVE

    override fun requiredBodyTypeNames(tenantId: UUID, servicePointId: UUID): Set<String> =
        containerRepository.findAllByTenantIdAndServicePointId(tenantId, servicePointId)
            .map { it.requiredBodyType.name }
            .toSet()

    override fun vehicleBodyTypeName(tenantId: UUID, vehicleId: UUID): String? =
        vehicleRepository.findByTenantIdAndId(tenantId, vehicleId)?.bodyType?.name
}
