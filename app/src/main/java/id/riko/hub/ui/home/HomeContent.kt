package id.riko.hub.ui.home

import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.tooling.preview.Preview
import id.riko.hub.R
import id.riko.hub.core.time.Clock
import id.riko.hub.domain.logic.*
import id.riko.hub.domain.model.*
import id.riko.hub.navigation.Destination
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import id.riko.hub.ui.tasks.*
import id.riko.hub.ui.theme.*
import kotlinx.coroutines.delay
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeContent(state: HomeUiState, reduced: Boolean, resumed: Boolean, onNavigate: (Destination) -> Unit,
                onAdd: () -> Unit, onPeek: (Long) -> Unit, onComplete: (Task) -> Unit, onPin: (Long) -> Unit,
                onRetry: () -> Unit, hintsShownCount: Int = 0, onBackgroundColorChanged: (Color) -> Unit = {}) {
    val snapshot = state.snapshot
    val background = MaterialTheme.colorScheme.background
    SideEffect { onBackgroundColorChanged(background) }
    if (state.loading && !state.readFailed) {
        Box(Modifier.fillMaxSize().background(background).testTag("screen-Home").semantics { isTraversalGroup = true },
            contentAlignment = Alignment.Center) { HomeLoading() }
        return
    }
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
        Column(Modifier.fillMaxSize().background(background).testTag("screen-Home").semantics { isTraversalGroup = true }) {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.sectionGap)) {
                HomeHeader(state, onNavigate)
                Column(Modifier.padding(horizontal = Spacing.screenGutter).fillMaxWidth().testTag("home-field")
                    .semantics { traversalIndex = 0f }, verticalArrangement = Arrangement.spacedBy(Spacing.space4)) {
                    when {
                        state.readFailed -> Surface(shape = RikoShapes.asymA, color = MaterialTheme.colorScheme.surfaceVariant) {
                            Column(Modifier.padding(Spacing.space6)) {
                                Text(stringResource(R.string.read_failed))
                                TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
                            }
                        }
                        snapshot != null && snapshot.ranked.isEmpty() -> HomeEmpty(snapshot.doneToday.isNotEmpty())
                        snapshot != null -> HomeFocusDeck(snapshot, state.busy, onPeek, onComplete, onPin) { onNavigate(Destination.Tasks) }
                    }
                    if (!state.readFailed && snapshot != null) {
                        if (snapshot.total > 0) HomeProgress(snapshot, reduced, resumed)
                        snapshot.next?.let { next ->
                            Surface(color = MaterialTheme.colorScheme.surface, shape = RikoShapes.asymB) {
                                Row(Modifier.fillMaxWidth().padding(Spacing.space4), horizontalArrangement = Arrangement.spacedBy(Spacing.space3),
                                    verticalAlignment = Alignment.Top) {
                                    Icon(painterResource(if (next.event) R.drawable.ic_event else R.drawable.ic_schedule), null,
                                        Modifier.padding(top = Spacing.space1).size(Spacing.space6), tint = MaterialTheme.colorScheme.primary)
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.space1)) {
                                        Text(stringResource(if (next.event) R.string.home_next_event else R.string.home_next_task),
                                            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(Instant.ofEpochMilli(next.at).atZone(snapshot.zone).format(
                                            DateTimeFormatter.ofPattern("EEE, d MMM · HH:mm", Locale.forLanguageTag("id-ID"))),
                                            style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                        Text(next.title, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                        HomeDoneRows(snapshot.doneToday)
                    }
                    TextButton(onClick = { onNavigate(Destination.Tasks) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touch).testTag("home-all-tasks")) {
                        Text(stringResource(R.string.renaissance_all_tasks))
                        Icon(painterResource(R.drawable.ic_chevron_right), null, Modifier.padding(start = Spacing.space2).size(Spacing.space5))
                    }
                }
                Spacer(Modifier.height(Spacing.space2))
            }
            HomeQuickActions(!state.busy && !state.loading && !state.readFailed, onAdd, onNavigate)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeQuickActions(enabled: Boolean, onAdd: () -> Unit, onNavigate: (Destination) -> Unit) {
    FlowRow(Modifier.fillMaxWidth().padding(horizontal = Spacing.screenGutter, vertical = Spacing.space2),
        horizontalArrangement = Arrangement.spacedBy(Spacing.space2), verticalArrangement = Arrangement.spacedBy(Spacing.space1)) {
        Button(onClick = onAdd, enabled = enabled, shape = RikoShapes.pill,
            modifier = Modifier.heightIn(min = Spacing.touch).testTag("home-add")) {
            Icon(painterResource(R.drawable.ic_dock_capture), null, Modifier.size(Spacing.space5))
            Spacer(Modifier.width(Spacing.space2))
            Text(stringResource(R.string.home_quick_add))
        }
        TextButton(onClick = { onNavigate(Destination.PersonalSpace) }, modifier = Modifier.heightIn(min = Spacing.touch)) {
            Icon(painterResource(R.drawable.ic_vault_document), null, Modifier.size(Spacing.space5))
            Spacer(Modifier.width(Spacing.space2))
            Text(stringResource(R.string.today_quick_personal))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeFocusDeck(snapshot: HomeSnapshot, busy: Boolean, onPeek: (Long) -> Unit,
                          onComplete: (Task) -> Unit, onPin: (Long) -> Unit, onOverflow: () -> Unit) {
    val focus = snapshot.ranked.firstOrNull { it.task.id == snapshot.focus } ?: snapshot.ranked.firstOrNull() ?: return
    val visibleCount = if (LocalDensity.current.fontScale >= 1.3f) 3 else 6
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.space4)) {
        val completeLabel = stringResource(R.string.home_complete)
        val pinLabel = stringResource(if (snapshot.validPin == focus.task.id) R.string.home_unpin else R.string.home_pin)
        val ink = MaterialTheme.colorScheme.onPrimary
        Surface(color = MaterialTheme.colorScheme.primary, contentColor = ink, shape = TodayFocusContour,
            modifier = Modifier.fillMaxWidth().combinedClickable(enabled = !busy,
                onClick = { onPeek(focus.task.id) }, onLongClick = { onComplete(focus.task) })
                .semantics(mergeDescendants = true) {
                    customActions = listOf(
                        CustomAccessibilityAction(completeLabel) { if (!busy) onComplete(focus.task); !busy },
                        CustomAccessibilityAction(pinLabel) { if (!busy) onPin(focus.task.id); !busy },
                    )
                }.testTag("bubble-${focus.task.id}")) {
            Column(Modifier.padding(Spacing.space5), verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                    Icon(painterResource(R.drawable.ic_personal_sparkle), null, Modifier.size(Spacing.space5))
                    Text(stringResource(if (snapshot.validPin == focus.task.id) R.string.renaissance_core_refresh_focus_pinned
                        else R.string.renaissance_core_refresh_focus),
                        style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    Icon(painterResource(R.drawable.ic_chevron_right), null, Modifier.size(Spacing.space6))
                }
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val showIllustration = maxWidth >= 280.dp && LocalDensity.current.fontScale < 1.3f
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space2), verticalAlignment = Alignment.CenterVertically) {
                        Text(focus.task.title, style = MaterialTheme.typography.displayMedium,
                            modifier = Modifier.weight(1f))
                        if (showIllustration) Image(painterResource(R.drawable.illustration_study_desk), null,
                            modifier = Modifier.width(112.dp).height(80.dp).clip(RikoShapes.medium),
                            contentScale = ContentScale.Crop)
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.space3), verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                    Text(stringResource(focus.task.priority.label()), style = MaterialTheme.typography.labelLarge)
                    Text(stringResource(focus.task.status.label()), style = MaterialTheme.typography.labelMedium)
                    if (focus.isUrgent) Text(stringResource(if (focus.isOverdue) R.string.home_overdue else R.string.renaissance_attention),
                        style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold)
                    Text(deadlineLabel(focus.task, snapshot.now, snapshot.zone), style = MaterialTheme.typography.bodyMedium)
                }
                if (focus.task.checklist.isNotEmpty()) {
                    val done = focus.task.checklist.count { it.done }
                    Text(stringResource(R.string.renaissance_checklist_progress, done, focus.task.checklist.size),
                        style = MaterialTheme.typography.labelMedium)
                    LinearProgressIndicator(progress = { done.toFloat() / focus.task.checklist.size }, color = ink,
                        trackColor = ink.copy(alpha = .18f), modifier = Modifier.fillMaxWidth().height(Spacing.space1), drawStopIndicator = {})
                }
                val actionColors = ButtonDefaults.textButtonColors(contentColor = ink, disabledContentColor = ink.copy(alpha = .6f))
                if (LocalDensity.current.fontScale >= 1.3f) Column(Modifier.fillMaxWidth()) {
                    TextButton(onClick = { onPin(focus.task.id) }, enabled = !busy, colors = actionColors,
                        modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touch)) { Text(pinLabel) }
                    TextButton(onClick = { onComplete(focus.task) }, enabled = !busy, colors = actionColors,
                        modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touch).testTag("home-complete-${focus.task.id}")) {
                        Icon(painterResource(R.drawable.ic_personal_check), null, Modifier.size(Spacing.space5))
                        Spacer(Modifier.width(Spacing.space2))
                        Text(completeLabel)
                    }
                } else Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { onPin(focus.task.id) }, enabled = !busy, colors = actionColors,
                        modifier = Modifier.weight(1f).heightIn(min = Spacing.touch)) { Text(pinLabel) }
                    TextButton(onClick = { onComplete(focus.task) }, enabled = !busy, colors = actionColors,
                        modifier = Modifier.heightIn(min = Spacing.touch).testTag("home-complete-${focus.task.id}")) {
                        Icon(painterResource(R.drawable.ic_personal_check), null, Modifier.size(Spacing.space5))
                        Spacer(Modifier.width(Spacing.space2))
                        Text(completeLabel)
                    }
                }
            }
        }
        if (snapshot.ranked.size > 1) Text(stringResource(R.string.today_priority_heading),
            style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        snapshot.ranked.asSequence().filter { it.task.id != focus.task.id }.take(visibleCount - 1).forEachIndexed { index, item ->
            HomeRankedRow(item, index, snapshot, busy, onPeek, onComplete, onPin)
        }
        if (snapshot.ranked.size > visibleCount) TextButton(onClick = onOverflow) {
            Text(stringResource(R.string.home_overflow, snapshot.ranked.size - visibleCount))
        }
    }
}

@Composable
private fun HomeRankedRow(item: UrgencyInfo, index: Int, snapshot: HomeSnapshot, busy: Boolean,
                          onPeek: (Long) -> Unit, onComplete: (Task) -> Unit, onPin: (Long) -> Unit) {
    val task = item.task
    val completeLabel = stringResource(R.string.home_complete)
    val pinLabel = stringResource(if (snapshot.validPin == task.id) R.string.home_unpin else R.string.home_pin)
    val largeText = LocalDensity.current.fontScale >= 1.3f
    Surface(shape = RikoShapes.wide, color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().combinedClickable(enabled = !busy, onClick = { onPeek(task.id) },
            onLongClick = { onComplete(task) }).semantics(mergeDescendants = true) {
            customActions = listOf(
                CustomAccessibilityAction(completeLabel) { if (!busy) onComplete(task); !busy },
                CustomAccessibilityAction(pinLabel) { if (!busy) onPin(task.id); !busy },
            )
        }.testTag("bubble-${task.id}")) {
        Column(Modifier.fillMaxWidth().padding(horizontal = Spacing.space3, vertical = Spacing.space2)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                Text("%02d".format(index + 2), style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.space1)) {
                    Text(task.title, style = MaterialTheme.typography.titleSmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Text((if (item.isOverdue) stringResource(R.string.home_overdue) + " · " else "") + deadlineLabel(task, snapshot.now, snapshot.zone),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(task.priority.label()) + " · " + stringResource(task.status.label()),
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!largeText) {
                    IconButton(onClick = { onPin(task.id) }, enabled = !busy) {
                        Icon(painterResource(R.drawable.ic_personal_sparkle), pinLabel, Modifier.size(Spacing.space5),
                            tint = MaterialTheme.colorScheme.primary)
                    }
                    Checkbox(checked = false, onCheckedChange = { if (it) onComplete(task) }, enabled = !busy,
                        modifier = Modifier.testTag("home-complete-${task.id}").semantics { contentDescription = completeLabel })
                }
            }
            if (largeText) {
                TextButton(onClick = { onPin(task.id) }, enabled = !busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touch)) { Text(pinLabel) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Checkbox(checked = false, onCheckedChange = { if (it) onComplete(task) }, enabled = !busy,
                        modifier = Modifier.testTag("home-complete-${task.id}").semantics { contentDescription = completeLabel })
                }
            }
        }
    }
}

@Composable
private fun HomeDoneRows(tasks: List<Task>) {
    if (tasks.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(top = Spacing.space2), verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
        Text(stringResource(R.string.renaissance_done_today, tasks.size), style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        tasks.take(4).forEach { task ->
            Surface(shape = RikoShapes.wide, color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth().testTag("home-done-${task.id}")) {
                Row(Modifier.padding(horizontal = Spacing.space3, vertical = Spacing.space2), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = true, onCheckedChange = null, enabled = false)
                    Text(task.title, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(state: HomeUiState, onNavigate: (Destination) -> Unit) {
    val snapshot = state.snapshot
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
        Row(Modifier.fillMaxWidth().padding(start = Spacing.screenGutter, end = Spacing.space3, top = Spacing.space2),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.today_homebase), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
                if (snapshot != null) Text(snapshot.today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.forLanguageTag("id-ID"))),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = { onNavigate(Destination.Settings) }, modifier = Modifier.heightIn(min = Spacing.touch)) {
                Text(stringResource(R.string.settings), color = MaterialTheme.colorScheme.onSurface)
            }
        }
        Column(Modifier.padding(horizontal = Spacing.screenGutter), verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
            Text(stringResource(R.string.home_greeting, state.name.ifBlank { stringResource(R.string.today_default_name) }),
                style = MaterialTheme.typography.displayLarge, modifier = Modifier.semantics { heading(); traversalIndex = -2f })
            Text(stringResource(R.string.today_greeting_note), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (snapshot != null && !state.readFailed) Row(Modifier.padding(horizontal = Spacing.screenGutter),
            horizontalArrangement = Arrangement.spacedBy(Spacing.space2), verticalAlignment = Alignment.Top) {
            Icon(painterResource(R.drawable.ic_personal_sparkle), null, Modifier.padding(top = Spacing.space1).size(Spacing.space4),
                tint = MaterialTheme.colorScheme.primary)
            Text(if (snapshot.urgentCount == 0) stringResource(if (snapshot.ranked.isEmpty() && snapshot.doneToday.isNotEmpty())
                R.string.home_clear else R.string.home_calm) else stringResource(R.string.home_urgent, snapshot.urgentCount),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HomeProgress(snapshot: HomeSnapshot, reduced: Boolean, resumed: Boolean) {
    val progress by androidx.compose.animation.core.animateFloatAsState(snapshot.done.toFloat() / snapshot.total,
        if (reduced || !resumed) androidx.compose.animation.core.snap() else tween(Motion.short), label = "today progress")
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space1)) {
        Text(stringResource(R.string.home_progress, snapshot.done, snapshot.total), style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("home-progress").semantics { traversalIndex = 2f })
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(Spacing.space1), color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant, drawStopIndicator = {})
    }
}

@Composable
private fun HomeEmpty(allDone: Boolean) {
    val accent = MaterialTheme.colorScheme.primary
    Column(Modifier.fillMaxWidth().padding(Spacing.space6), verticalArrangement = Arrangement.spacedBy(Spacing.space4)) {
        Canvas(Modifier.size(Spacing.space12)) {
            drawOval(accent.copy(alpha = .3f), size = androidx.compose.ui.geometry.Size(size.width*.8f,size.height*.6f))
            drawCircle(accent, radius = size.width*.16f,
                center = androidx.compose.ui.geometry.Offset(size.width*.8f,size.height*.6f))
        }
        Text(stringResource(if (allDone) R.string.home_all_done else R.string.home_empty), style = MaterialTheme.typography.displayMedium)
    }
}

@Composable
private fun HomeLoading() {
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(150); show = true }
    if (show) Box(Modifier.fillMaxWidth().padding(horizontal = Spacing.screenGutter), contentAlignment = Alignment.Center) {
        LinearProgressIndicator(Modifier.widthIn(max = 320.dp).fillMaxWidth())
    }
}

fun edgeHintAlpha(launchCount: Int) = if (launchCount > 3) 0.55f else 1f

@Preview @Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Preview(fontScale = 2f, heightDp = 1000)
@Composable
fun HomePreview() {
    val clock = object : Clock {
        override fun now() = Instant.parse("2026-10-06T06:00:00Z").toEpochMilli()
        override fun zone() = ZoneId.of("Asia/Makassar")
    }
    val tasks = listOf(Task(id = 1, title = "Persiapan UTS", category = Category.ACADEMIC, priority = Priority.HIGH,
        deadlineAt = clock.now() + 18_000_000), Task(id = 2, title = "Tugas Individu", category = Category.PROJECTS),
        Task(id = 3, title = "Minum air", priority = Priority.LOW))
    RikoHubTheme(darkTheme = androidx.compose.foundation.isSystemInDarkTheme()) { HomeContent(HomeUiState(homeSnapshot(tasks, emptyList(), AppSettings(), clock), "Riko", false),
        true, false, {}, {}, {}, {}, {}, {}) }
}
