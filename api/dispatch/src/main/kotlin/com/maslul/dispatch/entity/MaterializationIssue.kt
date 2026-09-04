package com.maslul.dispatch.entity

// Set at materialization time when a template stop had a known data problem. The stop is
// still created either way - decided during dispatch skeleton review: materialize and flag,
// so the dispatcher sees the problem on the board instead of the whole route failing to
// publish, or the stop silently vanishing. SERVICE_POINT_INACTIVE takes precedence over
// BODY_TYPE_MISMATCH when both apply - see DefaultRouteInstanceMaterializationService.
enum class MaterializationIssue {
    SERVICE_POINT_INACTIVE,
    BODY_TYPE_MISMATCH,
}
