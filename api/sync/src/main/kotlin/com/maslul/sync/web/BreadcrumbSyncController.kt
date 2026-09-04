package com.maslul.sync.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.maslul.identity.tenant.TenantContext
import com.maslul.sync.ingest.BreadcrumbIngestService
import com.maslul.sync.ingest.IncomingBreadcrumb
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID
import java.util.zip.GZIPInputStream

// Reads the raw, gzip-compressed request body itself (rather than a @RequestBody DTO) so
// this endpoint never shares parsing code, a service, or a table with EventSyncController
// (spec §5 - events and breadcrumbs must not share a retry budget or queue).
@RestController
@RequestMapping("/v1/sync/breadcrumbs")
class BreadcrumbSyncController(
    private val breadcrumbIngestService: BreadcrumbIngestService,
    private val tenantContext: TenantContext,
    private val objectMapper: ObjectMapper,
) {

    @PostMapping
    fun sync(request: HttpServletRequest): ResponseEntity<Void> {
        val body = GZIPInputStream(request.inputStream).use { it.readBytes() }
        val batch = objectMapper.readValue(body, BreadcrumbBatchRequest::class.java)

        breadcrumbIngestService.ingest(
            tenantContext.currentTenantId(),
            batch.deviceId,
            batch.points.map(BreadcrumbDto::toIncomingBreadcrumb),
        )

        return ResponseEntity.status(HttpStatus.ACCEPTED).build()
    }
}

data class BreadcrumbBatchRequest(
    val deviceId: UUID,
    val points: List<BreadcrumbDto>,
)

data class BreadcrumbDto(
    val lat: Double,
    val lng: Double,
    val t: Instant,
    val speed: Double?,
    val heading: Double?,
    val accuracy: Double?,
) {
    fun toIncomingBreadcrumb() = IncomingBreadcrumb(lat, lng, t, speed, heading, accuracy)
}
