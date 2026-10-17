package id.riko.hub.ui.timeline

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.riko.hub.R
import id.riko.hub.domain.logic.TimelineDay
import id.riko.hub.domain.logic.TimelineStop
import id.riko.hub.domain.logic.buildTimeline
import id.riko.hub.domain.logic.timelineWeek
import id.riko.hub.domain.model.Category
import id.riko.hub.domain.model.Event
import id.riko.hub.ui.theme.ChefButter
import id.riko.hub.ui.theme.ChefCanvas
import id.riko.hub.ui.theme.ChefCoral
import id.riko.hub.ui.theme.ChefGround
import id.riko.hub.ui.theme.ChefInk
import id.riko.hub.ui.theme.ChefLime
import id.riko.hub.ui.theme.ChefLine
import id.riko.hub.ui.theme.ChefMuted
import id.riko.hub.ui.theme.ChefPanel
import id.riko.hub.ui.theme.ChefRaised
import id.riko.hub.ui.theme.ChefSage
import id.riko.hub.ui.theme.LocalRikoColors
import id.riko.hub.ui.theme.NotoCondensedBlack
import id.riko.hub.ui.theme.RikoHubTheme
import id.riko.hub.ui.theme.RikoShapes
import id.riko.hub.ui.theme.Spacing
import id.riko.hub.ui.theme.rememberReducedMotion
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

internal val timelineDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("id-ID"))
private val weekDateFormat = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("id-ID"))
private val dayFormat = DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.forLanguageTag("id-ID"))

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimelineContent(
    state: TimelineUiState,
    onWeek: (Long) -> Unit,
    onCurrent: () -> Unit,
    onToggleDayMode: () -> Unit,
    onOpen: (TimelineStop) -> Unit,
    onAdd: () -> Unit,
    onRetry: () -> Unit,
    onSelectDay: ((LocalDate) -> Unit)? = null,
) {
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

    Column(
        Modifier
            .fillMaxSize()
            .background(ChefSage)
            .testTag("screen-Timeline"),
    ) {
        // TOP STAGE (Sage Background with 24h Radial Dial)
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Screen Header Bar: Back, Title, Current Week Dropdown
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "OPERASIONAL JADWAL",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = ChefGround.copy(alpha = 0.7f),
                    )
                    Text(
                        text = "OPERATIONAL TIMING",
                        fontFamily = NotoCondensedBlack,
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp,
                        color = ChefGround,
                        modifier = Modifier.semantics { heading() },
                    )
                }

                Surface(
                    onClick = {
                        positionedWeek = null
                        cursorRaw = state.today.toString()
                        onCurrent()
                    },
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    modifier = Modifier.heightIn(min = 36.dp),
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = stringResource(if (state.dayMode) R.string.timeline_today_label else R.string.timeline_current_week),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = ChefGround,
                        )
                        Text("⌄", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ChefGround)
                    }
                }
            }

            // 24-HOUR CIRCULAR RADIAL DIAL (Exact match full-app-v1.html line 290-330)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(130.dp)) {
                    val w = size.width
                    val h = size.height
                    val cx = w / 2f
                    val cy = h / 2f

                    // Crosshair tick marks
                    drawLine(color = Color(0xFF9EA396), start = Offset(cx, 4f), end = Offset(cx, 16f), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(color = Color(0xFF9EA396), start = Offset(w - 4f, cy), end = Offset(w - 16f, cy), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(color = Color(0xFF9EA396), start = Offset(cx, h - 4f), end = Offset(cx, h - 16f), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(color = Color(0xFF9EA396), start = Offset(4f, cy), end = Offset(16f, cy), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)

                    // Concentric radial circles
                    drawCircle(color = Color.White.copy(alpha = 0.5f), radius = 54.dp.toPx(), center = Offset(cx, cy), style = Stroke(width = 1.dp.toPx()))
                    drawCircle(color = Color.White.copy(alpha = 0.7f), radius = 36.dp.toPx(), center = Offset(cx, cy), style = Stroke(width = 1.dp.toPx()))

                    // Coral Peak Wedge (09.00 - 14.00)
                    val wedgePath = Path().apply {
                        moveTo(cx, cy)
                        lineTo(cx, cy - 36.dp.toPx())
                        arcTo(
                            rect = androidx.compose.ui.geometry.Rect(cx - 36.dp.toPx(), cy - 36.dp.toPx(), cx + 36.dp.toPx(), cy + 36.dp.toPx()),
                            startAngleDegrees = -90f,
                            sweepAngleDegrees = 110f,
                            forceMoveTo = false,
                        )
                        close()
                    }
                    drawPath(path = wedgePath, color = ChefCoral.copy(alpha = 0.88f))

                    // White center circle
                    drawCircle(color = Color.White, radius = 20.dp.toPx(), center = Offset(cx, cy))

                    // Clock hands pointing to ~12:47
                    drawLine(color = ChefGround, start = Offset(cx, cy), end = Offset(cx + 22.dp.toPx(), cy - 20.dp.toPx()), strokeWidth = 2.8.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(color = ChefGround, start = Offset(cx, cy), end = Offset(cx, cy - 30.dp.toPx()), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                    drawCircle(color = ChefCoral, radius = 3.5.dp.toPx(), center = Offset(cx, cy))
                }
            }

            // Mode switcher chips (Week vs Day) and Week navigation
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = !state.dayMode,
                        onClick = { if (state.dayMode) onToggleDayMode() },
                        enabled = !state.busy,
                        label = { Text(stringResource(R.string.timeline_week_mode), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("timeline-mode-week"),
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ChefGround,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White.copy(alpha = 0.5f),
                            labelColor = ChefGround,
                        ),
                    )
                    FilterChip(
                        selected = state.dayMode,
                        onClick = { if (!state.dayMode) onToggleDayMode() },
                        enabled = !state.busy,
                        label = { Text(stringResource(R.string.timeline_day_mode), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("timeline-mode-day"),
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ChefGround,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White.copy(alpha = 0.5f),
                            labelColor = ChefGround,
                        ),
                    )
                }

                WeekSwitcher(
                    state = state,
                    onWeek = { direction ->
                        onWeek(direction)
                        cursorRaw = cursorDate.plusDays(if (state.dayMode) direction else direction * 7).toString()
                    },
                    onCurrent = onCurrent,
                )
            }

            // Day Selector Strip
            if (state.days.isNotEmpty()) {
                TimelineDayStrip(state, cursorDate, !state.busy) { date ->
                    if (onSelectDay != null) onSelectDay(date) else {
                        val offset = ChronoUnit.DAYS.between(if (state.dayMode) state.selectedDay ?: cursorDate else state.weekStart, date)
                        if (!state.dayMode) onToggleDayMode()
                        onWeek(offset)
                    }
                    cursorRaw = date.toString()
                    positionedWeek = null
                }
            }
        }

        // DARK SLIDING BOTTOM SHEET (Panel with rounded top corners)
        Surface(
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = ChefCanvas,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(top = 12.dp),
            ) {
                // Sliding Sheet Handle Bar
                Box(
                    Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(ChefLine)
                        .align(Alignment.CenterHorizontally),
                )

                // Timing Legend Row (Jam Puncak coral dot + Waktu Mandiri white dot)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(ChefCoral))
                        Text(
                            text = "Jam Puncak (09.00 - 14.00)",
                            style = MaterialTheme.typography.labelSmall,
                            color = ChefMuted,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(Color.White))
                        Text(
                            text = "Waktu Mandiri",
                            style = MaterialTheme.typography.labelSmall,
                            color = ChefMuted,
                        )
                    }
                }

                // Agenda List
                LazyColumn(
                    state = list,
                    modifier = Modifier.weight(1f).testTag("timeline-list"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    when {
                        state.readFailed -> item {
                            Column(Modifier.padding(16.dp)) {
                                Text(stringResource(R.string.read_failed), color = ChefMuted)
                                TextButton(onClick = onRetry) { Text(stringResource(R.string.retry), color = ChefLime) }
                            }
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

                // Bottom Action Button: ＋ JADWALKAN BLOK WAKTU BARU
                Surface(
                    onClick = onAdd,
                    enabled = !state.loading && !state.busy,
                    shape = RoundedCornerShape(999.dp),
                    color = ChefPanel,
                    border = BorderStroke(1.dp, ChefLine),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .height(52.dp)
                        .testTag("timeline-add"),
                ) {
                    Row(
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "＋ JADWALKAN BLOK WAKTU BARU",
                            fontFamily = NotoCondensedBlack,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp,
                            color = ChefInk,
                        )
                        Text(
                            text = "↗",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = ChefLime,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimelineDayIsland(day: TimelineDay, cursorDate: LocalDate, today: LocalDate) {
    val selected = day.date == cursorDate

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) ChefRaised else ChefPanel,
        border = BorderStroke(1.dp, if (selected) ChefLime.copy(alpha = 0.5f) else ChefLine),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("day-${day.date}"),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = day.date.format(dayFormat).uppercase(),
                    fontFamily = NotoCondensedBlack,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = ChefInk,
                    modifier = Modifier.semantics { heading() },
                )
                if (day.stops.isEmpty()) {
                    Text(
                        text = stringResource(R.string.plan_river_quiet_day),
                        style = MaterialTheme.typography.bodySmall,
                        color = ChefMuted,
                    )
                }
            }

            if (day.date == today) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = ChefLime,
                ) {
                    Text(
                        text = stringResource(R.string.timeline_today_marker),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                        color = ChefGround,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            } else {
                Text(
                    text = "${day.stops.size} agenda",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ChefMuted,
                )
            }
        }
    }
}

@Composable
private fun TimelineDayStrip(
    state: TimelineUiState,
    cursorDate: LocalDate,
    enabled: Boolean,
    onSelect: (LocalDate) -> Unit,
) {
    val dates = (0..6).map { state.weekStart.plusDays(it.toLong()) }
    LazyRow(
        Modifier.fillMaxWidth().testTag("timeline-day-strip"),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(dates, key = { it.toString() }) { date ->
            val day = state.days.firstOrNull { it.date == date }
            val selected = date == cursorDate
            val isToday = date == state.today

            Surface(
                onClick = { onSelect(date) },
                enabled = enabled,
                shape = RoundedCornerShape(16.dp),
                color = when {
                    selected -> ChefGround
                    isToday -> Color.White
                    else -> Color.White.copy(alpha = 0.5f)
                },
                contentColor = if (selected) Color.White else ChefGround,
                border = if (isToday && !selected) BorderStroke(1.5.dp, ChefGround) else null,
                modifier = Modifier
                    .width(44.dp)
                    .height(54.dp)
                    .testTag("timeline-day-$date")
                    .semantics {
                        this.selected = selected
                    },
            ) {
                Column(
                    Modifier.fillMaxSize().padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = date.format(DateTimeFormatter.ofPattern("EE", Locale.forLanguageTag("id-ID"))).uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = date.dayOfMonth.toString(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Box(
                        Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(if (day?.stops?.isNotEmpty() == true) ChefCoral else Color.Transparent),
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekSwitcher(state: TimelineUiState, onWeek: (Long) -> Unit, onCurrent: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        val currentWeek = state.today.minusDays(state.today.dayOfWeek.value.toLong() - 1)
        if (state.weekStart != currentWeek) {
            TextButton(
                onClick = onCurrent,
                enabled = !state.busy,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    stringResource(R.string.timeline_current_week),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ChefGround,
                )
            }
        }
        IconButton(
            enabled = !state.busy,
            onClick = { onWeek(-1) },
            modifier = Modifier.size(36.dp).testTag("timeline-previous"),
        ) {
            Text("‹", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ChefGround)
        }
        IconButton(
            enabled = !state.busy,
            onClick = { onWeek(1) },
            modifier = Modifier.size(36.dp).testTag("timeline-next"),
        ) {
            Text("›", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ChefGround)
        }
    }
}

@Composable
private fun TimelineEmpty() {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ChefPanel,
        border = BorderStroke(1.dp, ChefLine),
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
    ) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(R.string.timeline_empty),
                fontFamily = NotoCondensedBlack,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = ChefInk,
            )
            Text(
                stringResource(R.string.timeline_empty_hint),
                style = MaterialTheme.typography.bodySmall,
                color = ChefMuted,
            )
        }
    }
}

@Composable
private fun TimelineLoading() {
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(150); show = true }
    if (show) Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(3) {
            Surface(
                Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(16.dp),
                color = ChefPanel,
            ) {}
        }
    }
}

@Preview
@Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, fontScale = 2f)
@Composable
private fun TimelinePreview() {
    val zone = ZoneId.of("Asia/Makassar")
    val today = LocalDate.of(2026, 10, 6)
    val at = today.atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
    val event = Event(id = 1, title = "Diskusi kelompok", startAt = at, endAt = at + 7_200_000)
    val days = buildTimeline(emptyList(), listOf(event), timelineWeek(today), zone)
    RikoHubTheme {
        Surface {
            TimelineContent(
                TimelineUiState(
                    timelineWeek(today),
                    today = today,
                    now = at,
                    zone = zone,
                    days = days,
                    loading = false,
                ),
                {}, {}, {}, {}, {}, {}, {},
            )
        }
    }
}
