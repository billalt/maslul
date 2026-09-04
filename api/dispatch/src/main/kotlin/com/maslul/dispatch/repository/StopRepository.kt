package com.maslul.dispatch.repository

import com.maslul.dispatch.entity.Stop
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
interface StopRepository : JpaRepository<Stop, UUID> {

    fun findAllByTenantIdAndRouteInstanceIdOrderBySequenceAsc(
        tenantId: UUID,
        routeInstanceId: UUID,
    ): List<Stop>
}
