package id.riko.hub.domain.logic

import java.time.Instant
import java.time.ZoneId

fun widgetTaskCapacity(heightDp: Int, fontScale: Float): Int {
    // Budget: 24dp outer padding + 48dp header + 58dp clock + 48dp date + 64dp per full task row, with 16dp host slack.
    val available = heightDp / fontScale.coerceAtLeast(1f)
    return when {
        available >= 386 -> 3
        available >= 322 -> 2
        else -> 1
    }
}

fun nextDateRefreshAt(now: Long, zone: ZoneId): Long = Instant.ofEpochMilli(now).atZone(zone)
    .toLocalDate().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
