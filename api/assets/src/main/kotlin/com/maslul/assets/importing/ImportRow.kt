package com.maslul.assets.importing

// One CSV row or GeoJSON feature, after parsing. A malformed row is never dropped or thrown -
// it comes back with servicePoint == null and parseError set, so the caller can still report it.
data class ImportRow(
    val rowNumber: Int,
    val servicePoint: ImportedServicePoint?,
    val containers: List<ImportedContainer>,
    val parseError: String?,
)
