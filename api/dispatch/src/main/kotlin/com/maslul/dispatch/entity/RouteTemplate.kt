package com.maslul.dispatch.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

// Schedule rule (spec §3: "reusable ordered list of ServicePoints + schedule rule") is not
// modeled yet - the real thing is a per-tenant/per-zone calendar engine (spec §9) that has to
// handle Shabbat, Jewish holidays, and Muslim/Christian holidays, and is its own task.
// scheduleHint is a human-readable placeholder only; nothing interprets it.
@Entity
@Table(name = "route_template")
class RouteTemplate(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    var tenantId: UUID? = null,

    @Column(nullable = false)
    var name: String = "",

    // assets.Zone - referenced by id only, no cross-module entity import (spec §4).
    @Column(name = "zone_id")
    var zoneId: UUID? = null,

    @Column(nullable = false)
    var active: Boolean = true,

    @Column(name = "schedule_hint")
    var scheduleHint: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
