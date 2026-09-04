package com.maslul.assets.importing

import org.springframework.stereotype.Component
import java.io.InputStream

@Component
class CsvServicePointImportParser : ServicePointImportParser {

    override fun supports(format: ImportFormat): Boolean = format == ImportFormat.CSV

    override fun parse(input: InputStream): List<ImportRow> {
        TODO("not yet implemented - awaiting review of the skeleton")
    }
}
