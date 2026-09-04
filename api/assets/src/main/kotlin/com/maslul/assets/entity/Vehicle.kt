package com.maslul.assets.entity

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
@Table(name = "vehicle")
class Vehicle(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    var tenantId: UUID? = null,

    @Column(nullable = false)
    var plate: String = "",

    @Column(nullable = false)
    var type: String = "",

    @Column(name = "capacity_m3", nullable = false)
    var capacityM3: Double = 0.0,

    @Column(name = "capacity_kg", nullable = false)
    var capacityKg: Double = 0.0,

    @Column(name = "height_cm", nullable = false)
    var heightCm: Int = 0,

    @Column(name = "width_cm", nullable = false)
    var widthCm: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "body_type", nullable = false)
    var bodyType: BodyType = BodyType.REAR_LOADER,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
