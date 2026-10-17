package id.riko.hub.ui.spaces

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import id.riko.hub.R
import id.riko.hub.RikoHubApp
import id.riko.hub.domain.model.Category
import id.riko.hub.domain.model.SpaceNote
import id.riko.hub.domain.model.Task
import id.riko.hub.domain.model.TaskStatus
import id.riko.hub.navigation.Destination
import id.riko.hub.ui.components.TaskEditorPresentation
import id.riko.hub.ui.components.TaskEditorSheet
import id.riko.hub.ui.components.rememberReminderPermissionState
import id.riko.hub.ui.components.rikoFormColorScheme
import id.riko.hub.ui.components.rikoFormFieldColors
import id.riko.hub.ui.tasks.TaskEffect
import id.riko.hub.ui.tasks.TasksViewModel
import id.riko.hub.ui.theme.ChefButter
import id.riko.hub.ui.theme.ChefCanvas
import id.riko.hub.ui.theme.ChefCoral
import id.riko.hub.ui.theme.ChefCyan
import id.riko.hub.ui.theme.ChefGround
import id.riko.hub.ui.theme.ChefInk
import id.riko.hub.ui.theme.ChefLime
import id.riko.hub.ui.theme.ChefLine
import id.riko.hub.ui.theme.ChefMuted
import id.riko.hub.ui.theme.ChefPanel
import id.riko.hub.ui.theme.ChefPeriwinkle
import id.riko.hub.ui.theme.ChefRaised
import id.riko.hub.ui.theme.ChefSage
import id.riko.hub.ui.theme.Motion
import id.riko.hub.ui.theme.NotoCondensedBlack
import id.riko.hub.ui.theme.RikoShapes
import id.riko.hub.ui.theme.Spacing
import id.riko.hub.ui.theme.rememberReducedMotion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
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
    var deckIndex by rememberSaveable { mutableIntStateOf(0) }
    var ideaEditingSyncId by rememberSaveable { mutableStateOf<String?>(null) }
    var ideaDraft by rememberSaveable { mutableStateOf("") }
    var ideaDialogOpen by rememberSaveable { mutableStateOf(false) }
    var ideaBusy by remember { mutableStateOf(false) }
    var deleteIdea by remember { mutableStateOf<SpaceNote?>(null) }
    val reduced = rememberReducedMotion()
    val projects = remember(tasksState.tasks) { projectTasks(tasksState.tasks) }
    val visible = remember(projects, filter) { visibleProjects(projects, filter) }
    val selected = visible.getOrNull(projectDeckIndex(deckIndex, visible.size)) ?: projects.firstOrNull()
    val projectBySyncId = remember(projects) { projects.associateBy { it.syncId } }
    val fontScale = LocalConfiguration.current.fontScale
    val tasksReady = !tasksState.loading && !tasksState.readFailed && tasksState.now > 0
    val ideaEditing = ideas.firstOrNull { it.syncId == ideaEditingSyncId }

    Box(Modifier.fillMaxSize().background(ChefCanvas).testTag("screen-ProjectsSpace")) {
        LazyColumn(
            Modifier.fillMaxSize().testTag("projects-list"),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1. Header Bar (Back circle, REVENUE title, Plus circle)
            item {
                ProjectsRevenueHeader(
                    onBack = { onNavigate(Destination.Spaces) },
                    onAdd = { tasksModel.edit(Task(title = "", category = Category.PROJECTS)) },
                    enabled = !tasksState.busy,
                )
            }

            // 2. Joined Double Pill Filter (Skripsi Utama ⌄ | Sprint 42 ⌄)
            item {
                JoinedDoublePillFilter(
                    projectTitle = selected?.title ?: "Skripsi Utama",
                    filter = filter,
                    onFilterChange = {
                        filter = it
                        deckIndex = 0
                    },
                )
            }

            // 3. MASSIVE CORAL HERO REVENUE CARD (M-CHEF signature)
            item {
                RevenueHeroCard(
                    task = selected,
                    totalProjects = projects.size,
                    now = tasksState.now,
                    zone = tasksState.zone,
                )
            }

            // 4. Loading / Error / Empty States
            when {
                tasksState.loading -> item {
                    LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 20.dp), color = ChefLime)
                }
                tasksState.readFailed -> item {
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        Text(stringResource(R.string.projects_load_failed), color = ChefMuted)
                        TextButton(onClick = tasksModel::retryRead, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text(stringResource(R.string.retry), color = ChefLime)
                        }
                    }
                }
                visible.isEmpty() -> item {
                    ProjectsEmpty(onAdd = { tasksModel.edit(Task(title = "", category = Category.PROJECTS)) })
                }
                else -> item {
                    // Filter Chips row (Active, All, Done)
                    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "DAFTAR TUGAS PROYEK",
                                fontFamily = NotoCondensedBlack,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = ChefMuted,
                                modifier = Modifier.semantics { heading() },
                            )
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = ChefPanel,
                                border = BorderStroke(1.dp, ChefLine),
                            ) {
                                Text(
                                    text = "${visible.size} AKTIF",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ChefLime,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(ProjectFilter.entries) { choice ->
                                val selectedFilter = filter == choice
                                FilterChip(
                                    selected = selectedFilter,
                                    onClick = {
                                        filter = choice
                                        deckIndex = 0
                                    },
                                    label = {
                                        Text(stringResource(when (choice) {
                                            ProjectFilter.ACTIVE -> R.string.projects_filter_active
                                            ProjectFilter.ALL -> R.string.projects_filter_all
                                            ProjectFilter.DONE -> R.string.projects_filter_done
                                        }))
                                    },
                                    modifier = Modifier.heightIn(min = 48.dp).testTag("projects-filter-${choice.name}"),
                                    shape = CircleShape,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ChefLime,
                                        selectedLabelColor = ChefGround,
                                        containerColor = ChefPanel,
                                        labelColor = ChefMuted,
                                    ),
                                )
                            }
                        }
                    }
                }
            }

            // 5. Interactive Projects Card Deck
            if (visible.isNotEmpty()) {
                item {
                    ProjectsDeck(
                        tasks = visible,
                        index = deckIndex,
                        now = tasksState.now,
                        zone = tasksState.zone,
                        enabled = !tasksState.busy,
                        reduced = reduced,
                        onIndexChange = { index -> deckIndex = index },
                        onEdit = tasksModel::edit,
                        onComplete = { task -> tasksModel.complete(task, task.status != TaskStatus.DONE) },
                    )
                }
            }

            // 6. Sprint Range strip
            if (tasksReady) {
                item { ProjectsSprint(projects, tasksState.now, tasksState.zone, fontScale) }
            }

            // 7. TILTED DELIVERABLES & IDEAS (Section Title + Stack)
            item {
                ProjectsTiltedDeliverablesSection(
                    ideas = ideas,
                    selected = selected,
                    projectBySyncId = projectBySyncId,
                    reduced = reduced,
                    ideasReadFailed = ideasReadFailed,
                    onRetry = { ideasReload++ },
                    onAdd = { ideaEditingSyncId = null; ideaDraft = ""; ideaDialogOpen = true },
                    onEdit = { note -> ideaEditingSyncId = note.syncId; ideaDraft = note.body; ideaDialogOpen = true },
                    onDelete = { deleteIdea = it },
                )
            }

            // 8. Wide Bottom Add Action Button (M-CHEF full pill button)
            item {
                Surface(
                    onClick = { tasksModel.edit(Task(title = "", category = Category.PROJECTS)) },
                    enabled = !tasksState.busy,
                    shape = RoundedCornerShape(999.dp),
                    color = ChefPanel,
                    border = BorderStroke(1.dp, ChefLine),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(52.dp)
                        .testTag("projects-wide-action"),
                ) {
                    Row(
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "＋ TAMBAH TUGAS SPRINT BARU",
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

        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    // Task Editor Sheet
    tasksState.editor?.let { task ->
        TaskEditorSheet(
            task = task,
            now = tasksState.now,
            zone = tasksState.zone,
            busy = tasksState.busy,
            snackbar = snackbar,
            onChange = tasksModel::change,
            onSave = tasksModel::save,
            onDelete = { tasksModel.delete(task) },
            onDismiss = tasksModel::dismissEditor,
            reminderPermission = reminderPermission,
            presentation = TaskEditorPresentation.PROJECT,
        )
    }

    // Idea Dialog
    if (ideaDialogOpen) {
        ProjectsIdeaDialog(
            editing = ideaEditingSyncId != null,
            body = ideaDraft,
            busy = ideaBusy,
            canSave = ideaEditingSyncId == null || ideaEditing != null,
            onBody = { ideaDraft = it },
            onDismiss = {
                if (!ideaBusy) {
                    ideaDialogOpen = false
                    ideaEditingSyncId = null
                    ideaDraft = ""
                }
            },
            onSave = {
                if (ideaBusy) return@ProjectsIdeaDialog
                ideaBusy = true
                scope.launch {
                    try {
                        val editingId = ideaEditingSyncId
                        if (editingId == null) app.container.spaceNotes.saveIdea(selected?.syncId, ideaDraft)
                        else if (ideaEditing != null) app.container.spaceNotes.update(editingId, ideaDraft)
                        else error("The idea is no longer available")
                        ideaDialogOpen = false
                        ideaEditingSyncId = null
                        ideaDraft = ""
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Exception) {
                        snackbar.showSnackbar(ideaFailure)
                    } finally { ideaBusy = false }
                }
            },
        )
    }

    // Delete Idea Dialog
    deleteIdea?.let { note ->
        AlertDialog(
            onDismissRequest = { if (!ideaBusy) deleteIdea = null },
            title = { Text(stringResource(R.string.projects_delete_idea_title)) },
            text = { Text(stringResource(R.string.projects_delete_idea_body)) },
            confirmButton = {
                TextButton(
                    enabled = !ideaBusy,
                    onClick = {
                        if (!ideaBusy) {
                            ideaBusy = true
                            scope.launch {
                                try {
                                    app.container.spaceNotes.softDelete(note.syncId)
                                    deleteIdea = null
                                } catch (error: CancellationException) {
                                    throw error
                                } catch (_: Exception) {
                                    snackbar.showSnackbar(ideaFailure)
                                } finally { ideaBusy = false }
                            }
                        }
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.projects_delete_idea_action)) }
            },
            dismissButton = {
                TextButton(
                    enabled = !ideaBusy,
                    onClick = { deleteIdea = null },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.close), color = ChefMuted) }
            },
        )
    }
}

@Composable
private fun ProjectsRevenueHeader(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    enabled: Boolean,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Back circle button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable(onClick = onBack)
                .semantics {
                    role = Role.Button
                    contentDescription = "Kembali ke Ruang"
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "←", color = ChefGround, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }

        // Title
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "GROSS REVENUE / SPRINT",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
                color = ChefMuted,
            )
            Text(
                text = "REVENUE",
                fontFamily = NotoCondensedBlack,
                fontWeight = FontWeight.Black,
                fontSize = 26.sp,
                color = ChefInk,
                modifier = Modifier.semantics { heading() },
            )
        }

        // Add circle button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable(enabled = enabled, onClick = onAdd)
                .semantics {
                    role = Role.Button
                    contentDescription = "Tambah tugas sprint baru"
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "＋", color = ChefGround, fontSize = 20.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun JoinedDoublePillFilter(
    projectTitle: String,
    filter: ProjectFilter,
    onFilterChange: (ProjectFilter) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = ChefPanel,
        border = BorderStroke(1.dp, ChefLine),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left Half: Project Title Dropdown
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = projectTitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = ChefInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Text("⌄", fontSize = 12.sp, color = ChefMuted, fontWeight = FontWeight.Bold)
                }
            }

            // Vertical divider
            Box(Modifier.width(1.dp).height(20.dp).background(ChefLine))

            // Right Half: Sprint Filter Dropdown
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable {
                        val next = when (filter) {
                            ProjectFilter.ACTIVE -> ProjectFilter.ALL
                            ProjectFilter.ALL -> ProjectFilter.DONE
                            ProjectFilter.DONE -> ProjectFilter.ACTIVE
                        }
                        onFilterChange(next)
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "Sprint 42 · ${filter.name}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = ChefInk,
                    )
                    Text("⌄", fontSize = 12.sp, color = ChefMuted, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RevenueHeroCard(
    task: Task?,
    totalProjects: Int,
    now: Long,
    zone: ZoneId,
) {
    val title = task?.title ?: "SKRIPSI · BAB 3"
    val checklist = task?.checklist.orEmpty()
    val doneCount = checklist.count { it.done }
    val totalCount = checklist.size.coerceAtLeast(1)
    val milestoneText = if (checklist.isNotEmpty()) "$doneCount/$totalCount SPRINT" else "3/4 SPRINT"

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = ChefCoral,
        contentColor = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Row 1: Gross revenue label & +7.5% pill
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "GROSS REVENUE / SPRINT",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 0.8.sp),
                    color = Color.White.copy(alpha = 0.85f),
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.22f),
                ) {
                    Text(
                        text = "+7.5%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }

            // Big Title
            Text(
                text = title.uppercase(),
                fontFamily = NotoCondensedBlack,
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                lineHeight = 30.sp,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            // Row 2: Target & +2.4%
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "TARGET PENYELESAIAN",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 0.8.sp),
                    color = Color.White.copy(alpha = 0.85f),
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.22f),
                ) {
                    Text(
                        text = "+2.4%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }

            // Subline: Date & description
            Column {
                val deadlineDate = task?.deadlineAt?.let {
                    Instant.ofEpochMilli(it).atZone(zone).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("id-ID")))
                } ?: "18 OKT 2026"
                Text(
                    text = deadlineDate,
                    fontFamily = NotoCondensedBlack,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    color = Color.White,
                )
                Text(
                    text = task?.notes?.takeIf { it.isNotBlank() } ?: "Batas Bimbingan Dosen & Seminar Proposal",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Striped Hatched Burndown Bar Chart Stage
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.14f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Floating active milestone pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text(
                            text = milestoneText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                            color = ChefGround,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }

                    // 5 Burndown Bars Row (M1 to M5)
                    val barFills = listOf(0.75f, 0.85f, 0.90f, 0.40f, 0.20f)
                    val barLabels = listOf("M1", "M2", "M3", "M4", "M5")
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        barFills.forEachIndexed { i, fill ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Box(
                                    Modifier
                                        .size(width = 38.dp, height = 54.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.BottomCenter,
                                ) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .height((54 * fill).dp)
                                            .background(if (i == 2) Color.White else Color.White.copy(alpha = 0.65f)),
                                    )
                                }
                                Text(
                                    text = barLabels[i],
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.85f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectsDeck(
    tasks: List<Task>,
    index: Int,
    now: Long,
    zone: ZoneId,
    enabled: Boolean,
    reduced: Boolean,
    onIndexChange: (Int) -> Unit,
    onEdit: (Task) -> Unit,
    onComplete: (Task) -> Unit,
) {
    val count = tasks.size
    val currentIndex = projectDeckIndex(index, count)
    val front = tasks.getOrNull(currentIndex) ?: return
    val nextIndex = (currentIndex + 1) % count
    val afterNext = (currentIndex + 2) % count
    val x = remember { androidx.compose.animation.core.Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val context = LocalContext.current
    val threshold = with(density) { 72.dp.toPx() }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 260.dp),
            contentAlignment = Alignment.Center,
        ) {
            val cardWidth = constraints.maxWidth.toFloat()
            if (count > 2) DeckCardBack(tasks[afterNext], 2)
            if (count > 1) DeckCardBack(tasks[nextIndex], 1)
            Box(
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationX = x.value
                        rotationZ = (x.value / cardWidth.coerceAtLeast(1f) * 8f).coerceIn(-8f, 8f)
                    }
                    .pointerInput(count, currentIndex, reduced) {
                        var drag = 0f
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { change, amount ->
                                drag += amount
                                change.consume()
                                scope.launch {
                                    x.snapTo((x.value + amount).coerceIn(-cardWidth * .9f, cardWidth * .9f))
                                }
                            },
                            onDragEnd = {
                                if (kotlin.math.abs(drag) >= threshold && count > 1) {
                                    val direction = if (drag < 0) 1 else -1
                                    scope.launch {
                                        x.animateTo(cardWidth * direction, if (reduced) snap() else tween(Motion.short))
                                        onIndexChange(nextProjectDeckIndex(currentIndex, direction, count))
                                        x.snapTo(0f)
                                    }
                                } else scope.launch { x.animateTo(0f, if (reduced) snap() else tween(Motion.short)) }
                            },
                            onDragCancel = { scope.launch { x.animateTo(0f, if (reduced) snap() else tween(Motion.short)) } },
                        )
                    },
            ) {
                ProjectTaskCard(
                    task = front,
                    accent = projectColor(front.syncId),
                    now = now,
                    zone = zone,
                    enabled = enabled,
                    reduced = reduced,
                    onEdit = { onEdit(front) },
                    onComplete = { onComplete(front) },
                )
            }
        }

        // Stepper Navigation Buttons
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                onClick = { onIndexChange(nextProjectDeckIndex(currentIndex, -1, count)) },
                enabled = count > 1,
                shape = RoundedCornerShape(999.dp),
                color = ChefPanel,
                border = BorderStroke(1.dp, ChefLine),
                modifier = Modifier.heightIn(min = 40.dp),
            ) {
                Box(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text("← Sebelumnya", color = ChefInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Text(
                text = "${currentIndex + 1} / $count",
                fontFamily = NotoCondensedBlack,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                color = ChefMuted,
            )

            Surface(
                onClick = { onIndexChange(nextProjectDeckIndex(currentIndex, 1, count)) },
                enabled = count > 1,
                shape = RoundedCornerShape(999.dp),
                color = ChefPanel,
                border = BorderStroke(1.dp, ChefLine),
                modifier = Modifier.heightIn(min = 40.dp),
            ) {
                Box(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text("Berikutnya →", color = ChefInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DeckCardBack(task: Task, depth: Int) {
    val shape = RoundedCornerShape(22.dp)
    val accent = projectColor(task.syncId)
    val offsetY = if (depth == 1) (-10).dp else (-18).dp
    val scale = if (depth == 1) 0.95f else 0.90f
    val rotation = if (depth == 1) 2.2f else -2.5f

    Surface(
        shape = shape,
        color = accent.copy(alpha = if (depth == 1) 0.35f else 0.2f),
        border = BorderStroke(1.dp, ChefLine.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .graphicsLayer {
                translationY = offsetY.toPx()
                scaleX = scale
                scaleY = scale
                rotationZ = rotation
            }
            .height(210.dp)
            .padding(16.dp),
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
            Text(
                text = task.title,
                color = ChefInk.copy(alpha = 0.7f),
                fontFamily = NotoCondensedBlack,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ProjectTaskCard(
    task: Task,
    accent: Color,
    now: Long,
    zone: ZoneId,
    enabled: Boolean,
    reduced: Boolean,
    onEdit: () -> Unit,
    onComplete: () -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    val done = task.status == TaskStatus.DONE
    val status = stringResource(if (done) R.string.projects_status_done else R.string.projects_status_active)
    val toggleDescription = stringResource(if (done) R.string.projects_reopen_task else R.string.projects_toggle_task, task.title)

    Surface(
        shape = shape,
        color = ChefPanel,
        border = BorderStroke(1.5.dp, if (done) ChefLine else accent.copy(alpha = 0.7f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .testTag("project-${task.syncId}"),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(accent))
                    Text(
                        text = "TASK PROYEK",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = ChefMuted,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (done) ChefLine.copy(alpha = 0.3f) else accent.copy(alpha = 0.15f),
                ) {
                    Text(
                        text = status,
                        color = if (done) ChefMuted else accent,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }

            Text(
                text = task.title,
                fontFamily = NotoCondensedBlack,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                color = ChefInk,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (!task.notes.isNullOrBlank()) {
                Text(
                    text = task.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = ChefMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (task.checklist.isNotEmpty()) {
                val doneCount = task.checklist.count { it.done }
                val total = task.checklist.size
                val fraction = doneCount.toFloat() / total
                val animated by animateFloatAsState(
                    fraction,
                    if (reduced) snap() else tween(Motion.short),
                    label = "project-progress-${task.syncId}",
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "$doneCount dari $total tahapan",
                            style = MaterialTheme.typography.labelSmall,
                            color = ChefMuted,
                        )
                        Text(
                            text = "${(fraction * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = ChefInk,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { animated },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = accent,
                        trackColor = ChefGround,
                        drawStopIndicator = {},
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                task.deadlineAt?.let {
                    Text(
                        text = "Tenggat: " + projectDeadline(it, now, zone, task.hasDeadlineTime),
                        style = MaterialTheme.typography.labelSmall,
                        color = ChefMuted,
                    )
                } ?: Spacer(Modifier.width(1.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = onEdit,
                        enabled = enabled,
                        modifier = Modifier.heightIn(min = 40.dp),
                    ) {
                        Text("Buka ↗", color = ChefLime, fontWeight = FontWeight.Bold)
                    }
                    IconButton(
                        onClick = onComplete,
                        enabled = enabled,
                        modifier = Modifier
                            .size(40.dp)
                            .semantics {
                                contentDescription = toggleDescription
                                stateDescription = status
                            },
                    ) {
                        Box(
                            Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, if (done) ChefLime else ChefLine, CircleShape)
                                .background(if (done) ChefLime else Color.Transparent),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (done) Text("✓", color = ChefGround, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
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
    val due = remember(tasks, days, zone) {
        tasks.filter { it.status != TaskStatus.DONE }.mapNotNull { task ->
            task.deadlineAt?.let { task to Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
        }.filter { it.second in days }.groupBy({ it.second }, { it.first })
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ChefPanel,
        border = BorderStroke(1.dp, ChefLine),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "AGENDA PEKANAN PROYEK",
                    fontFamily = NotoCondensedBlack,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = ChefInk,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = "${days.first().format(DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("id-ID")))} - ${days.last().format(DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("id-ID")))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ChefMuted,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                days.forEach { date ->
                    val dueTasks = due[date].orEmpty()
                    val isToday = date == today
                    Column(
                        Modifier.weight(1f).heightIn(min = 54.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = date.format(DateTimeFormatter.ofPattern(if (fontScale >= 1.3f) "EEEEE" else "EE", Locale.forLanguageTag("id-ID"))),
                            color = ChefMuted,
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isToday) ChefLime else if (dueTasks.isNotEmpty()) ChefRaised else Color.Transparent),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = date.dayOfMonth.toString(),
                                color = if (isToday) ChefGround else ChefInk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectsTiltedDeliverablesSection(
    ideas: List<SpaceNote>,
    selected: Task?,
    projectBySyncId: Map<String, Task>,
    reduced: Boolean,
    ideasReadFailed: Boolean,
    onRetry: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (SpaceNote) -> Unit,
    onDelete: (SpaceNote) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Section Title
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "BERKAS DELIVERABLE PROYEK",
                    fontFamily = NotoCondensedBlack,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = ChefInk,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = "Catatan & dokumen pendukung sprint",
                    style = MaterialTheme.typography.bodySmall,
                    color = ChefMuted,
                )
            }
            TextButton(
                onClick = onAdd,
                modifier = Modifier.testTag("project-idea-add"),
            ) {
                Text("＋ IDE ↗", color = ChefLime, fontWeight = FontWeight.Bold)
            }
        }

        // Tilted Deliverables Cards (M-CHEF signature from reference Image 3)
        // 1. Coral Tilted Card (-3.5 deg)
        TiltedDeliverableCard(
            title = "DRAFT BAB 3 (REVISI)",
            status = "Dalam Review",
            monogram = "DOCX",
            color = ChefCoral,
            textColor = Color.White,
            rotation = if (reduced) 0f else -3.5f,
            onClick = onAdd,
        )

        // 2. Mustard Yellow Tilted Card (+2.5 deg)
        TiltedDeliverableCard(
            title = "DATASET SURVEY 30 RESPONDEN",
            status = "✔ Terverifikasi",
            monogram = "CSV",
            color = ChefButter,
            textColor = ChefGround,
            rotation = if (reduced) 0f else 2.5f,
            onClick = onAdd,
        )

        // 3. Mint / Cyan Tilted Card (-2.0 deg)
        TiltedDeliverableCard(
            title = "SLIDE SEMINAR PROPOSAL",
            status = "✔ Siap Presentasi",
            monogram = "PPTX",
            color = ChefCyan,
            textColor = ChefGround,
            rotation = if (reduced) 0f else -2.0f,
            onClick = onAdd,
        )

        // Real Ideas from SpaceNotes Database (if present)
        if (ideas.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "PAPAN CATATAN & GAGASAN",
                fontFamily = NotoCondensedBlack,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                color = ChefMuted,
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                items(ideas, key = { it.syncId }) { note ->
                    val tone = when (Math.floorMod(note.syncId.hashCode(), 3)) {
                        0 -> ChefCoral
                        1 -> ChefButter
                        else -> ChefSage
                    }
                    val textColor = if (tone == ChefCoral) Color.White else ChefGround
                    Column(
                        Modifier
                            .widthIn(min = 140.dp, max = 200.dp)
                            .heightIn(min = 110.dp)
                            .graphicsLayer { rotationZ = if (reduced) 0f else when (Math.floorMod(note.syncId.hashCode(), 3)) { 0 -> -1.5f; 1 -> .8f; else -> -.4f } }
                            .clip(RoundedCornerShape(14.dp))
                            .background(tone)
                            .clickable(onClick = { onEdit(note) })
                            .padding(12.dp)
                            .testTag("project-idea-${note.syncId}"),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = note.body,
                            color = textColor,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Hapus",
                                color = textColor.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { onDelete(note) },
                            )
                            Text("↗", color = textColor, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TiltedDeliverableCard(
    title: String,
    status: String,
    monogram: String,
    color: Color,
    textColor: Color,
    rotation: Float,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color,
        border = BorderStroke(2.dp, ChefGround),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { rotationZ = rotation }
            .clickable(onClick = onClick),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    fontFamily = NotoCondensedBlack,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = textColor,
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (textColor == Color.White) Color.White.copy(alpha = 0.25f) else ChefGround.copy(alpha = 0.12f),
                ) {
                    Text(
                        text = status,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }

            // Square Monogram Tile (DOCX, CSV, PPTX)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ChefGround,
                modifier = Modifier.size(46.dp),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = monogram,
                        fontFamily = NotoCondensedBlack,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectsIdeaDialog(
    editing: Boolean,
    body: String,
    busy: Boolean,
    canSave: Boolean,
    onBody: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    MaterialTheme(colorScheme = rikoFormColorScheme()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = ChefPanel,
            title = {
                Text(
                    text = if (editing) "Edit Ide Proyek" else "Catat Ide Proyek Baru",
                    fontFamily = NotoCondensedBlack,
                    fontWeight = FontWeight.Black,
                    color = ChefInk,
                )
            },
            text = {
                OutlinedTextField(
                    value = body,
                    onValueChange = onBody,
                    placeholder = { Text("Tulis ide atau catatan pendukung...") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                    colors = rikoFormFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = canSave && !busy && body.isNotBlank(),
                    onClick = onSave,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.save), color = ChefLime, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(
                    enabled = !busy,
                    onClick = onDismiss,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.close), color = ChefMuted) }
            },
        )
    }
}

@Composable
private fun ProjectsEmpty(onAdd: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = ChefPanel,
        border = BorderStroke(1.dp, ChefLine),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        Column(
            Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                stringResource(R.string.projects_empty),
                fontFamily = NotoCondensedBlack,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                color = ChefInk,
            )
            Text(
                stringResource(R.string.projects_no_active_hint),
                color = ChefMuted,
                style = MaterialTheme.typography.bodySmall,
            )
            Button(
                onClick = onAdd,
                modifier = Modifier.heightIn(min = 48.dp),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ChefLime,
                    contentColor = ChefGround,
                ),
            ) {
                Text(stringResource(R.string.projects_add), fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun projectColor(syncId: String): Color = projectColors[projectIdentityIndex(syncId, projectColors.size)]
private val projectColors: List<Color> = listOf(ChefLime, ChefPeriwinkle, ChefButter, ChefCoral, ChefCyan, ChefSage)

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
