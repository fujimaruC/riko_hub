package id.riko.hub.ui.spaces.personal

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import id.riko.hub.ui.theme.*
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    Box(Modifier.fillMaxSize().background(ChefCanvas).testTag("screen-PersonalSpace")) {
        var showAll by rememberSaveable { mutableStateOf(false) }
        var editorPresentation by rememberSaveable { mutableStateOf(TaskEditorPresentation.TASK) }
        val onAddRoutine = {
            editorPresentation = TaskEditorPresentation.ROUTINE
            model.edit(Task(title = "", category = Category.PERSONAL, recurrence = Recurrence.DAILY))
        }
        val personal = remember(state.tasks) { personalTasks(state.tasks) }
        val todayTasks = remember(personal, state.now, state.zone) { personalTodayTasks(personal, state.now, state.zone) }
        val routines = remember(personal, state.now, state.zone) { personalDailyRoutines(personal, state.now, state.zone) }
        val reminders = remember(personal, state.now, state.zone) { personalTodayReminders(personal, state.now, state.zone) }
        val notes = remember(personal) { personalNotes(personal) }
        val journalNow = if (state.now > 0L) state.now else app.container.clock.now()
        val journalZone = if (state.now > 0L) state.zone else app.container.clock.zone()
        val twoColumns = LocalConfiguration.current.fontScale < 1.5f
        val noteRows = remember(notes, twoColumns) { personalNoteRows(notes, twoColumns) }
        val reducedMotion = rememberReducedMotion()
        val listState = rememberLazyListState()
        LazyColumn(Modifier.fillMaxSize().testTag("personal-list"), state = listState, contentPadding = PaddingValues(bottom = 32.dp)) {
            item(key = "personal-garden-header") {
                PersonalGardenHeader(
                    title = stringResource(R.string.personal_title),
                    subtitle = stringResource(R.string.personal_subtitle),
                    onBack = { onNavigate(Destination.Spaces) },
                )
            }
            item { PersonalGreeting(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.personal_today).uppercase(),
                        fontFamily = NotoCondensedBlack,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = ChefInk,
                    )
                    if (personal.size > todayTasks.size) {
                        TextButton(onClick = { showAll = !showAll }) {
                            Text(stringResource(if (showAll) R.string.personal_show_today else R.string.personal_show_all), color = ChefLime, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
            val visibleTasks = if (showAll) personal else todayTasks
            if (state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp), color = ChefLime) }
            else if (state.readFailed) item {
                Text(stringResource(R.string.personal_read_failed), Modifier.padding(horizontal = 16.dp), color = ChefMuted)
                TextButton(onClick = model::retryRead, modifier = Modifier.padding(horizontal = 8.dp)) { Text(stringResource(R.string.retry), color = ChefLime) }
            } else if (visibleTasks.isEmpty()) item {
                Text(stringResource(R.string.personal_tasks_empty), Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = ChefMuted)
            } else items(visibleTasks, key = { "task-${it.id}" }) { task ->
                PersonalTaskRow(task, state.now, state.zone, !state.busy, reducedMotion, {
                    editorPresentation = if (task.recurrence == Recurrence.DAILY) TaskEditorPresentation.ROUTINE else TaskEditorPresentation.TASK
                    model.edit(task)
                },
                    { done -> model.complete(task, done) }, Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }
            item {
                TextButton(onClick = { editorPresentation = TaskEditorPresentation.TASK; model.edit(Task(title = "", category = Category.PERSONAL)) }, enabled = !state.busy,
                    modifier = Modifier.padding(horizontal = 8.dp).heightIn(min = 44.dp).testTag("personal-add-task")) {
                    Text("＋ " + stringResource(R.string.personal_add_task).uppercase(), color = ChefLime, fontWeight = FontWeight.Bold)
                }
            }
            item(key = "personal-journal") { PersonalJournalSurface(app.container.spaceNotes, journalNow, journalZone,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) }
            if (!state.loading && !state.readFailed) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.personal_reminders).uppercase(),
                            fontFamily = NotoCondensedBlack,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = ChefInk,
                        )
                        TextButton(onClick = { editorPresentation = TaskEditorPresentation.TASK; model.edit(Task(title = "", category = Category.PERSONAL)) }) {
                            Text("＋ " + stringResource(R.string.personal_add_task).uppercase(), color = ChefLime, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
                if (reminders.isEmpty()) item {
                    Text(stringResource(R.string.personal_reminders_empty), Modifier.padding(horizontal = 16.dp, vertical = 4.dp), color = ChefMuted)
                } else items(reminders, key = { "reminder-${it.first.id}" }) { (task, reminderAt) ->
                    PersonalReminderRow(task, reminderAt, state.now, state.zone, Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable(role = Role.Button, onClick = { editorPresentation = TaskEditorPresentation.TASK; model.edit(task) }))
                }
            }
            if (!state.loading && !state.readFailed) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.personal_routines).uppercase(),
                            fontFamily = NotoCondensedBlack,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = ChefInk,
                        )
                        if (routines.isNotEmpty()) {
                            TextButton(onClick = onAddRoutine) {
                                Text("＋ " + stringResource(R.string.contextual_routine_new).uppercase(), color = ChefLime, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
                if (routines.isEmpty()) {
                    item {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.renaissance_personal_routines_empty), color = ChefMuted)
                            TextButton(onClick = onAddRoutine, enabled = !state.busy,
                                modifier = Modifier.heightIn(min = 44.dp).testTag("personal-add-routine-task")) {
                                Text(stringResource(R.string.contextual_routine_new), color = ChefLime, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    item {
                        FlowRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.personal_notes).uppercase(),
                            fontFamily = NotoCondensedBlack,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = ChefInk,
                        )
                        TextButton(onClick = { editorPresentation = TaskEditorPresentation.NOTE; model.edit(Task(title = "", category = Category.PERSONAL)) }) {
                            Text("＋ " + stringResource(R.string.personal_add_note).uppercase(), color = ChefLime, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
                if (notes.isEmpty()) item {
                    Text(stringResource(R.string.personal_notes_empty), Modifier.padding(horizontal = 16.dp, vertical = 4.dp), color = ChefMuted)
                } else {
                    itemsIndexed(noteRows, key = { _, row -> "notes-${row.first().id}" }) { _, row ->
                        BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            val columns = twoColumns && maxWidth >= 320.dp && row.size > 1
                            if (columns) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { task -> PersonalNoteCard(task, Math.floorMod(task.id, 3L).toInt(), Modifier.weight(1f)
                                    .clickable(role = Role.Button, onClick = { editorPresentation = TaskEditorPresentation.NOTE; model.edit(task) })) }
                            } else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { task -> PersonalNoteCard(task, Math.floorMod(task.id, 3L).toInt(), Modifier.fillMaxWidth()
                                    .clickable(role = Role.Button, onClick = { editorPresentation = TaskEditorPresentation.NOTE; model.edit(task) })) }
                            }
                        }
                    }
                }
            }
        }
        if (state.editor == null) SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        state.editor?.let { task ->
            TaskEditorSheet(task, state.now, state.zone, state.busy, snackbar, model::change, model::save,
                { model.delete(task) }, model::dismissEditor, reminderPermission = reminderPermission,
                presentation = editorPresentation)
        }
    }
}

@Composable
private fun PersonalGardenHeader(title: String, subtitle: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(ChefPanel)
                    .border(1.dp, ChefLine, CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) {
                Text("←", color = ChefInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Column {
                Text("03 / PERSONAL", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = ChefMuted)
                Text(
                    text = "RITME TENANG",
                    fontFamily = NotoCondensedBlack,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    color = ChefInk,
                    modifier = Modifier.semantics { heading() }
                )
            }
        }
    }
}

@Composable
private fun PersonalGreeting(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = ChefSage,
        border = BorderStroke(2.dp, ChefGround),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ChefGround.copy(alpha = 0.12f),
            ) {
                Text(
                    text = stringResource(R.string.personal_today).uppercase(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                    color = ChefGround,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier.size(34.dp).clip(CircleShape).background(ChefGround),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🌿", fontSize = 16.sp)
                }
                Text(
                    text = stringResource(R.string.personal_greeting),
                    fontFamily = NotoCondensedBlack,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    color = ChefGround,
                )
            }
        }
    }
}

@Composable
private fun PersonalTaskRow(task: Task, now: Long, zone: java.time.ZoneId, enabled: Boolean, reducedMotion: Boolean, onEdit: (Task) -> Unit,
                            onComplete: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val done = task.status == TaskStatus.DONE
    val fill by animateColorAsState(if (done) ChefLime else Color.Transparent,
        if (reducedMotion) snap() else tween(180), label = "personal-task-fill-${task.id}")
    val taskState = stringResource(if (done) R.string.personal_task_done else R.string.personal_task_open)
    val taskDescription = stringResource(R.string.personal_task_check_state, task.title, taskState)
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = ChefPanel,
        border = BorderStroke(1.dp, ChefLine),
        modifier = modifier.fillMaxWidth().testTag("personal-task-${task.id}"),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.size(38.dp).testTag("personal-task-check-${task.id}").toggleable(value = done, enabled = enabled, role = Role.Checkbox,
                onValueChange = onComplete)
                .semantics { contentDescription = taskDescription; stateDescription = taskState },
                contentAlignment = Alignment.Center) {
                Box(Modifier.size(22.dp).clip(CircleShape)
                    .background(fill)
                    .border(1.5.dp, if (done) ChefLime else if (task.recurrence == Recurrence.DAILY) ChefSage else ChefCoral, CircleShape),
                    contentAlignment = Alignment.Center) {
                    if (done) Text("✓", color = ChefGround, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
            }
            Column(Modifier.weight(1f).clickable(role = Role.Button, onClick = { onEdit(task) })) {
                Text(task.title, color = if (done) ChefMuted else ChefInk,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None)
                val line = when {
                    done && task.completedAt != null -> stringResource(R.string.personal_task_done_at,
                        formatDeadline(task.completedAt, now, zone, hasTime = true))
                    done -> stringResource(R.string.personal_task_done)
                    task.deadlineAt == null -> stringResource(R.string.personal_task_no_deadline)
                    else -> formatDeadline(task.deadlineAt, now, zone, task.hasDeadlineTime)
                }
                Text(line, color = ChefMuted, style = MaterialTheme.typography.bodySmall)
            }
            if (!task.notes.isNullOrBlank()) Text("📝", fontSize = 14.sp)
        }
    }
}

@Composable
private fun PersonalReminderRow(task: Task, reminderAt: Long, now: Long, zone: java.time.ZoneId, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = ChefPanel,
        border = BorderStroke(1.dp, ChefLine),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(ChefCoral))
            Column(Modifier.weight(1f)) {
                Text(task.title, color = ChefInk, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                Text(stringResource(R.string.personal_reminder_at, formatTime(reminderAt, zone), formatDeadline(task.deadlineAt!!, now, zone, task.hasDeadlineTime)), color = ChefMuted, style = MaterialTheme.typography.bodySmall)
            }
            Text("⏰", fontSize = 14.sp)
        }
    }
}

@Composable
private fun RoutineChip(task: Task, onClick: () -> Unit, enabled: Boolean, reducedMotion: Boolean, modifier: Modifier = Modifier) {
    val done = task.status == TaskStatus.DONE
    val stateLabel = stringResource(if (done) R.string.personal_task_done else R.string.personal_task_open)
    val fill by animateColorAsState(if (done) ChefLime.copy(alpha = 0.2f) else ChefPanel,
        if (reducedMotion) snap() else tween(180), label = "personal-routine-fill-${task.id}")
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = fill,
        border = BorderStroke(1.dp, if (done) ChefLime else ChefLine),
        modifier = modifier.heightIn(min = 40.dp)
            .toggleable(value = done, enabled = enabled, role = Role.Checkbox, onValueChange = { onClick() })
            .semantics { stateDescription = stateLabel },
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(task.title, color = if (done) ChefLime else ChefInk, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            if (done) Text("✓", color = ChefLime, fontWeight = FontWeight.Black, fontSize = 12.sp)
        }
    }
}

@Composable
private fun PersonalNoteCard(task: Task, tone: Int, modifier: Modifier = Modifier) {
    val borderColor = when (tone) {
        0 -> ChefButter
        1 -> ChefCoral
        else -> ChefSage
    }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = ChefPanel,
        border = BorderStroke(1.dp, ChefLine),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(task.title, color = ChefInk, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(task.notes.orEmpty(), color = ChefMuted, style = MaterialTheme.typography.bodySmall,
                maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text(stringResource(R.string.personal_note_hint), color = borderColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
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
