package com.maslul.assets.importing

import java.util.UUID

data class ImportRowResult(
    val rowNumber: Int,
    val outcome: ImportRowOutcome,
    val servicePointId: UUID?,
    val containerIds: List<UUID>,
    val message: String?,
)
