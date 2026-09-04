package com.maslul.sync.ingest

import com.maslul.sync.entity.DeviceEvent
import com.maslul.sync.entity.DeviceEventStatus
import com.maslul.sync.entity.SyncCursor
import com.maslul.sync.repository.DeviceEventRepository
import com.maslul.sync.repository.SyncCursorRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

// Assumes at most one in-flight batch per device at a time, which matches how a device
// actually behaves: it waits for one batch's response before sending the next (spec §5).
// Concurrent overlapping batches for the same device_id are not guarded against here.
@Service
class DefaultEventIngestService(
    private val deviceEventRepository: DeviceEventRepository,
    private val syncCursorRepository: SyncCursorRepository,
) : EventIngestService {

    @Transactional
    override fun ingest(tenantId: UUID, deviceId: UUID, events: List<IncomingEvent>): Long {
        val cursor = syncCursorRepository.findByTenantIdAndDeviceId(tenantId, deviceId)
            ?: SyncCursor(tenantId = tenantId, deviceId = deviceId, lastDeviceSeq = 0)
        var watermark = cursor.lastDeviceSeq

        // Spec §5: process in device_seq order within a batch.
        for (event in events.sortedBy { it.deviceSeq }) {
            // event_id, not device_seq, is the idempotency key - this check must run before
            // any device_seq-based branching below. A resent event that was already
            // accepted (including one already cascade-promoted out of quarantine by an
            // earlier event in this very batch) must short-circuit here: falling through to
            // the ordering logic would either double-count it or try to re-insert it and
            // blow up on the (tenant_id, event_id) unique constraint.
            val existing = deviceEventRepository.findByTenantIdAndEventId(tenantId, event.eventId)
            if (existing != null) {
                continue
            }

            val now = Instant.now()
            when {
                event.deviceSeq == watermark + 1 -> {
                    deviceEventRepository.save(event.toDeviceEvent(tenantId, deviceId, DeviceEventStatus.ACCEPTED, now))
                    watermark += 1
                    watermark = promoteContiguousQuarantined(tenantId, deviceId, watermark)
                }
                event.deviceSeq > watermark + 1 -> {
                    // A gap ahead of the watermark - quarantine rather than apply out of
                    // order (spec §5).
                    val quarantined = event.toDeviceEvent(tenantId, deviceId, DeviceEventStatus.QUARANTINED, now)
                    quarantined.quarantinedAt = now
                    deviceEventRepository.save(quarantined)
                }
                else -> {
                    // device_seq <= watermark but event_id is unseen - not a gap-fill
                    // (nothing forward of the watermark to fill), and not a replay (event_id
                    // already ruled that out above). Most likely a device that reinstalled
                    // or reset its counter and reused an old device_seq value under a new
                    // event_id. Accept it on its own terms; it doesn't advance the watermark
                    // because it doesn't extend the contiguous run.
                    deviceEventRepository.save(event.toDeviceEvent(tenantId, deviceId, DeviceEventStatus.ACCEPTED, now))
                }
            }
        }

        cursor.lastDeviceSeq = watermark
        cursor.updatedAt = Instant.now()
        syncCursorRepository.save(cursor)
        return watermark
    }

    // Promotes any already-quarantined events that are now contiguous with the watermark,
    // in device_seq order - handles both a batch that only resends the gap-filler (the rest
    // of the run was already quarantined from an earlier batch) and one that resends the
    // whole un-acked tail (in which case the idempotency check above skips the redundant
    // re-processing of those same events later in this loop).
    private fun promoteContiguousQuarantined(tenantId: UUID, deviceId: UUID, startWatermark: Long): Long {
        var watermark = startWatermark
        while (true) {
            val next = deviceEventRepository.findByTenantIdAndDeviceIdAndStatusAndDeviceSeq(
                tenantId, deviceId, DeviceEventStatus.QUARANTINED, watermark + 1,
            ) ?: break
            next.status = DeviceEventStatus.ACCEPTED
            // quarantinedAt is left as-is - the forensic trail that this event was once held.
            deviceEventRepository.save(next)
            watermark += 1
        }
        return watermark
    }

    private fun IncomingEvent.toDeviceEvent(
        tenantId: UUID,
        deviceId: UUID,
        status: DeviceEventStatus,
        serverReceivedAt: Instant,
    ) = DeviceEvent(
        tenantId = tenantId,
        deviceId = deviceId,
        eventId = eventId,
        deviceSeq = deviceSeq,
        eventType = type,
        deviceTime = deviceTime,
        monotonicMs = monotonicMs,
        payload = payload,
        status = status,
        serverReceivedAt = serverReceivedAt,
    )
}
