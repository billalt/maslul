package com.maslul.assets.importing

import java.io.InputStream
import java.util.UUID

interface ServicePointImportService {
    fun importFile(tenantId: UUID, format: ImportFormat, input: InputStream, options: ImportOptions): ImportReport
}
