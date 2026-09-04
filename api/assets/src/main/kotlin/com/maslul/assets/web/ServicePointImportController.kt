package com.maslul.assets.web

import com.maslul.assets.importing.ImportFormat
import com.maslul.assets.importing.ImportReport
import com.maslul.assets.importing.ServicePointImportService
import com.maslul.identity.tenant.TenantContext
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/v1/service-points")
class ServicePointImportController(
    private val importService: ServicePointImportService,
    private val tenantContext: TenantContext,
) {

    @PostMapping("/import")
    fun import(
        @RequestParam format: ImportFormat,
        @RequestParam file: MultipartFile,
    ): ImportReport =
        importService.importFile(tenantContext.currentTenantId(), format, file.inputStream)
}
