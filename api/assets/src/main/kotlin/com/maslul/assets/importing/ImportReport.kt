package com.maslul.assets.importing

data class ImportReport(
    val format: ImportFormat,
    val totalRows: Int,
    val created: Int,
    val updated: Int,
    val duplicateSkipped: Int,
    val malformed: Int,
    val rows: List<ImportRowResult>,
)
