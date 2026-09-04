package com.maslul.sync.web

import com.fasterxml.jackson.databind.JsonNode
import com.maslul.identity.tenant.TenantContext
import com.maslul.sync.entity.DeviceEventType
import com.maslul.sync.ingest.EventIngestService
import com.maslul.sync.ingest.IncomingEvent
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/v1/sync/events")
class EventSyncController(
    private val eventIngestService: EventIngestService,
    private val tenantContext: TenantContext,
) {

    // Spec §5: cap batch size at ~200 so a device offline a full shift (~500 events) drains
    // in a few batches over a marginal connection instead of one oversized request.
    private val maxBatchSize = 200

    @PostMapping
    fun sync(@RequestBody request: EventBatchRequest): ResponseEntity<EventBatchResponse> {
        require(request.events.size <= maxBatchSize) {
            "Batch of ${request.events.size} events exceeds the $maxBatchSize cap"
        }

        val watermark = eventIngestService.ingest(
            tenantContext.currentTenantId(),
            request.deviceId,
            request.events.map(EventDto::toIncomingEvent),
        )

        return ResponseEntity.ok(EventBatchResponse(watermark))
    }
}

data class EventBatchRequest(
    val deviceId: UUID,
    val events: List<EventDto>,
)

data class EventDto(
    val eventId: UUID,
    val deviceSeq: Long,
    val deviceTime: Instant,
    val monotonicMs: Long,
    val type: DeviceEventType,
    val payload: JsonNode,
) {
    fun toIncomingEvent() = IncomingEvent(eventId, deviceSeq, deviceTime, monotonicMs, type, payload.toString())
}

// The device deletes its queue up to this value only (spec §5) - never further.
data class EventBatchResponse(
    val watermark: Long,
)
