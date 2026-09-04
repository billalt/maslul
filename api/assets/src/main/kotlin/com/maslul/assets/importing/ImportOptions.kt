package com.maslul.assets.importing

// Per-tenant configurable at import time - see ServicePointDuplicateDetector.
data class ImportOptions(
    val duplicateRadiusMeters: Double = DEFAULT_DUPLICATE_RADIUS_METERS,
    val duplicateMode: DuplicateMode = DuplicateMode.SKIP,
) {
    companion object {
        const val DEFAULT_DUPLICATE_RADIUS_METERS = 15.0
    }
}
