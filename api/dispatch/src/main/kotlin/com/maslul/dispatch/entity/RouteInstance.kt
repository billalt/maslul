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

// version increments on every republish after a mid-shift edit (spec §5, replanning
// conflicts). Materialization always creates version 1.
@Entity
@Table(name = "route_instance")
class RouteInstance(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    var tenantId: UUID? = null,

    @Column(name = "route_template_id", nullable = false)
    var routeTemplateId: UUID? = null,

    @Column(name = "shift_id", nullable = false)
    var shiftId: UUID? = null,

    @Column(name = "service_date", nullable = false)
    var serviceDate: LocalDate = LocalDate.now(),

    @Column(nullable = false)
    var version: Int = 1,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: RouteInstanceStatus = RouteInstanceStatus.DRAFT,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
