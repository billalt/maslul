package com.maslul.dispatch.materialization

import com.maslul.dispatch.entity.MaterializationIssue
import com.maslul.dispatch.entity.RouteInstance
import com.maslul.dispatch.entity.RouteInstanceStatus
import com.maslul.dispatch.entity.Stop
import com.maslul.dispatch.entity.StopOutcome
import com.maslul.dispatch.repository.RouteInstanceRepository
import com.maslul.dispatch.repository.RouteTemplateStopRepository
import com.maslul.dispatch.repository.ShiftRepository
import com.maslul.dispatch.repository.StopRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class DefaultRouteInstanceMaterializationService(
    private val routeTemplateStopRepository: RouteTemplateStopRepository,
    private val shiftRepository: ShiftRepository,
    private val routeInstanceRepository: RouteInstanceRepository,
    private val stopRepository: StopRepository,
    private val assetAvailabilityPort: AssetAvailabilityPort,
) : RouteInstanceMaterializationService {

    // One transaction for the RouteInstance and every Stop it materializes - a partial
    // failure here must not publish half a route.
    @Transactional
    override fun materialize(tenantId: UUID, shiftId: UUID, routeTemplateId: UUID, serviceDate: LocalDate): RouteInstance {
        val shift = shiftRepository.findByTenantIdAndId(tenantId, shiftId)
            ?: throw NoSuchElementException("Shift $shiftId not found for tenant $tenantId")
        val vehicleId = checkNotNull(shift.vehicleId) { "Shift $shiftId has no vehicle" }
        val vehicleBodyType = assetAvailabilityPort.vehicleBodyTypeName(tenantId, vehicleId)

        val templateStops = routeTemplateStopRepository
            .findAllByTenantIdAndRouteTemplateIdOrderBySequenceAsc(tenantId, routeTemplateId)

        val routeInstance = routeInstanceRepository.save(
            RouteInstance(
                tenantId = tenantId,
                routeTemplateId = routeTemplateId,
                shiftId = shiftId,
                serviceDate = serviceDate,
                version = 1,
                status = RouteInstanceStatus.DRAFT,
            ),
        )
        val routeInstanceId = checkNotNull(routeInstance.id)

        // No optimization - stop order and duration are copied verbatim from the template
        // (spec §0, §11 M0). A bad stop is materialized and flagged, never skipped or made to
        // fail the whole route (decided during dispatch skeleton review).
        for (templateStop in templateStops) {
            val servicePointId = checkNotNull(templateStop.servicePointId)
            stopRepository.save(
                Stop(
                    tenantId = tenantId,
                    routeInstanceId = routeInstanceId,
                    sequence = templateStop.sequence,
                    servicePointId = servicePointId,
                    plannedDurationS = templateStop.plannedDurationS,
                    outcome = StopOutcome.PENDING,
                    materializationIssue = materializationIssueFor(tenantId, servicePointId, vehicleBodyType),
                ),
            )
        }

        return routeInstance
    }

    private fun materializationIssueFor(
        tenantId: UUID,
        servicePointId: UUID,
        vehicleBodyType: String?,
    ): MaterializationIssue? {
        if (!assetAvailabilityPort.isServicePointActive(tenantId, servicePointId)) {
            return MaterializationIssue.SERVICE_POINT_INACTIVE
        }

        val requiredBodyTypes = assetAvailabilityPort.requiredBodyTypeNames(tenantId, servicePointId)
        if (requiredBodyTypes.isNotEmpty() && vehicleBodyType !in requiredBodyTypes) {
            return MaterializationIssue.BODY_TYPE_MISMATCH
        }

        return null
    }
}
