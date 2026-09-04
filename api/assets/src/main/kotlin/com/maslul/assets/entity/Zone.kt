package com.maslul.assets.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.locationtech.jts.geom.Geometry
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "zone")
class Zone(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    var tenantId: UUID? = null,

    @Column(nullable = false)
    var name: String = "",

    // Nullable - a zone can exist before its geofence is drawn (spec: routes are "drawn manually").
    @Column(columnDefinition = "geometry(Geometry,4326)")
    var boundary: Geometry? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
