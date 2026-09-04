package com.maslul.sync.repository

import com.maslul.sync.entity.DeviceEvent
import com.maslul.sync.entity.DeviceEventStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional(readOnly = true)
interface DeviceEventRepository : JpaRepository<DeviceEvent, UUID> {

    // The idempotency lookup - see spec §5.
    fun findByTenantIdAndEventId(tenantId: UUID, eventId: UUID): DeviceEvent?

    // Used to cascade-promote a quarantined event once the device_seq immediately before it
    // has been accepted (see EventIngestService).
    fun findByTenantIdAndDeviceIdAndStatusAndDeviceSeq(
        tenantId: UUID,
        deviceId: UUID,
        status: DeviceEventStatus,
        deviceSeq: Long,
    ): DeviceEvent?

    fun findAllByTenantIdAndDeviceIdOrderByDeviceSeqAsc(tenantId: UUID, deviceId: UUID): List<DeviceEvent>
}
