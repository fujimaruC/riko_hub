package id.riko.hub.ui.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import id.riko.hub.R
import id.riko.hub.ui.components.eventKindLabel
import id.riko.hub.domain.logic.*
import id.riko.hub.domain.model.Category
import id.riko.hub.ui.tasks.*
import id.riko.hub.ui.theme.*
import java.time.Instant
import java.time.ZoneId

@Composable
fun TimelineStopRow(stop: TimelineStop, index: Int, dense: Boolean, now: Long, zone: ZoneId,
                    enabled: Boolean, onOpen: () -> Unit) {
    val category = stop.event?.category ?: requireNotNull(stop.task).category
    val palette = LocalRikoColors.current
    val categoryAccent = when (category) {
        Category.ACADEMIC -> palette.academic; Category.ORGANIZATION -> palette.organization
        Category.PERSONAL -> palette.personal; Category.PROJECTS -> palette.projects
    }
    val accent = when (stop.kind) {
        TimelineStopKind.DEADLINE -> palette.urgent
        TimelineStopKind.COMPLETED -> palette.personal
        TimelineStopKind.EVENT -> categoryAccent
    }
    val largeText = LocalDensity.current.fontScale >= 1.3f
    val past = (stop.endAt ?: stop.at) < now
    val currentEvent = stop.isCurrentEvent(now)
    val riverColor = categoryAccent.color
    val ink = MaterialTheme.colorScheme.onSurface
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    val time = stopTime(stop, zone)
    val inlineTime = largeText || time.length > 13
    val gutter = if (largeText) 32.dp else 76.dp
    val riverX = if (largeText) 12.dp else 60.dp
    Box(Modifier.fillMaxWidth()) {
        Canvas(Modifier.matchParentSize()) {
            val x = riverX.toPx()
            drawTimeRiver(listOf(riverColor), x, if (dense || largeText) 2.dp.toPx() else 8.dp.toPx())
            val node = Offset(x + if (dense || largeText) 2.dp.toPx() else 8.dp.toPx(), size.height * .5f)
            drawLine(accent.color.copy(alpha = .5f), node, Offset(gutter.toPx() + 4.dp.toPx(), node.y), 1.dp.toPx())
            drawCircle(accent.color.copy(alpha = .14f), if (currentEvent) 12.dp.toPx() else 8.dp.toPx(), node)
            if (stop.kind == TimelineStopKind.DEADLINE) {
                val radius = 4.dp.toPx()
                drawPath(Path().apply {
                    moveTo(node.x, node.y - radius); lineTo(node.x + radius, node.y)
                    lineTo(node.x, node.y + radius); lineTo(node.x - radius, node.y); close()
                }, accent.color)
            } else {
                drawCircle(accent.color, 4.dp.toPx(), node)
                if (stop.kind == TimelineStopKind.COMPLETED) drawPath(Path().apply {
                    moveTo(node.x - 3.dp.toPx(), node.y)
                    lineTo(node.x, node.y + 3.dp.toPx()); lineTo(node.x + 5.dp.toPx(), node.y - 4.dp.toPx())
                }, ink, style = Stroke(1.5.dp.toPx()))
            }
        }
        if (!inlineTime) Text(time, style = MaterialTheme.typography.labelMedium, color = secondary,
            modifier = Modifier.width(48.dp).padding(top = 20.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
        Surface(onClick = onOpen, enabled = enabled,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 8.dp, topEnd = 24.dp, bottomEnd = 8.dp, bottomStart = 24.dp),
            color = if (currentEvent) accent.container else MaterialTheme.colorScheme.surfaceVariant,
            border = androidx.compose.foundation.BorderStroke(if (currentEvent) 1.5.dp else 1.dp,
                if (currentEvent) accent.color else MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth().padding(start = gutter,
                end = if (!dense && !largeText && index % 2 == 1) 8.dp else 0.dp,
                top = 4.dp, bottom = 8.dp).heightIn(min = Spacing.touch).testTag("timeline-stop-${stop.key}")) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = if (dense) 8.dp else 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Canvas(Modifier.width(3.dp).height(32.dp)) {
                    drawLine(categoryAccent.color.copy(alpha = if (past) .6f else 1f), Offset(size.width / 2, 0f),
                        Offset(size.width / 2, size.height), size.width, androidx.compose.ui.graphics.StrokeCap.Round)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (inlineTime) Text(time, style = MaterialTheme.typography.labelLarge, color = secondary)
                    Text(stop.event?.title ?: requireNotNull(stop.task).title,
                        style = MaterialTheme.typography.titleSmall, color = ink)
                    val kind = stringResource(when (stop.kind) {
                        TimelineStopKind.EVENT -> requireNotNull(stop.event).kind.eventKindLabel()
                        TimelineStopKind.DEADLINE -> R.string.timeline_deadline_stop
                        TimelineStopKind.COMPLETED -> R.string.timeline_completed_stop
                    })
                    Text(stringResource(R.string.timeline_stop_metadata, kind, stringResource(category.label())),
                        style = MaterialTheme.typography.bodySmall, color = secondary)
                    if (currentEvent) Text(stringResource(R.string.timeline_now),
                        style = MaterialTheme.typography.labelMedium, color = ink)
                    stop.event?.location?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = secondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun stopTime(stop: TimelineStop, zone: ZoneId): String {
    if (stop.kind == TimelineStopKind.DEADLINE && stop.task?.hasDeadlineTime == false)
        return stringResource(R.string.timeline_date_only)
    val start = Instant.ofEpochMilli(stop.event?.startAt ?: stop.at).atZone(zone)
    val end = (stop.event?.endAt ?: stop.endAt)?.let { Instant.ofEpochMilli(it).atZone(zone) }
    if (end == null || end == start) return start.format(taskTimeFormat)
    return if (start.toLocalDate() == end.toLocalDate()) stringResource(R.string.timeline_time_range,
        start.format(taskTimeFormat), end.format(taskTimeFormat))
    else stringResource(R.string.timeline_time_range,
        stringResource(R.string.timeline_date_time, start.format(timelineDateFormat), start.format(taskTimeFormat)),
        stringResource(R.string.timeline_date_time, end.format(timelineDateFormat), end.format(taskTimeFormat)))
}
