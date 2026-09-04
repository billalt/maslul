package com.maslul.dispatch.repository

import com.maslul.dispatch.entity.StopMedia
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
interface StopMediaRepository : JpaRepository<StopMedia, UUID> {

    fun findAllByTenantIdAndStopId(tenantId: UUID, stopId: UUID): List<StopMedia>
}
