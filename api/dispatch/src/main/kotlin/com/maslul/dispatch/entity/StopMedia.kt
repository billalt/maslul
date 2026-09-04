package com.maslul.dispatch.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

// media_ids[] in spec §3, modeled as one row per photo (own id/tenant_id/created_at, like
// every other table here) rather than an array column - photos are a first-class artifact,
// not an attachment (spec §3), and this shape is what a future media/S3 join needs anyway.
@Entity
@Table(name = "stop_media")
class StopMedia(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    var tenantId: UUID? = null,

    @Column(name = "stop_id", nullable = false)
    var stopId: UUID? = null,

    // The S3 object / media record id - media storage itself isn't modeled by any module yet.
    @Column(name = "media_id", nullable = false)
    var mediaId: UUID? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
)
