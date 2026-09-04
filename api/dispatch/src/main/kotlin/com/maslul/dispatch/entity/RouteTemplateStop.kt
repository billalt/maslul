package com.maslul.dispatch.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

// The template's ordered stop list (spec §3). Materializing a RouteInstance copies this
// sequence verbatim - no optimizer in V1 (spec §0).
@Entity
@Table(name = "route_template_stop")
class RouteTemplateStop(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    var tenantId: UUID? = null,

    @Column(name = "route_template_id", nullable = false)
    var routeTemplateId: UUID? = null,

    // assets.ServicePoint - referenced by id only, no cross-module entity import (spec §4).
    @Column(name = "service_point_id", nullable = false)
    var servicePointId: UUID? = null,

    @Column(nullable = false)
    var sequence: Int = 0,

    @Column(name = "planned_duration_s")
    var plannedDurationS: Int? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
