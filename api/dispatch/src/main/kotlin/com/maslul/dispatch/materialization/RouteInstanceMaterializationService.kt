package com.maslul.dispatch.materialization

import com.maslul.dispatch.entity.RouteInstance
import java.time.LocalDate
import java.util.UUID

// Materializes a RouteInstance + its Stops from a RouteTemplate for one shift/date. No
// optimization - stop order is copied verbatim from RouteTemplateStop.sequence (spec §0, §11 M0).
interface RouteInstanceMaterializationService {

    fun materialize(tenantId: UUID, shiftId: UUID, routeTemplateId: UUID, serviceDate: LocalDate): RouteInstance
}
