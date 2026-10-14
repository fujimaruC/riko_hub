package id.riko.hub.ui.spaces

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import id.riko.hub.R
import id.riko.hub.RikoHubApp
import id.riko.hub.domain.model.*
import id.riko.hub.navigation.Destination
import id.riko.hub.ui.components.TaskEditorSheet
import id.riko.hub.ui.components.rememberReminderPermissionState
import id.riko.hub.ui.tasks.TaskEffect
import id.riko.hub.ui.tasks.TasksViewModel
import id.riko.hub.ui.tasks.label
import id.riko.hub.ui.theme.Motion
import id.riko.hub.ui.theme.SpacesTokens
import id.riko.hub.ui.theme.rememberReducedMotion
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ProjectsSpaceScreen(onNavigate: (Destination) -> Unit) {
    val app = LocalContext.current.applicationContext as RikoHubApp
    val tasksModel: TasksViewModel = viewModel(key = "projects-space-tasks", factory = viewModelFactory {
        initializer { TasksViewModel(app.container.tasks, app.container.clock) }
    })
    val tasksState by tasksModel.state.collectAsStateWithLifecycle()
    var ideasReadFailed by remember { mutableStateOf(false) }
    var ideasReload by rememberSaveable { mutableIntStateOf(0) }
    val ideasFlow = remember(app.container.spaceNotes, ideasReload) {
        app.container.spaceNotes.observeIdeas().onStart { ideasReadFailed = false }.catch { error ->
            if (error is CancellationException) throw error
            ideasReadFailed = true
            emit(emptyList())
        }
    }
    val ideas by ideasFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val reminderPermission = rememberReminderPermissionState { app.container.reminders.requestReconciliation() }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val accessibility = LocalAccessibilityManager.current
    val deleted = stringResource(R.string.deleted_task)
    val undo = stringResource(R.string.undo)
    val failure = stringResource(R.string.save_failed)
    val retry = stringResource(R.string.retry)
    val ideaFailure = stringResource(R.string.projects_idea_save_failed)
    LaunchedEffect(tasksModel, accessibility) {
        tasksModel.effects.collect { effect ->
            when (effect) {
                is TaskEffect.Deleted -> {
                    val timeout = accessibility?.calculateRecommendedTimeoutMillis(5_000, containsText = true, containsControls = true) ?: 5_000
                    if (withTimeoutOrNull(timeout) {
                            snackbar.showSnackbar(deleted, undo, withDismissAction = true, duration = SnackbarDuration.Indefinite)
                        } == SnackbarResult.ActionPerformed) tasksModel.restore(effect.id)
                }
                is TaskEffect.Failed -> if (snackbar.showSnackbar(failure, retry, withDismissAction = true,
                    duration = SnackbarDuration.Indefinite) == SnackbarResult.ActionPerformed) effect.retry()
            }
        }
    }
    var filter by rememberSaveable { mutableStateOf(ProjectFilter.ACTIVE) }
    var selectedSyncId by rememberSaveable { mutableStateOf<String?>(null) }
    var ideaEditingSyncId by rememberSaveable { mutableStateOf<String?>(null) }
    var ideaDraft by rememberSaveable { mutableStateOf("") }
    var ideaDialogOpen by rememberSaveable { mutableStateOf(false) }
    var ideaBusy by remember { mutableStateOf(false) }
    var deleteIdea by remember { mutableStateOf<SpaceNote?>(null) }
    val reduced = rememberReducedMotion()
    val projects = remember(tasksState.tasks) { projectTasks(tasksState.tasks) }
    val selected = projects.firstOrNull { it.syncId == selectedSyncId }
        ?: initialProjectSelection(projects)
    LaunchedEffect(selected?.syncId) { if (selectedSyncId == null && selected != null) selectedSyncId = selected.syncId }
    val visible = visibleProjects(projects, filter)
    val projectBySyncId = remember(projects) { projects.associateBy { it.syncId } }
    val fontScale = LocalConfiguration.current.fontScale
    val tasksReady = !tasksState.loading && !tasksState.readFailed && tasksState.now > 0
    val ideaEditing = ideas.firstOrNull { it.syncId == ideaEditingSyncId }
    SpacesTheme {
        Box(Modifier.fillMaxSize().background(SpacesTokens.canvas).testTag("screen-ProjectsSpace")) {
            LazyColumn(Modifier.fillMaxSize().testTag("projects-list"), contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)) {
                item {
                    ProjectsHeader(onBack = { onNavigate(Destination.Spaces) }, onAdd = {
                        tasksModel.edit(Task(title = "", category = Category.PROJECTS))
                    }, enabled = !tasksState.busy)
                }
                item { when {
                    tasksState.loading || tasksState.now <= 0 -> LinearProgressIndicator(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp))
                    tasksState.readFailed -> Text(stringResource(R.string.projects_load_failed),
                        Modifier.padding(horizontal = 20.dp), color = SpacesTokens.secondary)
                    else -> ProjectsHero(selected, tasksState.now, tasksState.zone, reduced, onClick = { selected?.let(tasksModel::edit) })
                } }
                item {
                    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SpacesSectionHeader(stringResource(R.string.projects_all), R.drawable.ic_dock_spaces,
                            stringResource(R.string.projects_add), { tasksModel.edit(Task(title = "", category = Category.PROJECTS)) })
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(ProjectFilter.entries) { choice ->
                                val selectedFilter = filter == choice
                                FilterChip(selected = selectedFilter, onClick = {
                                    filter = choice
                                    val next = visibleProjects(projects, choice)
                                    if (next.none { it.syncId == selected?.syncId }) {
                                        selectedSyncId = (next.firstOrNull() ?: initialProjectSelection(projects))?.syncId
                                    }
                                },
                                    label = { Text(stringResource(when (choice) {
                                        ProjectFilter.ACTIVE -> R.string.projects_filter_active
                                        ProjectFilter.ALL -> R.string.projects_filter_all
                                        ProjectFilter.DONE -> R.string.projects_filter_done
                                    })) }, modifier = Modifier.heightIn(min = 48.dp).testTag("projects-filter-${choice.name}"),
                                    shape = CircleShape, colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SpacesTokens.tangerine.copy(alpha = .16f),
                                        selectedLabelColor = SpacesTokens.tangerine))
                            }
                        }
                    }
                }
                when {
                    tasksState.loading -> item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) }
                    tasksState.readFailed -> item {
                        Text(stringResource(R.string.projects_load_failed), Modifier.padding(horizontal = 20.dp), color = SpacesTokens.secondary)
                        TextButton(onClick = tasksModel::retryRead, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.retry)) }
                    }
                    visible.isEmpty() -> item {
                        ProjectsEmpty(onAdd = { tasksModel.edit(Task(title = "", category = Category.PROJECTS)) })
                    }
                    else -> items(visible, key = { "project-${it.syncId}" }) { task ->
                        ProjectTaskCard(task, selected?.syncId == task.syncId, projectColor(task.syncId), tasksState.now,
                            tasksState.zone, !tasksState.busy, reduced, onSelect = { selectedSyncId = task.syncId },
                            onEdit = { tasksModel.edit(task) }, onComplete = { tasksModel.complete(task, task.status != TaskStatus.DONE) })
                    }
                }
                if (tasksReady) item { ProjectsSprint(projects, tasksState.now, tasksState.zone, fontScale) }
                item {
                    ProjectsIdeaBoard(ideas, selected, projectBySyncId, reduced, ideasReadFailed, onRetry = { ideasReload++ },
                        onAdd = { ideaEditingSyncId = null; ideaDraft = ""; ideaDialogOpen = true },
                        onEdit = { note -> ideaEditingSyncId = note.syncId; ideaDraft = note.body; ideaDialogOpen = true },
                        onDelete = { deleteIdea = it })
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
            tasksState.editor?.let { task ->
                TaskEditorSheet(task, tasksState.now, tasksState.zone, tasksState.busy, snackbar, tasksModel::change, tasksModel::save,
                    { tasksModel.delete(task) }, tasksModel::dismissEditor, reminderPermission = reminderPermission)
            }
            if (ideaDialogOpen) ProjectsIdeaDialog(ideaEditingSyncId != null, ideaDraft, ideaBusy,
                canSave = ideaEditingSyncId == null || ideaEditing != null, onBody = { ideaDraft = it },
                onDismiss = { if (!ideaBusy) { ideaDialogOpen = false; ideaEditingSyncId = null; ideaDraft = "" } },
                onSave = {
                    if (ideaBusy) return@ProjectsIdeaDialog
                    ideaBusy = true
                    scope.launch {
                        try {
                            val editingId = ideaEditingSyncId
                            if (editingId == null) app.container.spaceNotes.saveIdea(selected?.syncId, ideaDraft)
                            else if (ideaEditing != null) app.container.spaceNotes.update(editingId, ideaDraft)
                            else error("The idea is no longer available")
                            ideaDialogOpen = false; ideaEditingSyncId = null; ideaDraft = ""
                        } catch (error: CancellationException) {
                            throw error
                        } catch (_: Exception) {
                            snackbar.showSnackbar(ideaFailure)
                        } finally { ideaBusy = false }
                    }
                })
            deleteIdea?.let { note ->
                AlertDialog(onDismissRequest = { if (!ideaBusy) deleteIdea = null },
                    title = { Text(stringResource(R.string.projects_delete_idea_title)) },
                    text = { Text(stringResource(R.string.projects_delete_idea_body)) },
                    confirmButton = { TextButton(enabled = !ideaBusy, onClick = {
                        if (!ideaBusy) {
                            ideaBusy = true
                            scope.launch {
                                try { app.container.spaceNotes.softDelete(note.syncId); deleteIdea = null }
                                catch (error: CancellationException) { throw error }
                                catch (_: Exception) { snackbar.showSnackbar(ideaFailure) }
                                finally { ideaBusy = false }
                            }
                        }
                    }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.projects_delete_idea_action)) } },
                    dismissButton = { TextButton(enabled = !ideaBusy, onClick = { deleteIdea = null },
                        modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.close)) } })
            }
        }
    }
}

@Composable
private fun ProjectsHeader(onBack: () -> Unit, onAdd: () -> Unit, enabled: Boolean) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SpacesIconButton(R.drawable.ic_chevron_left, stringResource(R.string.spaces_back), onBack)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.spaces_title), color = SpacesTokens.secondary, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.weight(1f))
            FilledIconButton(onClick = onAdd, enabled = enabled, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = SpacesTokens.tangerine, contentColor = SpacesTokens.canvas)) {
                Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_add_project),
                    stringResource(R.string.projects_add_accessibility))
            }
        }
        Text(stringResource(R.string.projects_title), style = MaterialTheme.typography.displayMedium,
            color = SpacesTokens.primary, modifier = Modifier.semantics { heading() })
        Text(stringResource(R.string.projects_subtitle), style = MaterialTheme.typography.bodyMedium,
            color = SpacesTokens.secondary)
    }
}

@Composable
private fun ProjectsHero(task: Task?, now: Long, zone: ZoneId, reduced: Boolean, onClick: () -> Unit) {
    val progressTarget = task?.let(::projectMilestoneFraction) ?: 0f
    val progress by animateFloatAsState(progressTarget, if (reduced) snap() else tween(Motion.medium), label = "project-hero-progress")
    val shape = RoundedCornerShape(24.dp)
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).clip(shape)
        .background(Brush.linearGradient(listOf(SpacesTokens.tangerine.copy(alpha = .13f), SpacesTokens.surface, SpacesTokens.canvas)))
        .border(1.dp, SpacesTokens.tangerine.copy(alpha = .28f), shape)
        .drawWithCache {
            val grid = Color.White.copy(alpha = .035f)
            val glow = Brush.radialGradient(listOf(SpacesTokens.tangerine.copy(alpha = .18f), Color.Transparent),
                Offset(size.width * .92f, size.height * .1f), size.width * .55f)
            onDrawBehind {
                drawRect(glow)
                val spacing = 24.dp.toPx()
                var x = 0f
                while (x < size.width) { drawLine(grid, Offset(x, 0f), Offset(x, size.height), .5.dp.toPx()); x += spacing }
                var y = 0f
                while (y < size.height) { drawLine(grid, Offset(0f, y), Offset(size.width, y), .5.dp.toPx()); y += spacing }
            }
        }.clickable(role = Role.Button, onClick = onClick).testTag("projects-active-hero").padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(8.dp)) { drawCircle(SpacesTokens.tangerine) }
            Spacer(Modifier.width(8.dp))
            Text(stringResource(if (task?.status == TaskStatus.DONE) R.string.projects_selected_done else R.string.projects_active_label),
                color = SpacesTokens.tangerine, style = MaterialTheme.typography.labelMedium)
        }
        if (task == null) {
            Text(stringResource(R.string.projects_no_active), color = SpacesTokens.primary, style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.projects_no_active_hint), color = SpacesTokens.secondary, style = MaterialTheme.typography.bodyMedium)
        } else {
            Text(task.title, color = SpacesTokens.primary, style = MaterialTheme.typography.headlineSmall)
            if (!task.notes.isNullOrBlank()) Text(task.notes, color = SpacesTokens.secondary, style = MaterialTheme.typography.bodyMedium,
                maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (task.checklist.isEmpty()) Text(stringResource(R.string.projects_no_milestones), color = SpacesTokens.secondary,
                style = MaterialTheme.typography.bodySmall)
            else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.projects_milestones, task.checklist.count { it.done }, task.checklist.size),
                        color = SpacesTokens.secondary, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                    Text("${(progressTarget * 100).toInt()}%", color = SpacesTokens.tangerine, style = MaterialTheme.typography.labelLarge)
                }
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = SpacesTokens.tangerine, trackColor = Color.White.copy(alpha = .10f), drawStopIndicator = {})
                val activeIndex = task.checklist.indexOfFirst { !it.done }
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(alpha = .18f)).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    task.checklist.take(if (androidx.compose.ui.platform.LocalDensity.current.fontScale >= 1.3f) 3 else 4)
                        .forEachIndexed { index, milestone ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val color = when { milestone.done -> SpacesTokens.jade; index == activeIndex -> SpacesTokens.tangerine; else -> SpacesTokens.muted }
                                Canvas(Modifier.size(16.dp)) {
                                    drawCircle(color, style = Stroke(1.5.dp.toPx()))
                                    if (milestone.done) drawCircle(color)
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(milestone.title, color = if (index == activeIndex) SpacesTokens.primary else SpacesTokens.secondary,
                                        style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(stringResource(when { milestone.done -> R.string.projects_milestone_done
                                        index == activeIndex -> R.string.projects_milestone_active
                                        else -> R.string.projects_milestone_pending }),
                                        color = color, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    if (task.checklist.size > 4 && androidx.compose.ui.platform.LocalDensity.current.fontScale < 1.3f)
                        Text(stringResource(R.string.projects_more_milestones, task.checklist.size - 4), color = SpacesTokens.secondary,
                            style = MaterialTheme.typography.labelSmall)
                }
            }
            task.deadlineAt?.let { Text(stringResource(R.string.projects_deadline, projectDeadline(it, now, zone, task.hasDeadlineTime)),
                color = SpacesTokens.secondary, style = MaterialTheme.typography.labelMedium) }
        }
    }
}

@Composable
private fun ProjectsEmpty(onAdd: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).clip(RoundedCornerShape(20.dp))
        .background(SpacesTokens.surface).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.projects_empty), color = SpacesTokens.primary, style = MaterialTheme.typography.titleMedium)
        Button(onClick = onAdd, modifier = Modifier.heightIn(min = 48.dp), colors = ButtonDefaults.buttonColors(
            containerColor = SpacesTokens.tangerine, contentColor = SpacesTokens.canvas)) { Text(stringResource(R.string.projects_add)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProjectTaskCard(task: Task, selected: Boolean, accent: Color, now: Long, zone: ZoneId, enabled: Boolean,
                            reduced: Boolean, onSelect: () -> Unit, onEdit: () -> Unit, onComplete: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val done = task.status == TaskStatus.DONE
    val status = stringResource(if (done) R.string.projects_status_done else R.string.projects_status_active)
    val toggleDescription = stringResource(if (done) R.string.projects_reopen_task else R.string.projects_toggle_task, task.title)
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp).clip(shape).background(accent.copy(alpha = .07f))
        .border(1.dp, if (selected) accent.copy(alpha = .55f) else SpacesTokens.edge.copy(alpha = .45f), shape)
        .clickable(role = Role.Button, onClick = onSelect).testTag("project-${task.syncId}"), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(4.dp).heightIn(min = 88.dp).background(accent))
        Column(Modifier.weight(1f).padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(task.title, color = SpacesTokens.primary, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(task.notes?.takeIf { it.isNotBlank() } ?: stringResource(R.string.projects_task_based),
                color = SpacesTokens.secondary, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                Text(status, color = if (done) SpacesTokens.jade else SpacesTokens.secondary, style = MaterialTheme.typography.labelSmall)
                Text(stringResource(task.priority.label()), color = SpacesTokens.secondary, style = MaterialTheme.typography.labelSmall)
                task.deadlineAt?.let { Text(projectDeadline(it, now, zone, task.hasDeadlineTime), color = SpacesTokens.secondary,
                    style = MaterialTheme.typography.labelSmall) }
            }
            if (task.checklist.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val fraction = task.checklist.count { it.done }.toFloat() / task.checklist.size
                    val animated by animateFloatAsState(fraction, if (reduced) snap() else tween(Motion.short), label = "project-row-${task.syncId}")
                    LinearProgressIndicator(progress = { animated }, modifier = Modifier.weight(1f).height(4.dp),
                        color = accent, trackColor = SpacesTokens.edge, drawStopIndicator = {})
                    Text(stringResource(R.string.projects_milestones, task.checklist.count { it.done }, task.checklist.size),
                        color = SpacesTokens.secondary, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TextButton(onClick = onEdit, enabled = enabled, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.projects_open), color = SpacesTokens.tangerine)
            }
            Box(Modifier.size(48.dp).toggleable(value = done, enabled = enabled, role = Role.Checkbox,
                onValueChange = { onComplete() }).semantics { contentDescription = toggleDescription; stateDescription = status },
                contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(22.dp)) {
                    drawCircle(if (done) SpacesTokens.jade else accent, style = Stroke(1.5.dp.toPx()))
                    if (done) {
                        drawCircle(SpacesTokens.jade)
                        val check = Path().apply {
                            moveTo(size.width * .25f, size.height * .52f)
                            lineTo(size.width * .44f, size.height * .69f)
                            lineTo(size.width * .77f, size.height * .32f)
                        }
                        drawPath(check, Color.White, style = Stroke(1.8.dp.toPx()))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectsSprint(tasks: List<Task>, now: Long, zone: ZoneId, fontScale: Float) {
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val days = (0..6).map { today.plusDays(it.toLong()) }
    val due = remember(tasks, days, zone) { tasks.filter { it.status != TaskStatus.DONE }.mapNotNull { task ->
        task.deadlineAt?.let { task to Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
    }.filter { it.second in days }.groupBy({ it.second }, { it.first }) }
    Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp))
        .background(SpacesTokens.row).border(1.dp, SpacesTokens.edge.copy(alpha = .35f), RoundedCornerShape(18.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.projects_sprint), color = SpacesTokens.primary, style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.semantics { heading() })
            Text(stringResource(R.string.projects_sprint_range, days.first().format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())),
                days.last().format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))), color = SpacesTokens.secondary,
                style = MaterialTheme.typography.labelSmall)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            days.forEach { date ->
                val dueTasks = due[date].orEmpty()
                val color = dueTasks.firstOrNull()?.let { projectColor(it.syncId) } ?: SpacesTokens.muted
                Column(Modifier.weight(1f).heightIn(min = 56.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(date.format(DateTimeFormatter.ofPattern(if (fontScale >= 1.3f) "EEEEE" else "EE", Locale.getDefault())),
                        color = SpacesTokens.secondary, style = MaterialTheme.typography.labelSmall)
                    Box(Modifier.sizeIn(minWidth = 36.dp, minHeight = 36.dp).clip(RoundedCornerShape(10.dp))
                        .background(if (date == today) SpacesTokens.tangerine else if (dueTasks.isNotEmpty()) SpacesTokens.pressed else Color.Transparent),
                        contentAlignment = Alignment.Center) {
                        Text(date.dayOfMonth.toString(), color = if (date == today) SpacesTokens.canvas else SpacesTokens.primary,
                            style = MaterialTheme.typography.labelLarge)
                    }
                    Canvas(Modifier.size(5.dp)) { drawCircle(if (dueTasks.isEmpty()) Color.Transparent else color) }
                }
            }
        }
    }
}

@Composable
private fun ProjectsIdeaBoard(ideas: List<SpaceNote>, selected: Task?, projects: Map<String, Task>, reduced: Boolean,
                              readFailed: Boolean, onRetry: () -> Unit,
                              onAdd: () -> Unit, onEdit: (SpaceNote) -> Unit, onDelete: (SpaceNote) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).clip(RoundedCornerShape(18.dp))
        .background(SpacesTokens.row).border(1.dp, SpacesTokens.edge.copy(alpha = .35f), RoundedCornerShape(18.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.projects_ideas), color = SpacesTokens.primary, style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f).semantics { heading() })
            TextButton(onClick = onAdd, modifier = Modifier.heightIn(min = 48.dp).testTag("project-idea-add")) {
                Text(stringResource(R.string.projects_add_idea), color = SpacesTokens.tangerine)
            }
        }
        if (selected != null) Text(stringResource(R.string.projects_idea_target, selected.title), color = SpacesTokens.secondary,
            style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (readFailed) {
            Text(stringResource(R.string.projects_ideas_read_failed), color = SpacesTokens.secondary, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.retry)) }
        } else if (ideas.isEmpty()) Text(stringResource(R.string.projects_ideas_empty), color = SpacesTokens.secondary, style = MaterialTheme.typography.bodySmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
            items(ideas, key = { it.syncId }) { note ->
                val tone = projectIdeaColors[Math.floorMod(note.syncId.hashCode(), projectIdeaColors.size)]
                Column(Modifier.widthIn(min = 120.dp, max = 180.dp).heightIn(min = 100.dp)
                    .graphicsLayer { rotationZ = if (reduced) 0f else when (Math.floorMod(note.syncId.hashCode(), 3)) { 0 -> -1.5f; 1 -> .8f; else -> -.4f } }
                    .clip(RoundedCornerShape(12.dp)).background(tone).clickable(onClick = { onEdit(note) })
                    .padding(12.dp).testTag("project-idea-${note.syncId}"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(note.body, color = SpacesTokens.canvas, style = MaterialTheme.typography.bodySmall, maxLines = 4, overflow = TextOverflow.Ellipsis)
                    val target = note.projectTaskSyncId?.let(projects::get)
                    Text(target?.title ?: if (note.projectTaskSyncId == null) stringResource(R.string.projects_idea_unassigned)
                        else stringResource(R.string.projects_idea_orphaned), color = SpacesTokens.canvas.copy(alpha = .72f),
                        style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    TextButton(onClick = { onDelete(note) }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.projects_delete_idea_action), color = SpacesTokens.canvas)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectsIdeaDialog(editing: Boolean, body: String, busy: Boolean, canSave: Boolean, onBody: (String) -> Unit,
                               onDismiss: () -> Unit, onSave: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(stringResource(if (editing) R.string.projects_edit_idea else R.string.projects_new_idea)) },
        text = {
            OutlinedTextField(value = body, onValueChange = { onBody(it.take(10_000)) }, enabled = !busy,
                label = { Text(stringResource(R.string.projects_idea_body)) }, minLines = 3, maxLines = 8,
                supportingText = { Text(stringResource(R.string.projects_idea_limit, body.length)) },
                modifier = Modifier.fillMaxWidth().testTag("project-idea-body"))
        },
        confirmButton = { TextButton(onClick = onSave, enabled = !busy && canSave && body.isNotBlank()) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text(stringResource(R.string.close)) } })
}

private fun projectColor(syncId: String): Color = projectColors[projectIdentityIndex(syncId, projectColors.size)]
private val projectColors = listOf(SpacesTokens.jade, SpacesTokens.ultramarine, SpacesTokens.amber, SpacesTokens.magenta, SpacesTokens.tangerine)
private val projectIdeaColors = listOf(SpacesTokens.tangerine, SpacesTokens.amber, SpacesTokens.jade)

private fun projectDeadline(epoch: Long, now: Long, zone: ZoneId, hasTime: Boolean): String {
    val deadline = Instant.ofEpochMilli(epoch).atZone(zone)
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val time = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    return when {
        hasTime && deadline.toLocalDate() == today -> time.format(deadline)
        hasTime -> DateTimeFormatter.ofPattern("EEE, d MMM · HH:mm", Locale.getDefault()).format(deadline)
        else -> DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()).format(deadline)
    }
}
