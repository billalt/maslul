package com.maslul.sync.entity

// Device-originated event kinds (spec §5).
enum class DeviceEventType {
    SHIFT_START,
    ARRIVED,
    STOP_OUTCOME,
    ISSUE,
    TIP_ARRIVED,
    SHIFT_END,
}
