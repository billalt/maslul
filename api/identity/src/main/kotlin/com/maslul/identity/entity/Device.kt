package com.maslul.identity.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "device")
class Device(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    var tenantId: UUID? = null,

    @Column(name = "user_id")
    var userId: UUID? = null,

    @Column(name = "device_code", nullable = false)
    var deviceCode: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: DeviceStatus = DeviceStatus.PENDING,

    @Column(name = "last_seen_at")
    var lastSeenAt: Instant? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
