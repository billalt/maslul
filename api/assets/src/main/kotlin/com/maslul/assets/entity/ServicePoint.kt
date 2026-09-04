package com.maslul.assets.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.locationtech.jts.geom.Geometry
import java.time.Instant
import java.util.UUID

// One generic geometry column holds a Point (type=POINT) or a LineString (type=SEGMENT) - spec §3.
@Entity
@Table(name = "service_point")
class ServicePoint(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    var tenantId: UUID? = null,

    @Column(name = "external_ref")
    var externalRef: String? = null,

    @Column
    var name: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: ServicePointType = ServicePointType.POINT,

    @Column(nullable = false, columnDefinition = "geometry(Geometry,4326)")
    var geometry: Geometry? = null,

    @Enumerated(EnumType.STRING)
    @Column
    var side: Side? = null,

    @Column(nullable = false)
    var address: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ServicePointStatus = ServicePointStatus.ACTIVE,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
