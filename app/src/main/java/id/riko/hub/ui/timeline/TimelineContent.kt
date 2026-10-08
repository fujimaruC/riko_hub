package id.riko.hub.ui.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import id.riko.hub.R
import id.riko.hub.domain.logic.*
import id.riko.hub.domain.model.*
import id.riko.hub.navigation.Destination
import id.riko.hub.ui.components.ReturnHomeHandle
import id.riko.hub.ui.theme.*
import kotlinx.coroutines.delay
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

internal val timelineDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("id-ID"))
private val weekDateFormat = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("id-ID"))
private val dayFormat = DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.forLanguageTag("id-ID"))

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimelineContent(state: TimelineUiState, onNavigate: (Destination) -> Unit, onWeek: (Long) -> Unit,
                    onCurrent: () -> Unit, onToggleDayMode: () -> Unit, onOpen: (TimelineStop) -> Unit,
                    onAdd: () -> Unit, onRetry: () -> Unit) {
    val list = rememberLazyListState()
    val todayScrollOffset = with(LocalDensity.current) { -Spacing.space3.roundToPx() }
    var positionedWeek by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(state.weekStart, state.selectedDay, state.loading, state.days) {
        val positionKey = state.selectedDay?.toString() ?: state.weekStart.toString()
        if (!state.loading && !state.readFailed && state.days.isNotEmpty() && positionedWeek != positionKey) {
            val todayIndex = state.days.indexOfFirst { it.date == state.today }
            val target = if (todayIndex < 0) 0 else state.days.take(todayIndex).sumOf { 1 + it.stops.size }
            list.scrollToItem(if (state.days.all { it.stops.isEmpty() }) 0 else target, todayScrollOffset)
            positionedWeek = positionKey
        }
    }
    Column(Modifier.fillMaxSize().testTag("screen-Timeline")) {
        ReturnHomeHandle(Destination.Timeline, onNavigate, compact = true)
        Column(Modifier.fillMaxWidth().padding(horizontal = Spacing.space5), verticalArrangement = Arrangement.spacedBy(Spacing.space1)) {
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(Spacing.space1), itemVerticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.timeline), style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.semantics { heading() })
                TextButton(onClick = { positionedWeek = null; onCurrent() }, modifier = Modifier.heightIn(min = Spacing.touch)) {
                    Text(stringResource(if (state.dayMode) R.string.timeline_today_label else R.string.timeline_current_week),
                        style = MaterialTheme.typography.labelMedium)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                FilterChip(selected = !state.dayMode, onClick = { if (state.dayMode) onToggleDayMode() },
                    label = { Text(stringResource(R.string.timeline_week_mode)) }, modifier = Modifier.testTag("timeline-mode-week"))
                Spacer(Modifier.width(Spacing.space2))
                FilterChip(selected = state.dayMode, onClick = { if (!state.dayMode) onToggleDayMode() },
                    label = { Text(stringResource(R.string.timeline_day_mode)) }, modifier = Modifier.testTag("timeline-mode-day"))
            }
            WeekSwitcher(state, onWeek)
        }
        LazyColumn(state = list, modifier = Modifier.weight(1f).testTag("timeline-list"),
            contentPadding = PaddingValues(horizontal = Spacing.space5, vertical = Spacing.space3)) {
            when {
                state.readFailed -> item {
                    Text(stringResource(R.string.read_failed))
                    TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
                }
                state.loading -> item { TimelineLoading() }
                state.days.all { it.stops.isEmpty() } -> item(key = "timeline-empty") { TimelineEmpty() }
                else -> {
                    state.days.forEach { day ->
                        item(key = "day-${day.date}") {
                            Column(Modifier.fillMaxWidth().padding(vertical = Spacing.space2).testTag("day-${day.date}")) {
                                FlowRow(Modifier.fillMaxWidth(), itemVerticalAlignment = Alignment.CenterVertically,
                                    verticalArrangement = Arrangement.spacedBy(Spacing.space1)) {
                                    Text(day.date.format(dayFormat), style = MaterialTheme.typography.titleSmall,
                                        color = if (day.date == state.today) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.semantics { heading() })
                                    if (day.date == state.today) {
                                        Surface(shape = RikoShapes.pill, color = LocalRikoColors.current.organization.container,
                                            modifier = Modifier.padding(start = Spacing.space2)) {
                                            Text(stringResource(R.string.timeline_today_marker), style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = Spacing.space2, vertical = Spacing.space1))
                                        }
                                    }
                                }
                                HorizontalDivider(Modifier.padding(top = Spacing.space2), color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                        itemsIndexed(day.stops, key = { _, stop -> "${day.date}-${stop.key}" }) { index, stop ->
                            TimelineStopRow(stop, index, day.stops.size > 6, state.now, state.zone, !state.busy) { onOpen(stop) }
                        }
                    }
                }
            }
        }
        Button(onClick = onAdd, enabled = !state.loading && !state.busy, shape = RikoShapes.pill,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.space5, vertical = Spacing.space2).heightIn(min = Spacing.touch)
                .testTag("timeline-add")) { Text(stringResource(R.string.timeline_add_event)) }
    }
}

@Composable
private fun WeekSwitcher(state: TimelineUiState, onWeek: (Long) -> Unit) {
    val weekEnd = state.weekStart.plusDays(6)
    val format = if (state.weekStart.year == state.today.year && weekEnd.year == state.today.year) weekDateFormat else timelineDateFormat
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onWeek(-1) }, modifier = Modifier.size(Spacing.touch).testTag("timeline-previous")) {
            Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_chevron_left), stringResource(
                if (state.dayMode) R.string.timeline_previous_day else R.string.timeline_previous_week))
        }
        Text(if (state.dayMode) state.selectedDay?.format(dayFormat).orEmpty() else
            stringResource(R.string.timeline_week_range, state.weekStart.format(format), weekEnd.format(format)),
            style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f).padding(horizontal = Spacing.space1))
        IconButton(onClick = { onWeek(1) }, modifier = Modifier.size(Spacing.touch).testTag("timeline-next")) {
            Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_chevron_right), stringResource(
                if (state.dayMode) R.string.timeline_next_day else R.string.timeline_next_week))
        }
    }
}

@Composable
private fun TimelineEmpty() {
    val accent = LocalRikoColors.current.organization.color
    Surface(shape = RikoShapes.asymA, color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.fillMaxWidth().padding(Spacing.space6), verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
            Canvas(Modifier.size(Spacing.space12)) {
                drawCircle(accent, radius = size.width*.25f)
                drawLine(accent, androidx.compose.ui.geometry.Offset(0f,size.height),
                    androidx.compose.ui.geometry.Offset(size.width,size.height*.75f), strokeWidth = Spacing.pathStroke.toPx())
            }
            Text(stringResource(R.string.timeline_empty), style = MaterialTheme.typography.displayMedium)
            Text(stringResource(R.string.timeline_empty_hint))
        }
    }
}

@Composable
private fun TimelineLoading() {
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(150); show = true }
    if (show) Column(verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
        repeat(3) { Surface(Modifier.fillMaxWidth().height(Spacing.row), shape = RikoShapes.wide,
            color = MaterialTheme.colorScheme.surfaceVariant) {} }
    }
}

@Preview @Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, fontScale = 2f)
@Composable
private fun TimelinePreview() {
    val zone = ZoneId.of("Asia/Makassar")
    val today = LocalDate.of(2026,10,6)
    val at = today.atTime(9,0).atZone(zone).toInstant().toEpochMilli()
    val event = Event(id = 1, title = "Diskusi kelompok", startAt = at, endAt = at+7_200_000)
    val days = buildTimeline(emptyList(), listOf(event), timelineWeek(today), zone)
    RikoHubTheme { Surface { TimelineContent(TimelineUiState(timelineWeek(today), today = today, now = at, zone = zone,
        days = days, loading = false), {}, {}, {}, {}, {}, {}, {}) } }
}
