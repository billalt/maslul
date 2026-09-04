package com.maslul.assets.importing

import com.maslul.assets.repository.ContainerRepository
import com.maslul.assets.repository.ServicePointRepository
import org.springframework.stereotype.Service
import java.io.InputStream
import java.util.UUID

@Service
class DefaultServicePointImportService(
    private val parsers: List<ServicePointImportParser>,
    private val duplicateDetector: ServicePointDuplicateDetector,
    private val servicePointRepository: ServicePointRepository,
    private val containerRepository: ContainerRepository,
) : ServicePointImportService {

    override fun importFile(tenantId: UUID, format: ImportFormat, input: InputStream, options: ImportOptions): ImportReport {
        TODO("not yet implemented - awaiting review of the skeleton")
    }
}
