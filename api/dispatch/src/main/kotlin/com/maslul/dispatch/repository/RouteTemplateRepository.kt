package com.maslul.dispatch.repository

import com.maslul.dispatch.entity.RouteTemplate
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
interface RouteTemplateRepository : JpaRepository<RouteTemplate, UUID> {

    fun findByTenantIdAndId(tenantId: UUID, id: UUID): RouteTemplate?

    fun findAllByTenantId(tenantId: UUID): List<RouteTemplate>
}
