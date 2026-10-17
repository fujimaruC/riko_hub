package id.riko.hub.ui.spaces

import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import id.riko.hub.R
import id.riko.hub.RikoHubApp
import id.riko.hub.data.remote.drive.DriveFile
import id.riko.hub.domain.model.Category
import id.riko.hub.domain.model.Course
import id.riko.hub.domain.model.Event
import id.riko.hub.domain.model.EventKind
import id.riko.hub.domain.model.Task
import id.riko.hub.domain.model.TaskStatus
import id.riko.hub.ui.academic.CourseAssignmentsSection
import id.riko.hub.ui.components.TaskEditorSheet
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
import id.riko.hub.ui.theme.ChefRaised
import id.riko.hub.ui.theme.ChefSage
import id.riko.hub.ui.theme.NotoCondensedBlack
import id.riko.hub.ui.theme.Spacing
import id.riko.hub.ui.vault.VaultUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

@Composable
fun AcademicsSpaceScreen(
    onNavigate: (id.riko.hub.navigation.Destination) -> Unit,
    vaultState: VaultUiState = VaultUiState(),
    onOpenMaterial: ((DriveFile) -> Unit)? = null,
) {
    val app = LocalContext.current.applicationContext as RikoHubApp
    val clock = app.container.clock
    val quickAddModel: TasksViewModel = viewModel(key = "academics-quick-add", factory = viewModelFactory {
        initializer { TasksViewModel(app.container.tasks, clock) }
    })
    val quickState by quickAddModel.state.collectAsStateWithLifecycle()
    val now = quickState.now.takeIf { it > 0L } ?: clock.now()
    val zone = if (quickState.now > 0L) quickState.zone else clock.zone()
    var courseRetry by remember { mutableIntStateOf(0) }
    val courses by remember(courseRetry) {
        app.container.courses.observeAll()
            .map { CourseLoad(it.filter { course -> course.archivedAt == null }, false) }
            .onStart { emit(CourseLoad()) }
            .catch { error ->
                if (error is CancellationException) throw error
                Log.e("AcademicsSpace", "Course read failed", error)
                emit(CourseLoad(failed = true))
            }
    }.collectAsStateWithLifecycle(initialValue = CourseLoad())
    var examRetry by remember { mutableIntStateOf(0) }
    val examDate = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val examEvents by remember(examDate, zone, examRetry) {
        val from = examDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val until = examDate.plusDays(30).atStartOfDay(zone).toInstant().toEpochMilli()
        app.container.events.observeBetween(from, until)
            .map { events -> EventLoad(events.filter { it.kind == EventKind.EXAM && it.deletedAt == null }.sortedBy(Event::startAt), false) }
            .catch { error ->
                if (error is CancellationException) throw error
                Log.e("AcademicsSpace", "Exam event read failed", error)
                emit(EventLoad(failed = true))
            }
    }.collectAsStateWithLifecycle(initialValue = EventLoad())
    var chosenId by rememberSaveable { mutableStateOf<Long?>(null) }
    val activeCourse = courses.items.firstOrNull { it.id == chosenId }
        ?: if (chosenId == null) courses.items.firstOrNull { it.hasCurrentSlot(now, zone) } ?: courses.items.firstOrNull() else null
    LaunchedEffect(courses.items, courses.loading, courses.failed, chosenId) {
        if (!courses.loading && !courses.failed && chosenId != null && courses.items.none { it.id == chosenId }) chosenId = null
    }
    var assignmentRetry by remember(activeCourse?.id) { mutableIntStateOf(0) }
    val assignments by remember(activeCourse?.id, assignmentRetry) {
        val courseId = activeCourse?.id
        if (courseId == null) flowOf(AssignmentLoad())
        else app.container.assignments.observeForCourse(courseId).map { AssignmentLoad(it, false) }
            .onStart { emit(AssignmentLoad()) }
            .catch { error ->
                if (error is CancellationException) throw error
                Log.e("AcademicsSpace", "Assignment read failed", error)
                emit(AssignmentLoad(failed = true))
            }
    }.collectAsStateWithLifecycle(initialValue = AssignmentLoad())
    val fontScale = LocalConfiguration.current.fontScale
    val compact = fontScale < 1.3f
    val taskSnackbar = remember { SnackbarHostState() }
    val failedMessage = stringResource(R.string.save_failed)
    val deletedMessage = stringResource(R.string.deleted_task)
    val undoMessage = stringResource(R.string.undo)
    val retryMessage = stringResource(R.string.retry)
    val savedMessage = stringResource(R.string.capture_saved)
    var saveRequested by remember { mutableStateOf(false) }
    LaunchedEffect(quickAddModel) {
        quickAddModel.effects.collect { effect ->
            when (effect) {
                is TaskEffect.Failed -> if (taskSnackbar.showSnackbar(failedMessage, retryMessage, withDismissAction = true,
                    duration = SnackbarDuration.Indefinite) == SnackbarResult.ActionPerformed) effect.retry()
                is TaskEffect.Deleted -> if (taskSnackbar.showSnackbar(deletedMessage, undoMessage, withDismissAction = true,
                    duration = SnackbarDuration.Indefinite) == SnackbarResult.ActionPerformed) quickAddModel.restore(effect.id)
            }
        }
    }
    LaunchedEffect(quickState.editor) {
        if (quickState.editor == null && saveRequested) {
            saveRequested = false
            taskSnackbar.showSnackbar(savedMessage)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .testTag("screen-AcademicsSpace")
            .background(ChefCanvas)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header Bar: M-CHEF Header
        Row(
            Modifier.fillMaxWidth(),
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
                        .clickable { onNavigate(id.riko.hub.navigation.Destination.Spaces) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("←", color = ChefInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text(
                        text = "01 / AKADEMIK",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = ChefMuted,
                    )
                    Text(
                        text = "KAMPUS BERGERAK",
                        fontFamily = NotoCondensedBlack,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        color = ChefInk,
                        modifier = Modifier.semantics { heading() },
                    )
                }
            }

            TextButton(
                onClick = { onNavigate(id.riko.hub.navigation.Destination.Courses) },
                modifier = Modifier.testTag("academics-manage-courses"),
            ) {
                Text(stringResource(R.string.academics_manage), color = ChefLime, fontWeight = FontWeight.Bold)
            }
        }

        when {
            courses.loading -> LinearProgressIndicator(Modifier.fillMaxWidth(), color = ChefLime)
            courses.failed -> MessageCard(stringResource(R.string.academics_courses_failed)) {
                TextButton(onClick = { courseRetry++ }, modifier = Modifier.heightIn(min = Spacing.touch)) {
                    Text(stringResource(R.string.retry), color = ChefLime)
                }
            }
            courses.items.isEmpty() -> {
                EmptyCourses { onNavigate(id.riko.hub.navigation.Destination.Courses) }
                EmptyFocusCourseCard()
                DeadlineSection(AssignmentLoad(loading = false), now, zone, onRetry = { assignmentRetry++ },
                    onTaskClick = { quickAddModel.edit(it) }, noCourse = true)
            }
            else -> {
                if (activeCourse != null) {
                    FocusCourseCard(activeCourse, assignments, now, zone, compact, quickAddModel::edit)
                    CourseSelectionStrip(courses.items, activeCourse, onSelect = { chosenId = it.id })
                    DeadlineSection(assignments, now, zone, onRetry = { assignmentRetry++ },
                        onTaskClick = { quickAddModel.edit(it) })
                    CourseAssignmentsSection(activeCourse.id)
                }
            }
        }

        ExamSection(examEvents, zone) { examRetry++ }
        MaterialsPreview(vaultState, onOpenMaterial, onVault = { onNavigate(id.riko.hub.navigation.Destination.Vault) })
        if (vaultState.error != null) {
            MessageCard(stringResource(R.string.academics_vault_attention)) {
                Button(
                    onClick = { onNavigate(id.riko.hub.navigation.Destination.Vault) },
                    colors = ButtonDefaults.buttonColors(containerColor = ChefCoral, contentColor = ChefGround),
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Text(stringResource(R.string.academics_open_vault), fontWeight = FontWeight.Bold)
                }
            }
        }
        QuickActions(
            compact = compact,
            onManageCourses = { onNavigate(id.riko.hub.navigation.Destination.Courses) },
            onAddTask = { quickAddModel.edit(Task(title = "", category = Category.ACADEMIC)) },
            onPlan = { onNavigate(id.riko.hub.navigation.Destination.Timeline) },
            onVault = { onNavigate(id.riko.hub.navigation.Destination.Vault) },
        )
        SnackbarHost(taskSnackbar)
    }

    quickState.editor?.let { task ->
        TaskEditorSheet(task, now, zone, quickState.busy, taskSnackbar,
            quickAddModel::change, { saveRequested = true; quickAddModel.save() },
            { saveRequested = false; quickAddModel.delete(task) }, { saveRequested = false; quickAddModel.dismissEditor() }, quickAdd = true)
    }
}

private data class CourseLoad(val items: List<Course> = emptyList(), val loading: Boolean = true, val failed: Boolean = false)
private data class EventLoad(val items: List<Event> = emptyList(), val loading: Boolean = true, val failed: Boolean = false)
private data class AssignmentLoad(val items: List<id.riko.hub.domain.model.AssignmentTask> = emptyList(), val loading: Boolean = true, val failed: Boolean = false)

internal fun Course.hasCurrentSlot(now: Long, zone: ZoneId): Boolean {
    val local = Instant.ofEpochMilli(now).atZone(zone)
    val day = local.dayOfWeek.value
    val minute = local.hour * 60 + local.minute
    return weeklySlots.any { it.dayOfWeek == day && minute in it.startMinute until it.endMinute }
}

@Composable
private fun CourseSelectionStrip(courses: List<Course>, selected: Course, onSelect: (Course) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.academics_manage).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
            color = ChefMuted,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(courses, key = Course::id) { course ->
                val chosen = course.id == selected.id
                Surface(
                    onClick = { onSelect(course) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (chosen) ChefButter else ChefPanel,
                    border = BorderStroke(1.dp, if (chosen) ChefButter else ChefLine),
                ) {
                    Text(
                        text = course.title,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        color = if (chosen) ChefGround else ChefInk,
                        fontWeight = if (chosen) FontWeight.Bold else FontWeight.Medium,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyCourses(onManage: () -> Unit) {
    MessageCard(stringResource(R.string.academics_no_courses)) {
        Button(
            onClick = onManage,
            colors = ButtonDefaults.buttonColors(containerColor = ChefLime, contentColor = ChefGround),
            shape = RoundedCornerShape(999.dp),
        ) {
            Text(stringResource(R.string.academics_add_course), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EmptyFocusCourseCard() {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = ChefPanel,
        border = BorderStroke(1.dp, ChefButter.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(stringResource(R.string.academics_focus), style = MaterialTheme.typography.labelLarge, color = ChefButter, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.academics_choose_course), style = MaterialTheme.typography.titleMedium, color = ChefInk)
        }
    }
}

@Composable
private fun MessageCard(message: String, action: (@Composable () -> Unit)? = null) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ChefPanel,
        border = BorderStroke(1.dp, ChefLine),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(message, style = MaterialTheme.typography.bodyMedium, color = ChefMuted)
            action?.invoke()
        }
    }
}

@Composable
private fun FocusCourseCard(course: Course, assignments: AssignmentLoad, now: Long, zone: ZoneId, compact: Boolean, onEditNext: (Task) -> Unit) {
    val local = Instant.ofEpochMilli(now).atZone(zone)
    val currentSlot = course.weeklySlots.firstOrNull { it.dayOfWeek == local.dayOfWeek.value &&
        local.hour * 60 + local.minute in it.startMinute until it.endMinute }
    val nextSlot = currentSlot ?: course.weeklySlots.sortedWith(compareBy({ it.dayOfWeek }, { it.startMinute })).firstOrNull()

    // Hero Focus Card in Butter Yellow
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = ChefButter,
        border = BorderStroke(2.dp, ChefGround),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ChefGround.copy(alpha = 0.12f),
                ) {
                    Text(
                        text = stringResource(R.string.academics_focus).uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                        color = ChefGround,
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ChefGround),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("↗", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Text(
                text = course.title,
                fontFamily = NotoCondensedBlack,
                fontWeight = FontWeight.Black,
                fontSize = 26.sp,
                lineHeight = 28.sp,
                color = ChefGround,
            )

            val schedule = nextSlot?.let { slot ->
                val day = DayOfWeek.of(slot.dayOfWeek).getDisplayName(TextStyle.SHORT, java.util.Locale.getDefault())
                val nowLabel = if (slot == currentSlot) stringResource(R.string.academics_now) else null
                buildString {
                    nowLabel?.let { append(it).append(" · ") }
                    append(day).append(" · ").append(formatMinute(slot.startMinute)).append("–").append(formatMinute(slot.endMinute))
                    slot.location?.let { append(" · ").append(it) }
                }
            }
            Text(
                text = schedule ?: stringResource(R.string.academics_no_schedule),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = ChefGround.copy(alpha = 0.85f),
            )

            when {
                assignments.loading -> LinearProgressIndicator(Modifier.fillMaxWidth(), color = ChefGround, trackColor = ChefGround.copy(alpha = 0.2f))
                assignments.failed -> Text(stringResource(R.string.academics_assignments_failed), color = ChefCoral)
                assignments.items.isNotEmpty() -> {
                    val completed = assignments.items.count { it.assignment.state == id.riko.hub.domain.model.AcademicState.COMPLETED || it.task.status == TaskStatus.DONE }
                    val progress = completed.toFloat() / assignments.items.size
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.academics_assignments_progress, completed, assignments.items.size),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = ChefGround,
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                            color = ChefGround,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = ChefGround,
                        trackColor = ChefGround.copy(alpha = 0.2f),
                    )
                }
            }

            val nextTask = assignments.items.filter {
                it.task.status != TaskStatus.DONE && it.assignment.state !in setOf(
                    id.riko.hub.domain.model.AcademicState.SUBMITTED,
                    id.riko.hub.domain.model.AcademicState.COMPLETED,
                )
            }.sortedWith(compareBy({ it.task.deadlineAt ?: Long.MAX_VALUE }, { it.task.title })).firstOrNull()

            if (nextTask != null) {
                Surface(
                    onClick = { onEditNext(nextTask.task) },
                    shape = RoundedCornerShape(16.dp),
                    color = ChefGround,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.academics_next_action).uppercase(),
                                color = ChefMuted,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            )
                            Text(
                                text = nextTask.task.title,
                                color = ChefInk,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = if (compact) 2 else 3,
                            )
                        }
                        Text("↗", color = ChefLime, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DeadlineSection(
    state: AssignmentLoad,
    now: Long,
    zone: ZoneId,
    onRetry: () -> Unit,
    onTaskClick: (Task) -> Unit,
    noCourse: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            stringResource(R.string.academics_deadlines).uppercase(),
            fontFamily = NotoCondensedBlack,
            fontWeight = FontWeight.Black,
            fontSize = 15.sp,
            color = ChefInk,
        )
        when {
            state.loading -> LinearProgressIndicator(Modifier.fillMaxWidth(), color = ChefLime)
            state.failed -> Column {
                Text(stringResource(R.string.academics_deadlines_failed), color = ChefCoral)
                TextButton(onClick = onRetry) { Text(stringResource(R.string.retry), color = ChefLime) }
            }
            else -> {
                val deadlines = state.items.filter {
                    it.task.deadlineAt != null && it.task.status != TaskStatus.DONE &&
                        it.assignment.state !in setOf(
                            id.riko.hub.domain.model.AcademicState.SUBMITTED,
                            id.riko.hub.domain.model.AcademicState.COMPLETED,
                        )
                }.sortedBy { it.task.deadlineAt }.take(3)

                if (deadlines.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ChefPanel,
                        border = BorderStroke(1.dp, ChefLine),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            stringResource(if (noCourse) R.string.academics_deadlines_no_course else R.string.academics_no_deadlines),
                            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            color = ChefMuted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(deadlines, key = { it.task.id }) { item ->
                            val targetDate = Instant.ofEpochMilli(item.task.deadlineAt!!).atZone(zone).toLocalDate()
                            val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
                            val days = java.time.temporal.ChronoUnit.DAYS.between(today, targetDate)
                            val tierColor = when {
                                days <= 1L -> ChefCoral
                                days <= 5L -> ChefButter
                                else -> ChefLime
                            }
                            val badge = when {
                                days < 0L -> stringResource(R.string.academics_overdue)
                                days == 0L -> stringResource(R.string.academics_today)
                                days == 1L -> stringResource(R.string.academics_tomorrow)
                                else -> stringResource(R.string.academics_days_left, days)
                            }
                            Surface(
                                onClick = { onTaskClick(item.task) },
                                modifier = Modifier.widthIn(min = 180.dp),
                                shape = RoundedCornerShape(20.dp),
                                color = ChefPanel,
                                border = BorderStroke(1.dp, ChefLine),
                            ) {
                                Row(
                                    Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Column(
                                        Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(tierColor.copy(alpha = 0.18f)),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                    ) {
                                        Text(
                                            DateTimeFormatter.ofPattern("d", java.util.Locale.getDefault()).withZone(zone)
                                                .format(Instant.ofEpochMilli(item.task.deadlineAt)),
                                            color = tierColor,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                        )
                                        Text(
                                            DateTimeFormatter.ofPattern("MMM", java.util.Locale.getDefault()).withZone(zone)
                                                .format(Instant.ofEpochMilli(item.task.deadlineAt)),
                                            color = ChefMuted,
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            item.task.title,
                                            color = ChefInk,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            maxLines = 2,
                                        )
                                        Text(badge, color = tierColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExamSection(state: EventLoad, zone: ZoneId, onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            stringResource(R.string.academics_exams).uppercase(),
            fontFamily = NotoCondensedBlack,
            fontWeight = FontWeight.Black,
            fontSize = 15.sp,
            color = ChefInk,
        )
        when {
            state.loading -> LinearProgressIndicator(Modifier.fillMaxWidth(), color = ChefLime)
            state.failed -> Column {
                Text(stringResource(R.string.academics_exams_failed), color = ChefCoral)
                TextButton(onClick = onRetry) { Text(stringResource(R.string.retry), color = ChefLime) }
            }
            state.items.isEmpty() -> Text(stringResource(R.string.academics_no_exams), color = ChefMuted, style = MaterialTheme.typography.bodySmall)
            else -> state.items.take(3).forEach { event ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = ChefPanel,
                    border = BorderStroke(1.dp, ChefLine),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(event.title, color = ChefInk, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            event.location?.let { Text(it, color = ChefMuted, style = MaterialTheme.typography.bodySmall) }
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ChefCoral,
                        ) {
                            Text(
                                formatDate(event.startAt, zone),
                                Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                color = ChefGround,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MaterialsPreview(state: VaultUiState, onOpen: ((DriveFile) -> Unit)?, onVault: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = ChefPanel,
        border = BorderStroke(1.dp, ChefLine),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.academics_vault_preview),
                    Modifier.weight(1f),
                    color = ChefInk,
                    fontFamily = NotoCondensedBlack,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                )
                TextButton(onClick = onVault) {
                    Text(stringResource(R.string.academics_open_vault), color = ChefLime, fontWeight = FontWeight.Bold)
                }
            }
            Text(stringResource(R.string.academics_vault_preview_note), color = ChefMuted, style = MaterialTheme.typography.bodySmall)
            val files = state.files.filterNot { it.isFolder }.take(3)
            when {
                state.connectedEmail == null -> Text(stringResource(R.string.academics_vault_not_connected), color = ChefMuted, style = MaterialTheme.typography.bodySmall)
                state.loading -> LinearProgressIndicator(Modifier.fillMaxWidth(), color = ChefLime)
                files.isEmpty() -> Text(stringResource(R.string.academics_vault_empty), color = ChefMuted, style = MaterialTheme.typography.bodySmall)
                else -> files.forEach { file ->
                    Surface(
                        onClick = { onOpen?.invoke(file) ?: onVault() },
                        shape = RoundedCornerShape(14.dp),
                        color = ChefRaised,
                        border = BorderStroke(1.dp, ChefLine),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(ChefSage),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("📄", fontSize = 12.sp)
                            }
                            Text(file.name, Modifier.weight(1f), maxLines = 1, color = ChefInk, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
                            Text("↗", color = ChefMuted, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActions(compact: Boolean, onManageCourses: () -> Unit, onAddTask: () -> Unit, onPlan: () -> Unit, onVault: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            stringResource(R.string.academics_quick_actions).uppercase(),
            fontFamily = NotoCondensedBlack,
            fontWeight = FontWeight.Black,
            fontSize = 15.sp,
            color = ChefInk,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton(R.string.academics_manage, Modifier.weight(1f).testTag("academics-manage-courses"), onManageCourses)
            ActionButton(R.string.academics_add_task, Modifier.weight(1f), onAddTask)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton(R.string.academics_plan, Modifier.weight(1f), onPlan)
            ActionButton(R.string.academics_open_vault, Modifier.weight(1f), onVault)
        }
    }
}

@Composable
private fun ActionButton(label: Int, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = ChefPanel,
        border = BorderStroke(1.dp, ChefLine),
        modifier = modifier.heightIn(min = 44.dp),
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(label), maxLines = 1, color = ChefInk, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun formatDate(value: Long, zone: ZoneId): String =
    DateTimeFormatter.ofPattern("MMM d", java.util.Locale.getDefault()).withZone(zone).format(Instant.ofEpochMilli(value))

private fun formatMinute(value: Int): String = "%02d:%02d".format(value / 60, value % 60)
