package id.riko.hub.domain.logic

import java.time.Instant
import java.time.ZoneId

fun widgetTaskCapacity(widthDp: Int, heightDp: Int, fontScale: Float): Int {
    val scale = fontScale.coerceAtLeast(1f)
    val compact = widthDp < 300
    val dateMinHeight = if (compact) 60 else 68
    val dateTextSp = if (compact) 19 + 16 else 21 + 17
    val base = 24 + 20 + dateMinHeight + maxOf(dateMinHeight.toFloat(), dateTextSp * 1.364f * scale)
    // Rows budget 84dp primary, 76dp secondary, plus 20dp launcher slack.
    return when {
        heightDp >= base + (84 + 2 * 76) * scale + 20 -> 3
        heightDp >= base + (84 + 76) * scale + 20 -> 2
        else -> 1
    }
}

fun widgetShowsMiyu(widthDp: Int, fontScale: Float): Boolean {
    val compact = widthDp < 300
    val remainingDateWidth = widthDp - 24 - if (compact) 56 else 68
    val requiredDateWidth = if (compact) 156 else 166
    return remainingDateWidth >= requiredDateWidth * fontScale.coerceAtLeast(1f)
}

fun nextDateRefreshAt(now: Long, zone: ZoneId): Long = Instant.ofEpochMilli(now).atZone(zone)
    .toLocalDate().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
