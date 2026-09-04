package com.maslul.dispatch.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

// planned_* and actual_* columns live on the same row deliberately (spec §3) - every
// plan-vs-actual report is a comparison of those two column groups on one Stop.
@Entity
@Table(name = "stop")
class Stop(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(name = "tenant_id", nullable = false)
    var tenantId: UUID? = null,

    @Column(name = "route_instance_id", nullable = false)
    var routeInstanceId: UUID? = null,

    @Column(nullable = false)
    var sequence: Int = 0,

    // assets.ServicePoint - referenced by id only, no cross-module entity import (spec §4).
    @Column(name = "service_point_id", nullable = false)
    var servicePointId: UUID? = null,

    @Column(name = "planned_arrival")
    var plannedArrival: Instant? = null,

    @Column(name = "planned_duration_s")
    var plannedDurationS: Int? = null,

    @Column(name = "actual_arrival")
    var actualArrival: Instant? = null,

    @Column(name = "actual_departure")
    var actualDeparture: Instant? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var outcome: StopOutcome = StopOutcome.PENDING,

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome_reason")
    var outcomeReason: OutcomeReason? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "recorded_by")
    var recordedBy: RecordedBy? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "materialization_issue")
    var materializationIssue: MaterializationIssue? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
