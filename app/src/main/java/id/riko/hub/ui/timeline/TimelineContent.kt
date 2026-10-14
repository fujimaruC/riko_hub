package id.riko.hub.ui.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.drawscope.Stroke
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
import id.riko.hub.ui.theme.*
import kotlinx.coroutines.delay
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

internal val timelineDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("id-ID"))
private val weekDateFormat = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("id-ID"))
private val dayFormat = DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.forLanguageTag("id-ID"))

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimelineContent(state: TimelineUiState, onWeek: (Long) -> Unit,
                    onCurrent: () -> Unit, onToggleDayMode: () -> Unit, onOpen: (TimelineStop) -> Unit,
                    onAdd: () -> Unit, onRetry: () -> Unit, onSelectDay: ((LocalDate) -> Unit)? = null) {
    val list = rememberLazyListState()
    val reducedMotion = rememberReducedMotion()
    val largeText = LocalDensity.current.fontScale >= 1.3f
    var hasPositioned by remember { mutableStateOf(false) }
    val todayScrollOffset = with(LocalDensity.current) { -Spacing.space3.roundToPx() }
    var positionedWeek by rememberSaveable { mutableStateOf<String?>(null) }
    var cursorRaw by rememberSaveable { mutableStateOf((state.selectedDay ?: state.today).toString()) }
    val cursorDate = runCatching { LocalDate.parse(cursorRaw) }.getOrDefault(state.selectedDay ?: state.today)
    LaunchedEffect(state.selectedDay, state.weekStart) {
        cursorRaw = (state.selectedDay ?: state.today.takeIf {
            it >= state.weekStart && it <= state.weekStart.plusDays(6)
        } ?: state.weekStart).toString()
    }
    LaunchedEffect(state.weekStart, state.selectedDay, state.loading, state.days) {
        val positionKey = state.selectedDay?.toString() ?: state.weekStart.toString()
        if (!state.loading && !state.readFailed && state.days.isNotEmpty() && positionedWeek != positionKey) {
            val todayIndex = state.days.indexOfFirst { it.date == (state.selectedDay ?: state.today) }
            val target = if (todayIndex < 0) 0 else state.days.take(todayIndex).sumOf { 1 + it.stops.size }
            val targetIndex = if (state.days.all { it.stops.isEmpty() }) 0 else target
            if (!hasPositioned || reducedMotion) list.scrollToItem(targetIndex, todayScrollOffset)
            else list.animateScrollToItem(targetIndex, todayScrollOffset)
            hasPositioned = true
            positionedWeek = positionKey
        }
    }
    Column(Modifier.fillMaxSize().testTag("screen-Timeline")) {
        Column(Modifier.fillMaxWidth().padding(horizontal = Spacing.screenGutter),
            verticalArrangement = Arrangement.spacedBy(Spacing.space1)) {
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(Spacing.space1), itemVerticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(stringResource(R.string.plan_river_title), style = MaterialTheme.typography.displayMedium,
                        modifier = Modifier.semantics { heading() })
                    if (!largeText) Text(stringResource(R.string.plan_river_subtitle), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { positionedWeek = null; cursorRaw = state.today.toString(); onCurrent() },
                    enabled = !state.busy, modifier = Modifier.heightIn(min = Spacing.touch)) {
                    Text(stringResource(if (state.dayMode) R.string.timeline_today_label else R.string.timeline_current_week),
                        style = MaterialTheme.typography.labelMedium)
                }
            }
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.space2),
                verticalArrangement = Arrangement.spacedBy(Spacing.space1), itemVerticalAlignment = Alignment.CenterVertically) {
                if (!largeText) Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_event), null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                if (!largeText) Text(stringResource(R.string.plan_river_map), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                FilterChip(selected = !state.dayMode, onClick = { if (state.dayMode) onToggleDayMode() },
                    enabled = !state.busy, label = { Text(stringResource(R.string.timeline_week_mode)) },
                    modifier = Modifier.testTag("timeline-mode-week"))
                FilterChip(selected = state.dayMode, onClick = { if (!state.dayMode) onToggleDayMode() },
                    enabled = !state.busy, label = { Text(stringResource(R.string.timeline_day_mode)) },
                    modifier = Modifier.testTag("timeline-mode-day"))
            }
            if (!largeText) TimelineRiverHorizon(MaterialTheme.colorScheme.primary)
            WeekSwitcher(state, onWeek = { direction ->
                onWeek(direction)
                cursorRaw = cursorDate.plusDays(if (state.dayMode) direction else direction * 7).toString()
            })
        }
        if (state.days.isNotEmpty()) TimelineDayStrip(state, cursorDate, !state.busy) { date ->
            if (onSelectDay != null) onSelectDay(date) else {
                // Compatibility for standalone previews/callers; production uses an absolute date setter.
                val offset = ChronoUnit.DAYS.between(if (state.dayMode) state.selectedDay ?: cursorDate else state.weekStart, date)
                if (!state.dayMode) onToggleDayMode()
                onWeek(offset)
            }
            cursorRaw = date.toString()
            positionedWeek = null
        }
        LazyColumn(state = list, modifier = Modifier.weight(1f).testTag("timeline-list"),
            contentPadding = PaddingValues(horizontal = Spacing.screenGutter, vertical = 0.dp)) {
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
                            TimelineDayIsland(day, cursorDate, state.today)
                        }
                        itemsIndexed(day.stops, key = { _, stop -> "${day.date}-${stop.key}" }) { index, stop ->
                            TimelineStopRow(stop, index, day.stops.size > 6, state.now, state.zone, !state.busy) { onOpen(stop) }
                        }
                    }
                }
            }
        }
        Button(onClick = onAdd, enabled = !state.loading && !state.busy, shape = RikoShapes.pill,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screenGutter, vertical = Spacing.space2).heightIn(min = Spacing.touch)
                .testTag("timeline-add")) { Text(stringResource(R.string.timeline_add_event)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimelineDayIsland(day: TimelineDay, cursorDate: LocalDate, today: LocalDate) {
    val largeText = LocalDensity.current.fontScale >= 1.3f
    val riverX = if (largeText) 12.dp else 60.dp
    val gutter = if (largeText) 32.dp else 76.dp
    val palette = LocalRikoColors.current
    val trackColors = day.stops.map { stop ->
        when (stop.event?.category ?: requireNotNull(stop.task).category) {
            Category.ACADEMIC -> palette.academic.color
            Category.ORGANIZATION -> palette.organization.color
            Category.PERSONAL -> palette.personal.color
            Category.PROJECTS -> palette.projects.color
        }
    }.distinct().ifEmpty { listOf(MaterialTheme.colorScheme.primary) }
    val accent = trackColors.first()
    val selected = day.date == cursorDate
    Box(Modifier.fillMaxWidth().testTag("day-${day.date}")) {
        Canvas(Modifier.matchParentSize()) {
            drawTimeRiver(trackColors, riverX.toPx(), 2.dp.toPx())
            drawCircle(accent, 4.dp.toPx(), androidx.compose.ui.geometry.Offset(riverX.toPx() + 2.dp.toPx(), 28.dp.toPx()))
        }
        Column(Modifier.fillMaxWidth().padding(start = gutter, top = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                Text(day.date.format(dayFormat), style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.semantics { heading() })
                if (day.date == today) Surface(shape = RikoShapes.pill,
                    color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                    Text(stringResource(R.string.timeline_today_marker), style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
            if (selected) HorizontalDivider(color = accent.copy(alpha = .6f), modifier = Modifier.width(32.dp))
            if (day.stops.isEmpty()) Text(stringResource(R.string.plan_river_quiet_day),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TimelineDayStrip(state: TimelineUiState, cursorDate: LocalDate, enabled: Boolean, onSelect: (LocalDate) -> Unit) {
    val largeText = LocalDensity.current.fontScale >= 1.3f
    val selectedInk = MaterialTheme.colorScheme.onPrimary
    val stripInk = MaterialTheme.colorScheme.onSurfaceVariant
    val dates = (0..6).map { state.weekStart.plusDays(it.toLong()) }
    LazyRow(Modifier.fillMaxWidth().testTag("timeline-day-strip"),
        contentPadding = PaddingValues(horizontal = Spacing.screenGutter), horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
        items(dates, key = { it.toString() }) { date ->
            val day = state.days.firstOrNull { it.date == date }
            val selected = date == cursorDate
            val todayLabel = if (date == state.today) stringResource(R.string.timeline_today_marker) else ""
            val agendaLabel = day?.let { stringResource(if (it.stops.isEmpty()) R.string.renaissance_day_empty
                else R.string.renaissance_day_stops, it.stops.size) }.orEmpty()
            Surface(onClick = { onSelect(date) }, enabled = enabled, shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 20.dp, topEnd = 8.dp, bottomEnd = 20.dp, bottomStart = 8.dp),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                border = if (date == state.today) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                    else null,
                modifier = Modifier.widthIn(min = Spacing.touch).heightIn(min = Spacing.touch)
                    .testTag("timeline-day-$date").semantics { this.selected = selected; stateDescription = listOf(todayLabel, agendaLabel).filter { it.isNotEmpty() }.joinToString(", ") }) {
                Column(Modifier.padding(horizontal = Spacing.space3, vertical = Spacing.space2),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.space1)) {
                    Text(date.format(DateTimeFormatter.ofPattern("EE", Locale.forLanguageTag("id-ID"))),
                        style = MaterialTheme.typography.labelSmall)
                    Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.titleSmall)
                    Canvas(Modifier.size(8.dp)) {
                        val dot = if (selected) selectedInk else stripInk
                        if (day != null) {
                            if (day.stops.isNotEmpty()) drawCircle(dot)
                            else drawCircle(dot.copy(alpha = .5f), style = Stroke(1.dp.toPx()))
                        }
                    }
                    if (!largeText) day?.let {
                        Text(stringResource(if (it.stops.isEmpty()) R.string.renaissance_day_empty else R.string.renaissance_day_stops, it.stops.size),
                            style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekSwitcher(state: TimelineUiState, onWeek: (Long) -> Unit) {
    val weekEnd = state.weekStart.plusDays(6)
    val format = if (state.weekStart.year == state.today.year && weekEnd.year == state.today.year) weekDateFormat else timelineDateFormat
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(enabled = !state.busy, onClick = { onWeek(-1) }, modifier = Modifier.size(Spacing.touch).testTag("timeline-previous")) {
            Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_chevron_left), stringResource(
                if (state.dayMode) R.string.timeline_previous_day else R.string.timeline_previous_week))
        }
        Text(if (state.dayMode) state.selectedDay?.format(dayFormat).orEmpty() else
            stringResource(R.string.timeline_week_range, state.weekStart.format(format), weekEnd.format(format)),
            style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f).padding(horizontal = Spacing.space1))
        IconButton(enabled = !state.busy, onClick = { onWeek(1) }, modifier = Modifier.size(Spacing.touch).testTag("timeline-next")) {
            Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_chevron_right), stringResource(
                if (state.dayMode) R.string.timeline_next_day else R.string.timeline_next_week))
        }
    }
}

@Composable
private fun TimelineEmpty() {
    val accent = MaterialTheme.colorScheme.primary
    Column(Modifier.fillMaxWidth().padding(vertical = Spacing.space6),
        verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
        TimelineRiverHorizon(accent)
        Surface(shape = RikoShapes.asymA, color = MaterialTheme.colorScheme.surfaceVariant) {
            Column(Modifier.fillMaxWidth().padding(Spacing.space6), verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                Text(stringResource(R.string.timeline_empty), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.timeline_empty_hint), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
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
