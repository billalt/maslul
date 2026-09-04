package com.maslul.dispatch.entity

// spec §3 - required whenever outcome != COLLECTED (except PENDING, which hasn't happened yet).
enum class OutcomeReason {
    BLOCKED_BY_VEHICLE,
    BLOCKED_BY_CONSTRUCTION,
    BIN_NOT_PRESENTED,
    BIN_MISSING,
    BIN_DAMAGED,
    ACCESS_ROAD_CLOSED,
    OVERFLOW_LEFT_BEHIND,
    TRUCK_FULL,
    SAFETY_HAZARD,
}
