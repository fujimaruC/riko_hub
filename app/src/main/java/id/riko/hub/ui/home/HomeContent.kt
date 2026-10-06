package id.riko.hub.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.tooling.preview.Preview
import id.riko.hub.R
import id.riko.hub.core.time.Clock
import id.riko.hub.domain.logic.*
import id.riko.hub.domain.model.*
import id.riko.hub.navigation.Destination
import id.riko.hub.ui.compass.SpatialNavLayer
import id.riko.hub.ui.tasks.taskTimeFormat
import id.riko.hub.ui.theme.*
import kotlinx.coroutines.delay
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeContent(state: HomeUiState, reduced: Boolean, resumed: Boolean, onNavigate: (Destination) -> Unit,
                onAdd: () -> Unit, onPeek: (Long) -> Unit, onComplete: (Task) -> Unit, onPin: (Long) -> Unit, onRetry: () -> Unit) {
    val snapshot = state.snapshot
    val dark = MaterialTheme.colorScheme.background.luminance() < .5f
    val background by animateColorAsState(when (snapshot?.mode) {
        HomeMode.CALM -> if (dark) HomeTokens.calmDark else HomeTokens.calmLight
        HomeMode.DENSE -> MaterialTheme.colorScheme.background
        else -> MaterialTheme.colorScheme.background
    }, tween(if (reduced || !resumed) Motion.fast else Motion.long), label = "home mode")
    val fieldTint by animateColorAsState(if (snapshot?.mode == HomeMode.DENSE)
        (if (dark) HomeTokens.denseDark else HomeTokens.denseLight).copy(alpha = HomeTokens.denseWashAlpha) else background,
        tween(if (reduced || !resumed) Motion.fast else Motion.long), label = "field wash")
    SpatialNavLayer(Destination.Home, onNavigate, Modifier.fillMaxSize().testTag("screen-Home"), horizontalOnly = true) {
        Column(Modifier.fillMaxSize().background(background).semantics { isTraversalGroup = true }) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.space5),
                verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                HomeHeader(state, onNavigate)
                if (snapshot != null && snapshot.total > 0) HomeProgress(snapshot, reduced, resumed)
                snapshot?.next?.let { next ->
                    Surface(shape = RikoShapes.pill, color = MaterialTheme.colorScheme.surfaceVariant) {
                        Row(Modifier.fillMaxWidth().padding(Spacing.space3), horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                            Icon(androidx.compose.ui.res.painterResource(if (next.event) R.drawable.ic_event else R.drawable.ic_schedule),
                                contentDescription = null, modifier = Modifier.size(Spacing.space6))
                            Text(stringResource(if (next.event) R.string.home_next_event else R.string.home_next_task),
                                style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                            Text(stringResource(R.string.home_next, Instant.ofEpochMilli(next.at).atZone(snapshot.zone).format(taskTimeFormat), next.title),
                                style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(2f))
                        }
                    }
                }
                HomeHints(onNavigate, top = true)
                SpatialNavLayer(Destination.Home, onNavigate, Modifier.fillMaxWidth().background(fieldTint, RikoShapes.asymA).testTag("home-field").semantics { traversalIndex = 0f },
                    horizontalOnly = androidx.compose.ui.platform.LocalDensity.current.fontScale >= 1.3f) {
                    when {
                        state.readFailed -> Surface(shape = RikoShapes.asymA) {
                            Column(Modifier.padding(Spacing.space6)) {
                                Text(stringResource(R.string.read_failed))
                                TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
                            }
                        }
                        state.loading -> HomeLoading()
                        snapshot != null && snapshot.ranked.isEmpty() -> Column {
                            HomeEmpty(snapshot.doneToday.isNotEmpty())
                            BubbleField(snapshot, reduced, resumed, onPeek, onComplete, onPin) { onNavigate(Destination.Tasks) }
                        }
                        snapshot != null -> BubbleField(snapshot, reduced, resumed, onPeek, onComplete, onPin) { onNavigate(Destination.Tasks) }
                    }
                }
                HomeHints(onNavigate, top = false)
            }
            Button(onClick = onAdd, enabled = !state.busy && !state.loading && !state.readFailed,
                shape = RikoShapes.pill, modifier = Modifier.padding(horizontal = Spacing.space5, vertical = Spacing.space2)
                    .heightIn(min = Spacing.touch).testTag("home-add")) { Text(stringResource(R.string.home_quick_add)) }
        }
    }
}

@Composable
private fun HomeHeader(state: HomeUiState, onNavigate: (Destination) -> Unit) {
    Column(Modifier.padding(top = Spacing.space4)) {
        Text(stringResource(R.string.home_greeting, state.name.ifBlank { stringResource(R.string.home_default_name) }), style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.semantics { heading(); traversalIndex = -2f })
        val snapshot = state.snapshot
        if (snapshot != null) {
            Text(if (snapshot.urgentCount == 0) stringResource(if (snapshot.ranked.isEmpty() && snapshot.doneToday.isNotEmpty())
                R.string.home_clear else R.string.home_calm) else stringResource(R.string.home_urgent, snapshot.urgentCount),
                style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RikoShapes.pill, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.weight(1f)) {
                    Text(snapshot.today.format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.forLanguageTag("id-ID"))),
                        style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(Spacing.space2))
                }
                TextButton(onClick = { onNavigate(Destination.Settings) }) { Text(stringResource(R.string.settings)) }
            }
        }
    }
}

@Composable
private fun HomeProgress(snapshot: HomeSnapshot, reduced: Boolean, resumed: Boolean) {
    val progress by androidx.compose.animation.core.animateFloatAsState(snapshot.done.toFloat() / snapshot.total,
        if (reduced || !resumed) androidx.compose.animation.core.snap() else tween(Motion.short), label = "today progress")
    Surface(shape = RikoShapes.asymA, color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(Modifier.fillMaxWidth().padding(Spacing.space3), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.space3)) {
            CircularProgressIndicator(progress = { progress },
                modifier = Modifier.size(Spacing.space8).clearAndSetSemantics {}, color = LocalRikoColors.current.personal.color)
            Text(stringResource(R.string.home_progress, snapshot.done, snapshot.total), style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.testTag("home-progress").semantics { traversalIndex = 2f })
        }
    }
}

@Composable
private fun HomeEmpty(allDone: Boolean) {
    val colors = LocalRikoColors.current
    Column(Modifier.fillMaxWidth().padding(Spacing.space6), verticalArrangement = Arrangement.spacedBy(Spacing.space4)) {
        Canvas(Modifier.size(Spacing.space12)) {
            drawOval(colors.personal.color, size = androidx.compose.ui.geometry.Size(size.width*.8f,size.height*.6f))
            drawCircle(colors.academic.color, radius = size.width*.16f,
                center = androidx.compose.ui.geometry.Offset(size.width*.8f,size.height*.6f))
        }
        Text(stringResource(if (allDone) R.string.home_all_done else R.string.home_empty), style = MaterialTheme.typography.displayMedium)
    }
}

@Composable
private fun HomeLoading() {
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(150); show = true }
    if (show) Row(Modifier.fillMaxWidth().height(Spacing.illustration), horizontalArrangement = Arrangement.SpaceAround) {
        repeat(3) { Surface(Modifier.size(Spacing.touch), shape = HomeBlobShapes[it], color = MaterialTheme.colorScheme.surfaceVariant) {} }
    }
}

@Composable
private fun HomeHints(onNavigate: (Destination) -> Unit, top: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        val targets = if (top) listOf(Destination.Timeline) else listOf(Destination.Sync, Destination.Tasks, Destination.Vault)
        targets.forEach { target -> TextButton(onClick = { onNavigate(target) }) {
            Text(stringResource(when (target) {
                Destination.Timeline -> R.string.hint_timeline; Destination.Sync -> R.string.hint_sync
                Destination.Tasks -> R.string.hint_tasks; else -> R.string.hint_vault
            }), style = MaterialTheme.typography.labelSmall)
        } }
    }
}

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
    RikoHubTheme { HomeContent(HomeUiState(homeSnapshot(tasks, emptyList(), AppSettings(), clock), "Riko", false),
        true, false, {}, {}, {}, {}, {}, {}) }
}
