package com.maslul.assets.importing

import java.io.InputStream

interface ServicePointImportParser {
    fun supports(format: ImportFormat): Boolean

    // Never throws on a single bad row/feature - see ImportRow.parseError.
    fun parse(input: InputStream): List<ImportRow>
}
