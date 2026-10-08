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
    val accent = when (category) {
        Category.ACADEMIC -> palette.academic; Category.ORGANIZATION -> palette.organization
        Category.PERSONAL -> palette.personal; Category.PROJECTS -> palette.projects
    }
    val straight = dense || LocalDensity.current.fontScale >= 1.3f
    val past = (stop.endAt ?: stop.at) < now
    val today = Instant.ofEpochMilli(stop.at).atZone(zone).toLocalDate() == Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val tonal = today && !past
    Box(Modifier.fillMaxWidth()) {
        Canvas(Modifier.matchParentSize()) {
            val center = Spacing.space3.toPx()
            val nodeX = center + if (straight) 0f else if (index % 2 == 0) Spacing.space1.toPx() else -Spacing.space1.toPx()
            val middle = size.height * .5f
            val path = Path().apply {
                moveTo(center, 0f)
                if (straight) lineTo(center, size.height) else {
                    cubicTo(center, middle*.5f, nodeX, middle*.5f, nodeX, middle)
                    cubicTo(nodeX, middle*1.5f, center, middle*1.5f, center, size.height)
                }
            }
            drawPath(path, accent.color, style = Stroke(Spacing.pathStroke.toPx()))
            if (stop.kind == TimelineStopKind.COMPLETED) {
                drawPath(Path().apply {
                    moveTo(nodeX-Spacing.space1.toPx(), middle)
                    lineTo(nodeX, middle+Spacing.space1.toPx())
                    lineTo(nodeX+Spacing.space2.toPx(), middle-Spacing.space1.toPx())
                }, accent.color, style = Stroke(Spacing.pathStroke.toPx()))
            } else drawCircle(accent.color, Spacing.space1.toPx(), Offset(nodeX,middle))
        }
        Surface(onClick = onOpen, enabled = enabled, shape = RikoShapes.wide,
            color = if (tonal) accent.container else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
                .padding(start = Spacing.space8 + if (!straight && index % 2 == 0) Spacing.space2 else Spacing.space1,
                    end = if (!straight && index % 2 != 0) Spacing.space2 else Spacing.space1,
                    top = Spacing.space1, bottom = Spacing.space2)
                .heightIn(min = Spacing.touch).testTag("timeline-stop-${stop.key}")) {
            Column(Modifier.padding(Spacing.space4), verticalArrangement = Arrangement.spacedBy(Spacing.space1)) {
                val ink = if (past) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                val secondary = if (tonal) ink else MaterialTheme.colorScheme.onSurfaceVariant
                Text(stop.event?.title ?: requireNotNull(stop.task).title, style = MaterialTheme.typography.titleMedium, color = ink)
                Text(stopTime(stop, zone), style = MaterialTheme.typography.bodyMedium, color = secondary)
                val kind = stringResource(when (stop.kind) {
                    TimelineStopKind.EVENT -> requireNotNull(stop.event).kind.eventKindLabel()
                    TimelineStopKind.DEADLINE -> R.string.timeline_deadline_stop
                    TimelineStopKind.COMPLETED -> R.string.timeline_completed_stop
                })
                Text(stringResource(R.string.timeline_stop_metadata, kind, stringResource(category.label())),
                    style = MaterialTheme.typography.labelMedium, color = secondary)
                stop.event?.location?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = secondary) }
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
