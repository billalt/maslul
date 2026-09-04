package com.maslul.sync.repository

import com.maslul.sync.entity.SyncCursor
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
interface SyncCursorRepository : JpaRepository<SyncCursor, UUID> {

    fun findByTenantIdAndDeviceId(tenantId: UUID, deviceId: UUID): SyncCursor?
}
