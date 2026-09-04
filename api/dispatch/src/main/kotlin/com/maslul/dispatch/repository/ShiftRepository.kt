package com.maslul.dispatch.repository

import com.maslul.dispatch.entity.Shift
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
interface ShiftRepository : JpaRepository<Shift, UUID> {

    fun findByTenantIdAndId(tenantId: UUID, id: UUID): Shift?

    fun findAllByTenantId(tenantId: UUID): List<Shift>
}
