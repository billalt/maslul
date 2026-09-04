package com.maslul.sync.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

// event_id is the idempotency key (spec §5) - devices replay batches constantly because
// they lose connectivity mid-POST and cannot tell whether the write landed. device_seq
// orders events per device; a gap quarantines the event rather than applying it out of
// order. Never shares a table, queue, or retry budget with breadcrumbs (spec §5).
@Entity
@Table(name = "device_event")
class DeviceEvent(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    var tenantId: UUID? = null,

    @Column(name = "device_id", nullable = false)
    var deviceId: UUID? = null,

    @Column(name = "event_id", nullable = false)
    var eventId: UUID? = null,

    @Column(name = "device_seq", nullable = false)
    var deviceSeq: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    var eventType: DeviceEventType = DeviceEventType.SHIFT_START,

    // Untrusted device wall clock (spec §5) - recorded as-is, never used for SLA timing.
    @Column(name = "device_time", nullable = false)
    var deviceTime: Instant = Instant.now(),

    // SystemClock.elapsedRealtime() on the device - trusted for durations, not wall time.
    @Column(name = "monotonic_ms", nullable = false)
    var monotonicMs: Long = 0,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    var payload: String = "{}",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: DeviceEventStatus = DeviceEventStatus.QUARANTINED,

    // Set once, when this row is first inserted as QUARANTINED - left untouched when it's
    // later promoted to ACCEPTED. Null means this event was always in order. The forensic
    // trail for a disputed stop outcome: "was this ever out of order, and when did it arrive."
    @Column(name = "quarantined_at")
    var quarantinedAt: Instant? = null,

    // Stamped once, on first ingestion (quarantined or accepted) - never overwritten, so a
    // replayed batch is provably a no-op (spec §5, idempotency).
    @Column(name = "server_received_at", nullable = false)
    var serverReceivedAt: Instant = Instant.now(),

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
