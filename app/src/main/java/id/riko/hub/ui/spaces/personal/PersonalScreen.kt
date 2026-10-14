package id.riko.hub.ui.spaces.personal

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import id.riko.hub.R
import id.riko.hub.RikoHubApp
import id.riko.hub.domain.model.*
import id.riko.hub.navigation.Destination
import id.riko.hub.ui.components.*
import id.riko.hub.ui.spaces.*
import id.riko.hub.ui.tasks.TaskEffect
import id.riko.hub.ui.tasks.TasksViewModel
import id.riko.hub.ui.theme.SpacesTokens
import id.riko.hub.ui.theme.rememberReducedMotion
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun PersonalScreen(onNavigate: (Destination) -> Unit) {
    val app = LocalContext.current.applicationContext as RikoHubApp
    val model: TasksViewModel = viewModel(key = "personal-space-tasks", factory = viewModelFactory {
        initializer { TasksViewModel(app.container.tasks, app.container.clock) }
    })
    val state by model.state.collectAsStateWithLifecycle()
    val reminderPermission = rememberReminderPermissionState { app.container.reminders.requestReconciliation() }
    val snackbar = remember { SnackbarHostState() }
    val accessibility = LocalAccessibilityManager.current
    val deleted = stringResource(R.string.deleted_task)
    val undo = stringResource(R.string.undo)
    val failed = stringResource(R.string.save_failed)
    val retry = stringResource(R.string.retry)
    LaunchedEffect(model, accessibility) {
        model.effects.collect { effect ->
            when (effect) {
                is TaskEffect.Deleted -> {
                    val timeout = accessibility?.calculateRecommendedTimeoutMillis(5_000, containsText = true, containsControls = true) ?: 5_000
                    if (withTimeoutOrNull(timeout) {
                            snackbar.showSnackbar(deleted, undo, withDismissAction = true, duration = SnackbarDuration.Indefinite)
                        } == SnackbarResult.ActionPerformed) model.restore(effect.id)
                }
                is TaskEffect.Failed -> if (snackbar.showSnackbar(failed, retry, withDismissAction = true,
                    duration = SnackbarDuration.Indefinite) == SnackbarResult.ActionPerformed) effect.retry()
            }
        }
    }
    SpacesTheme {
        Box(Modifier.fillMaxSize().background(SpacesTokens.canvas).testTag("screen-PersonalSpace")) {
            var showAll by rememberSaveable { mutableStateOf(false) }
            val personal = remember(state.tasks) { personalTasks(state.tasks) }
            val todayTasks = remember(personal, state.now, state.zone) { personalTodayTasks(personal, state.now, state.zone) }
            val routines = remember(personal, state.now, state.zone) { personalDailyRoutines(personal, state.now, state.zone) }
            val reminders = remember(personal, state.now, state.zone) { personalTodayReminders(personal, state.now, state.zone) }
            val notes = remember(personal) { personalNotes(personal) }
            val journalNow = if (state.now > 0L) state.now else app.container.clock.now()
            val journalZone = if (state.now > 0L) state.zone else app.container.clock.zone()
            val twoColumns = LocalConfiguration.current.fontScale < 1.5f
            val noteRows = remember(notes, twoColumns) { personalNoteRows(notes, twoColumns) }
            val lifeState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
            val reducedMotion = rememberReducedMotion()
            val listState = rememberLazyListState()
            val headerVisible by remember { derivedStateOf { listState.layoutInfo.visibleItemsInfo.any { it.key == "personal-garden-header" } } }
            LazyColumn(Modifier.fillMaxSize().testTag("personal-list"), state = listState, contentPadding = PaddingValues(bottom = 32.dp)) {
                item(key = "personal-garden-header") {
                    PersonalGardenHeader(
                        lifecycleResumed = lifeState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED) && headerVisible,
                        reducedMotion = reducedMotion,
                        title = stringResource(R.string.personal_title),
                        subtitle = stringResource(R.string.personal_subtitle),
                        onBack = { onNavigate(Destination.Spaces) },
                    )
                }
                item { PersonalGreeting(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) }
                item {
                    Box(Modifier.padding(horizontal = 20.dp)) {
                        SpacesSectionHeader(stringResource(R.string.personal_today), R.drawable.ic_personal_leaf,
                            if (personal.size > todayTasks.size) stringResource(if (showAll) R.string.personal_show_today else R.string.personal_show_all) else null,
                            { showAll = !showAll })
                    }
                }
                val visibleTasks = if (showAll) personal else todayTasks
                if (state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) }
                else if (state.readFailed) item {
                    Text(stringResource(R.string.personal_read_failed), Modifier.padding(horizontal = 20.dp), color = SpacesTokens.secondary)
                    TextButton(onClick = model::retryRead, modifier = Modifier.padding(horizontal = 12.dp)) { Text(stringResource(R.string.retry)) }
                } else if (visibleTasks.isEmpty()) item {
                    Text(stringResource(R.string.personal_tasks_empty), Modifier.padding(horizontal = 20.dp, vertical = 8.dp), color = SpacesTokens.secondary)
                } else items(visibleTasks, key = { "task-${it.id}" }) { task ->
                    PersonalTaskRow(task, state.now, state.zone, !state.busy, reducedMotion, model::edit,
                        { done -> model.complete(task, done) }, Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
                }
                item {
                    TextButton(onClick = { model.edit(Task(title = "", category = Category.PERSONAL)) }, enabled = !state.busy,
                        modifier = Modifier.padding(horizontal = 12.dp).heightIn(min = 48.dp).testTag("personal-add-task")) {
                        Text(stringResource(R.string.personal_add_task), color = SpacesTokens.tangerine)
                    }
                }
                item(key = "personal-journal") { PersonalJournalSurface(app.container.spaceNotes, journalNow, journalZone,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) }
                if (!state.loading && !state.readFailed) {
                    item {
                        Box(Modifier.padding(horizontal = 20.dp)) {
                            SpacesSectionHeader(stringResource(R.string.personal_reminders), R.drawable.ic_notification_reminder,
                                stringResource(R.string.personal_add_task), { model.edit(Task(title = "", category = Category.PERSONAL)) })
                        }
                    }
                    if (reminders.isEmpty()) item {
                        Text(stringResource(R.string.personal_reminders_empty), Modifier.padding(horizontal = 20.dp, vertical = 4.dp), color = SpacesTokens.secondary)
                    } else items(reminders, key = { "reminder-${it.first.id}" }) { (task, reminderAt) ->
                        PersonalReminderRow(task, reminderAt, state.now, state.zone, Modifier.padding(horizontal = 20.dp, vertical = 3.dp)
                            .clickable(role = Role.Button, onClick = { model.edit(task) }))
                    }
                }
                if (!state.loading && !state.readFailed) {
                    item { Box(Modifier.padding(horizontal = 20.dp)) { SpacesSectionHeader(stringResource(R.string.personal_routines), R.drawable.ic_personal_leaf) } }
                    if (routines.isEmpty()) {
                        item {
                            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(stringResource(R.string.renaissance_personal_routines_empty), color = SpacesTokens.secondary)
                                TextButton(onClick = { model.edit(Task(title = "", category = Category.PERSONAL)) }, enabled = !state.busy,
                                    modifier = Modifier.heightIn(min = 48.dp).testTag("personal-add-routine-task")) {
                                    Text(stringResource(R.string.personal_add_task), color = SpacesTokens.tangerine)
                                }
                            }
                        }
                    } else {
                        item {
                            FlowRow(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                routines.forEach { task ->
                                    RoutineChip(task, onClick = { model.complete(task, task.status != TaskStatus.DONE) }, enabled = !state.busy, reducedMotion = reducedMotion,
                                        modifier = Modifier)
                                }
                            }
                        }
                    }
                }
                if (!state.loading && !state.readFailed) {
                    item {
                        Box(Modifier.padding(horizontal = 20.dp)) {
                            SpacesSectionHeader(stringResource(R.string.personal_notes), R.drawable.ic_vault_document,
                                stringResource(R.string.personal_add_note), { model.edit(Task(title = "", category = Category.PERSONAL)) })
                        }
                    }
                    if (notes.isEmpty()) item {
                        Text(stringResource(R.string.personal_notes_empty), Modifier.padding(horizontal = 20.dp, vertical = 4.dp), color = SpacesTokens.secondary)
                    } else {
                        itemsIndexed(noteRows, key = { _, row -> "notes-${row.first().id}" }) { _, row ->
                            BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                                val columns = twoColumns && maxWidth >= 320.dp && row.size > 1
                                if (columns) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEach { task -> PersonalNoteCard(task, Math.floorMod(task.id, 3L).toInt(), Modifier.weight(1f)
                                        .clickable(role = Role.Button, onClick = { model.edit(task) })) }
                                } else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEach { task -> PersonalNoteCard(task, Math.floorMod(task.id, 3L).toInt(), Modifier.fillMaxWidth()
                                        .clickable(role = Role.Button, onClick = { model.edit(task) })) }
                                }
                            }
                        }
                    }
                }
            }
            if (state.editor == null) SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
            state.editor?.let { task ->
                TaskEditorSheet(task, state.now, state.zone, state.busy, snackbar, model::change, model::save,
                    { model.delete(task) }, model::dismissEditor, reminderPermission = reminderPermission)
            }
        }
    }
}

@Composable
private fun PersonalGardenHeader(lifecycleResumed: Boolean, reducedMotion: Boolean, title: String, subtitle: String, onBack: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val animate = lifecycleResumed && visible && !reducedMotion
    val starPhase = if (animate) {
        val transition = rememberInfiniteTransition(label = "personal-stars")
        transition.animateFloat(0f, 1f, infiniteRepeatable(tween(4_000, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "star-phase")
    } else rememberUpdatedState(0f)
    Box(Modifier.fillMaxWidth().height(220.dp).onGloballyPositioned {
        val bounds = it.boundsInWindow()
        visible = bounds.width > 0f && bounds.height > 0f
    }.clipToBounds().drawWithCache {
        val w = size.width
        val h = size.height
        val background = Brush.linearGradient(listOf(Color(0xFF1A1008), Color(0xFF221508), Color(0xFF0F1A12), Color(0xFF0A100A)), Offset.Zero, Offset(w, h))
        fun glow(cx: Float, cy: Float, rx: Float, ry: Float): Path = Path().apply {
            moveTo(cx - rx * .72f, cy)
            cubicTo(cx - rx, cy - ry, cx + rx * .28f, cy - ry * .72f, cx + rx, cy - ry * .15f)
            cubicTo(cx + rx * .8f, cy + ry * .82f, cx - rx * .2f, cy + ry, cx - rx * .72f, cy)
            close()
        }
        val sageGlow = glow(w * .18f, h * .94f, w * .48f, h * .52f)
        val orangeGlow = glow(w * .95f, h * .27f, w * .45f, h * .45f)
        val creamGlow = glow(w * .26f, h * .45f, w * .28f, h * .27f)
        val sageBrush = Brush.radialGradient(listOf(SpacesTokens.sage.copy(alpha = .25f), Color.Transparent), Offset(w * .18f, h * .94f), maxOf(w * .48f, h * .52f))
        val orangeBrush = Brush.radialGradient(listOf(SpacesTokens.tangerine.copy(alpha = .18f), Color.Transparent), Offset(w * .95f, h * .27f), maxOf(w * .45f, h * .45f))
        val creamBrush = Brush.radialGradient(listOf(SpacesTokens.cream.copy(alpha = .12f), Color.Transparent), Offset(w * .26f, h * .45f), maxOf(w * .28f, h * .27f))
        val moon = Offset(w * .83f, h * .27f)
        val moonBrush = Brush.radialGradient(listOf(Color(0xFFD4A030), Color(0xFF8A6018)), moon, 30.dp.toPx())
        val moonGlow = Brush.radialGradient(listOf(Color(0xFFD4A030).copy(alpha = .16f), Color.Transparent), moon, 43.dp.toPx())
        val lamp = Offset(w * .79f, h * .79f)
        val lampGlow = Brush.radialGradient(listOf(SpacesTokens.tangerine.copy(alpha = .32f), Color.Transparent), lamp, 32.dp.toPx())
        val stars = arrayOf(.20f to .25f, .50f to .15f, .70f to .35f, .85f to .20f, .35f to .45f, .60f to .10f)
        val leaf = Color(0xFF18231A)
        val stems = Path()
        val leaves = Path()
        fun plant(x: Float, scale: Float, right: Boolean) {
            val baseY = h
            val dx = if (right) -scale * .12f else scale * .08f
            stems.moveTo(x, baseY)
            stems.lineTo(x + dx, baseY - scale)
            for (i in 0..4) {
                val y = baseY - scale * (.15f + i * .16f)
                val side = if ((i % 2 == 0) xor right) -1f else 1f
                val start = Offset(x + dx * (.15f + i * .16f), y)
                leaves.moveTo(start.x, start.y)
                leaves.cubicTo(start.x + side * scale * .18f, start.y - scale * .08f, start.x + side * scale * .35f, start.y - scale * .10f, start.x + side * scale * .40f, start.y + scale * .02f)
                leaves.cubicTo(start.x + side * scale * .26f, start.y + scale * .15f, start.x + side * scale * .12f, start.y + scale * .12f, start.x, start.y)
                leaves.close()
            }
        }
        plant(w * .09f, 63.dp.toPx(), false)
        plant(w * .18f, 47.dp.toPx(), true)
        plant(w * .89f, 55.dp.toPx(), true)
        val bottomScrim = Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .45f)), h - 72.dp.toPx(), h)
        onDrawBehind {
            drawRect(background)
            drawPath(sageGlow, sageBrush); drawPath(orangeGlow, orangeBrush); drawPath(creamGlow, creamBrush)
            drawCircle(moonGlow, 43.dp.toPx(), moon)
            drawCircle(moonBrush, 30.dp.toPx(), moon, alpha = .5f)
            if (reducedMotion) stars.forEach { (x, y) -> drawCircle(Color.White.copy(alpha = .35f), 1.dp.toPx(), Offset(w * x, h * y)) }
            else {
                val phase = starPhase.value
                stars.forEachIndexed { index, (x, y) ->
                    val pulse = ((sin((phase * 2f * PI.toFloat() + index * 1.05f).toDouble()) + 1.0) / 2.0).toFloat()
                    drawCircle(Color.White.copy(alpha = .3f + pulse * .7f), 1.dp.toPx(), Offset(w * x, h * y))
                }
            }
            drawPath(stems, leaf, style = androidx.compose.ui.graphics.drawscope.Stroke(2.3.dp.toPx()))
            drawPath(leaves, leaf.copy(alpha = .85f))
            drawCircle(lampGlow, 32.dp.toPx(), lamp)
            drawRoundRect(Color(0xFF332419), Offset(lamp.x - 5.dp.toPx(), lamp.y - 12.dp.toPx()), Size(10.dp.toPx(), 13.dp.toPx()), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
            drawLine(Color(0xFF332419), Offset(lamp.x, lamp.y + 1.dp.toPx()), Offset(lamp.x, h), 2.dp.toPx())
            drawRect(bottomScrim)
        }
    }) {
        Column(Modifier.align(Alignment.BottomStart).padding(start = 22.dp, bottom = 20.dp)) {
            Text(title, color = SpacesTokens.primary, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-.5).sp,
                modifier = Modifier.semantics { heading() })
            Text(subtitle, color = SpacesTokens.cream.copy(alpha = .9f), fontSize = 13.sp, fontStyle = FontStyle.Italic)
        }
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(start = 8.dp, top = 4.dp).sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
            Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_chevron_left), stringResource(R.string.spaces_back), tint = SpacesTokens.primary)
        }
    }
}

@Composable
private fun PersonalGreeting(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SpacesTokens.cream.copy(alpha = .07f))
        .border(1.dp, SpacesTokens.cream.copy(alpha = .13f), RoundedCornerShape(16.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_personal_sparkle), null, tint = SpacesTokens.cream, modifier = Modifier.size(22.dp))
        Text(stringResource(R.string.personal_greeting), color = SpacesTokens.cream, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PersonalTaskRow(task: Task, now: Long, zone: java.time.ZoneId, enabled: Boolean, reducedMotion: Boolean, onEdit: (Task) -> Unit,
                            onComplete: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val done = task.status == TaskStatus.DONE
    val fill by animateColorAsState(if (done) SpacesTokens.jade else Color.Transparent,
        if (reducedMotion) snap() else tween(180), label = "personal-task-fill-${task.id}")
    val taskState = stringResource(if (done) R.string.personal_task_done else R.string.personal_task_open)
    val taskDescription = stringResource(R.string.personal_task_check_state, task.title, taskState)
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(SpacesTokens.row).testTag("personal-task-${task.id}")
        .border(1.dp, SpacesTokens.edge.copy(alpha = .28f), RoundedCornerShape(14.dp)).padding(start = 4.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).testTag("personal-task-check-${task.id}").toggleable(value = done, enabled = enabled, role = Role.Checkbox,
            onValueChange = onComplete)
            .semantics { contentDescription = taskDescription; stateDescription = taskState }) {
            Box(Modifier.align(Alignment.Center).size(22.dp).clip(CircleShape)
                .background(fill)
                .border(1.5.dp, if (done) SpacesTokens.jade else if (task.recurrence == Recurrence.DAILY) SpacesTokens.sage.copy(alpha = .8f) else SpacesTokens.tangerine.copy(alpha = .55f), CircleShape),
                contentAlignment = Alignment.Center) {
                if (done) Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_personal_check), null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
        Column(Modifier.weight(1f).clickable(role = Role.Button, onClick = { onEdit(task) }).padding(vertical = 6.dp)) {
            Text(task.title, color = if (done) SpacesTokens.secondary else SpacesTokens.primary,
                style = MaterialTheme.typography.bodyLarge, textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None)
            val line = when {
                done && task.completedAt != null -> stringResource(R.string.personal_task_done_at,
                    formatDeadline(task.completedAt, now, zone, hasTime = true))
                done -> stringResource(R.string.personal_task_done)
                task.deadlineAt == null -> stringResource(R.string.personal_task_no_deadline)
                else -> formatDeadline(task.deadlineAt, now, zone, task.hasDeadlineTime)
            }
            Text(line, color = SpacesTokens.secondary, style = MaterialTheme.typography.bodySmall)
        }
        if (!task.notes.isNullOrBlank()) Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_vault_document), stringResource(R.string.personal_has_note), tint = SpacesTokens.cream, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun PersonalReminderRow(task: Task, reminderAt: Long, now: Long, zone: java.time.ZoneId, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SpacesTokens.row).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_notification_reminder), null, tint = SpacesTokens.cream, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            Text(task.title, color = SpacesTokens.primary, style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.personal_reminder_at, formatTime(reminderAt, zone), formatDeadline(task.deadlineAt!!, now, zone, task.hasDeadlineTime)), color = SpacesTokens.secondary, style = MaterialTheme.typography.bodySmall)
        }
        Box(Modifier.size(20.dp).clip(CircleShape).border(1.5.dp, SpacesTokens.secondary.copy(alpha = .65f), CircleShape))
    }
}

@Composable
private fun RoutineChip(task: Task, onClick: () -> Unit, enabled: Boolean, reducedMotion: Boolean, modifier: Modifier = Modifier) {
    val done = task.status == TaskStatus.DONE
    val stateLabel = stringResource(if (done) R.string.personal_task_done else R.string.personal_task_open)
    val fill by animateColorAsState(if (done) SpacesTokens.jade.copy(alpha = .13f) else Color.White.copy(alpha = .035f),
        if (reducedMotion) snap() else tween(180), label = "personal-routine-fill-${task.id}")
    Row(modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp)).background(fill)
        .border(1.dp, if (done) SpacesTokens.jade.copy(alpha = .4f) else SpacesTokens.edge.copy(alpha = .45f), RoundedCornerShape(12.dp))
        .toggleable(value = done, enabled = enabled, role = Role.Checkbox, onValueChange = { onClick() })
        .semantics { stateDescription = stateLabel }.padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(task.title, color = if (done) SpacesTokens.jade else SpacesTokens.primary, style = MaterialTheme.typography.labelMedium)
        if (done) Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_personal_check), null, tint = SpacesTokens.jade, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun PersonalNoteCard(task: Task, tone: Int, modifier: Modifier = Modifier) {
    val colors = when (tone) {
        0 -> listOf(Color(0xFF1A1A12), Color(0xFF10100D))
        1 -> listOf(Color(0xFF12181A), Color(0xFF0A0F10))
        else -> listOf(Color(0xFF1A1210), Color(0xFF100A08))
    }
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(colors))
        .border(1.dp, SpacesTokens.edge.copy(alpha = .3f), RoundedCornerShape(16.dp)).padding(14.dp)) {
        Text(task.title, color = SpacesTokens.primary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(task.notes.orEmpty(), Modifier.padding(top = 5.dp), color = SpacesTokens.secondary, style = MaterialTheme.typography.bodySmall,
            maxLines = 3, overflow = TextOverflow.Ellipsis)
        Text(stringResource(R.string.personal_note_hint), Modifier.padding(top = 7.dp), color = SpacesTokens.cream, style = MaterialTheme.typography.labelSmall)
    }
}

private fun formatTime(epoch: Long, zone: java.time.ZoneId): String = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    .format(Instant.ofEpochMilli(epoch).atZone(zone))

private fun formatDateTime(epoch: Long, zone: java.time.ZoneId): String = DateTimeFormatter.ofPattern("EEE, d MMM · HH:mm", Locale.getDefault())
    .format(Instant.ofEpochMilli(epoch).atZone(zone))

private fun formatDeadline(epoch: Long, now: Long, zone: java.time.ZoneId, hasTime: Boolean): String {
    val deadline = Instant.ofEpochMilli(epoch).atZone(zone)
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    return when {
        hasTime && deadline.toLocalDate() == today -> formatTime(epoch, zone)
        hasTime -> formatDateTime(epoch, zone)
        else -> DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()).format(deadline)
    }
}
