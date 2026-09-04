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
@Table(name = "container")
class Container(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    var tenantId: UUID? = null,

    @Column(name = "service_point_id", nullable = false)
    var servicePointId: UUID? = null,

    @Column(name = "external_ref")
    var externalRef: String? = null,

    @Column(name = "rfid_tag")
    var rfidTag: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "waste_stream", nullable = false)
    var wasteStream: WasteStream = WasteStream.GENERAL,

    @Column(name = "volume_liters", nullable = false)
    var volumeLiters: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "container_type", nullable = false)
    var containerType: ContainerType = ContainerType.WHEELIE,

    @Enumerated(EnumType.STRING)
    @Column(name = "required_body_type", nullable = false)
    var requiredBodyType: BodyType = BodyType.REAR_LOADER,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ContainerStatus = ContainerStatus.ACTIVE,

    @Column(name = "access_notes")
    var accessNotes: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
