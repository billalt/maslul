package com.maslul.sync.entity

// ACCEPTED events are contiguous with the device's watermark and applied. QUARANTINED
// events arrived ahead of a gap in device_seq (spec §5: "reject and quarantine gaps rather
// than applying out of order") and are held, never discarded, until the gap fills, at which
// point they flip to ACCEPTED too.
//
// Deliberately kept binary rather than adding a third PROMOTED value: DeviceEvent.
// quarantinedAt carries the forensic trail (this row was once out of order) without forcing
// every "is this durably applied" check elsewhere to match a growing set of statuses.
enum class DeviceEventStatus {
    ACCEPTED,
    QUARANTINED,
}
