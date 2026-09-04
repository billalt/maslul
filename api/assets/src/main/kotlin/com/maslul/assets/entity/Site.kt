package com.maslul.assets.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.locationtech.jts.geom.Point
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "site")
class Site(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    var tenantId: UUID? = null,

    @Column(nullable = false)
    var name: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: SiteType = SiteType.DEPOT,

    @Column(nullable = false, columnDefinition = "geometry(Point,4326)")
    var location: Point? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
