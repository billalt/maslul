package com.maslul.dispatch.entity

// spec §3 - the enum that decides whether people trust the product.
enum class StopOutcome {
    PENDING,
    COLLECTED,
    BLOCKED,
    NOT_PRESENTED,
    INACCESSIBLE,
    SKIPPED,
    PARTIAL,
}
