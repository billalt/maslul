package com.maslul.assets.importing

import com.maslul.assets.entity.ServicePointType
import com.maslul.assets.entity.Side
import org.locationtech.jts.geom.Geometry

// A ServicePoint as parsed from one import row, before duplicate detection or persistence.
data class ImportedServicePoint(
    val externalRef: String?,
    val name: String?,
    val type: ServicePointType,
    val geometry: Geometry,
    val side: Side?,
    val address: String,
)
