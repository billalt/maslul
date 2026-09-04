package com.maslul.dispatch.repository

import com.maslul.dispatch.entity.RouteTemplateStop
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
interface RouteTemplateStopRepository : JpaRepository<RouteTemplateStop, UUID> {

    fun findAllByTenantIdAndRouteTemplateIdOrderBySequenceAsc(
        tenantId: UUID,
        routeTemplateId: UUID,
    ): List<RouteTemplateStop>
}
