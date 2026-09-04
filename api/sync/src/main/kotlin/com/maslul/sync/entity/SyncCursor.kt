package com.maslul.sync.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

// One row per device. last_device_seq is the watermark (spec §5): the highest device_seq
// durably and contiguously persisted, with no gap before it. The device deletes its queue
// up to this value and nothing more.
@Entity
@Table(name = "sync_cursor")
class SyncCursor(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    var tenantId: UUID? = null,

    @Column(name = "device_id", nullable = false)
    var deviceId: UUID? = null,

    @Column(name = "last_device_seq", nullable = false)
    var lastDeviceSeq: Long = 0,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
