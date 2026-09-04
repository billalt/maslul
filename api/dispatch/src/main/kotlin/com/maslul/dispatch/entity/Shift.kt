package com.maslul.dispatch.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "shift")
class Shift(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    var tenantId: UUID? = null,

    // assets.Vehicle - referenced by id only, no cross-module entity import (spec §4).
    @Column(name = "vehicle_id", nullable = false)
    var vehicleId: UUID? = null,

    // identity.User with role DRIVER - referenced by id only, no cross-module entity import (spec §4).
    @Column(name = "driver_id", nullable = false)
    var driverId: UUID? = null,

    @Column(name = "shift_date", nullable = false)
    var shiftDate: LocalDate = LocalDate.now(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ShiftStatus = ShiftStatus.PLANNED,

    @Column(name = "started_at")
    var startedAt: Instant? = null,

    @Column(name = "ended_at")
    var endedAt: Instant? = null,

    @Column(name = "odometer_start")
    var odometerStart: Int? = null,

    @Column(name = "odometer_end")
    var odometerEnd: Int? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
