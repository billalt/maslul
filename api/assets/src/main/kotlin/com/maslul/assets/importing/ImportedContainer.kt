package com.maslul.assets.importing

import com.maslul.assets.entity.BodyType
import com.maslul.assets.entity.ContainerType
import com.maslul.assets.entity.WasteStream

// A Container as parsed from one import row, attached to the row's ImportedServicePoint.
data class ImportedContainer(
    val externalRef: String?,
    val wasteStream: WasteStream,
    val volumeLiters: Int,
    val containerType: ContainerType,
    val requiredBodyType: BodyType,
    val accessNotes: String?,
)
