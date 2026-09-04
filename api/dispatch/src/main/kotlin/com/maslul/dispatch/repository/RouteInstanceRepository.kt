package com.maslul.dispatch.repository

import com.maslul.dispatch.entity.RouteInstance
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
interface RouteInstanceRepository : JpaRepository<RouteInstance, UUID> {

    fun findByTenantIdAndId(tenantId: UUID, id: UUID): RouteInstance?

    fun findByTenantIdAndShiftId(tenantId: UUID, shiftId: UUID): RouteInstance?
}
