package id.riko.hub.ui.spaces

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.riko.hub.R
import id.riko.hub.RikoHubApp
import id.riko.hub.data.remote.drive.DriveFile
import id.riko.hub.domain.model.Course
import id.riko.hub.domain.model.Event
import id.riko.hub.domain.model.EventKind
import id.riko.hub.domain.model.Category
import id.riko.hub.domain.model.Task
import id.riko.hub.domain.model.TaskStatus
import id.riko.hub.ui.academic.CourseAssignmentsSection
import id.riko.hub.ui.components.TaskEditorSheet
import id.riko.hub.ui.tasks.TaskEffect
import id.riko.hub.ui.tasks.TasksViewModel
import id.riko.hub.ui.theme.SpacesTokens
import id.riko.hub.ui.theme.Spacing
import id.riko.hub.ui.theme.rememberReducedMotion
import id.riko.hub.ui.vault.VaultUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.flowOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.DayOfWeek
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items

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
    val reducedMotion = rememberReducedMotion()
    var showAllCourses by remember { mutableStateOf(false) }
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

    SpacesTheme {
        Column(
            Modifier.fillMaxSize().testTag("screen-AcademicsSpace").background(SpacesTokens.canvas)
                .verticalScroll(rememberScrollState()).padding(horizontal = Spacing.space5, vertical = Spacing.space4),
            verticalArrangement = Arrangement.spacedBy(Spacing.space4),
        ) {
            SpacesPageHeader(
                title = stringResource(R.string.academics_space_title),
                subtitle = stringResource(R.string.academics_space_subtitle),
                onBack = { onNavigate(id.riko.hub.navigation.Destination.Spaces) },
                trailing = { TextButton(onClick = { onNavigate(id.riko.hub.navigation.Destination.Courses) },
                    modifier = Modifier.heightIn(min = Spacing.touch).testTag("academics-manage-courses")) {
                    Text(stringResource(R.string.academics_manage))
                } },
            )
            when {
                courses.loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
                courses.failed -> MessageCard(stringResource(R.string.academics_courses_failed)) {
                    TextButton(onClick = { courseRetry++ }, modifier = Modifier.heightIn(min = Spacing.touch)) { Text(stringResource(R.string.retry)) }
                }
                courses.items.isEmpty() -> {
                    OrbitCard(emptyList(), null, compact, reducedMotion, showAllCourses,
                        onSelect = {}, onShowAll = { showAllCourses = !showAllCourses })
                    EmptyCourses { onNavigate(id.riko.hub.navigation.Destination.Courses) }
                    EmptyFocusCourseCard()
                    DeadlineSection(AssignmentLoad(loading = false), now, zone, onRetry = { assignmentRetry++ },
                        onTaskClick = { quickAddModel.edit(it) }, noCourse = true)
                }
                else -> {
                    OrbitCard(courses.items, activeCourse, compact, reducedMotion, showAllCourses,
                        onSelect = { chosenId = it.id }, onShowAll = { showAllCourses = !showAllCourses })
                    if (activeCourse != null) {
                        FocusCourseCard(activeCourse, assignments, now, zone, compact, quickAddModel::edit)
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
                    Button(onClick = { onNavigate(id.riko.hub.navigation.Destination.Vault) }, modifier = Modifier.heightIn(min = Spacing.touch)) {
                        Text(stringResource(R.string.academics_open_vault))
                    }
                }
            }
            QuickActions(compact,
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
private fun EmptyCourses(onManage: () -> Unit) {
    MessageCard(stringResource(R.string.academics_no_courses)) {
        Button(onClick = onManage, modifier = Modifier.heightIn(min = Spacing.touch)) { Text(stringResource(R.string.academics_add_course)) }
    }
}

@Composable
private fun EmptyFocusCourseCard() {
    val shape = RoundedCornerShape(28.dp)
    Column(Modifier.fillMaxWidth().clip(shape)
        .background(Brush.linearGradient(listOf(SpacesTokens.tangerine.copy(alpha = .12f), SpacesTokens.surface)))
        .border(1.5.dp, SpacesTokens.tangerine.copy(alpha = .24f), shape).padding(Spacing.space4),
        verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
        Text(stringResource(R.string.academics_focus), style = MaterialTheme.typography.labelLarge, color = SpacesTokens.tangerine)
        Text(stringResource(R.string.academics_choose_course), style = MaterialTheme.typography.titleMedium, color = SpacesTokens.primary)
    }
}

@Composable
private fun MessageCard(message: String, action: (@Composable () -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(SpacesTokens.surface).padding(Spacing.space4),
        verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
        Text(message, style = MaterialTheme.typography.bodyLarge, color = SpacesTokens.secondary)
        action?.invoke()
    }
}

@Composable
private fun OrbitCard(courses: List<Course>, selected: Course?, compact: Boolean, reducedMotion: Boolean, overflowOpen: Boolean,
                      onSelect: (Course) -> Unit, onShowAll: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(SpacesTokens.surface).padding(Spacing.space4),
        verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
        Text(stringResource(R.string.academics_orbit_title), style = MaterialTheme.typography.titleLarge, color = SpacesTokens.primary)
        if (compact) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val remaining = courses.filterNot { it.id == selected?.id }
                val narrow = maxWidth < 280.dp
                val visibleCount = if (narrow || courses.size > 8) 4 else 8
                val visible = remaining.take(visibleCount)
                val overflow = remaining.drop(visibleCount)
                val largeOrbit = remaining.size > 4
                val satelliteSize = if (largeOrbit) 44.dp else 56.dp
                val orbitWidth = maxWidth
                Column(Modifier.fillMaxWidth()) {
                    Box(Modifier.fillMaxWidth().height(200.dp)) {
                        Canvas(Modifier.fillMaxSize()) {
                            val stroke = 1.5.dp.toPx()
                            val firstOrbit = Path().apply {
                                addOval(Rect(Offset(size.width * .19f, size.height * .16f), androidx.compose.ui.geometry.Size(size.width * .62f, size.height * .66f)))
                            }
                            drawPath(firstOrbit, SpacesTokens.edge, style = Stroke(stroke,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx()))))
                            if (largeOrbit) {
                                val outerOrbit = Path().apply {
                                    addOval(Rect(Offset(size.width * .06f, size.height * .06f), androidx.compose.ui.geometry.Size(size.width * .88f, size.height * .88f)))
                                }
                                drawPath(outerOrbit, SpacesTokens.edge.copy(alpha = .7f), style = Stroke(stroke,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 6.dp.toPx()))))
                            }
                        }
                        Surface(
                            modifier = Modifier.align(Alignment.Center).size(88.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = SpacesTokens.tangerine.copy(alpha = .16f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SpacesTokens.tangerine.copy(alpha = .55f)),
                        ) {
                            Crossfade(targetState = selected, animationSpec = tween(if (reducedMotion) 0 else 180), label = "course-orbit-focus") { course ->
                                Column(Modifier.fillMaxSize().padding(Spacing.space2), horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center) {
                                    Canvas(Modifier.size(16.dp)) {
                                        val ink = SpacesTokens.tangerine
                                        val stroke = 1.4.dp.toPx()
                                        drawRoundRect(ink, Offset(size.width * .12f, size.height * .12f),
                                            androidx.compose.ui.geometry.Size(size.width * .76f, size.height * .76f),
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()), style = Stroke(stroke))
                                        drawLine(ink, Offset(size.width * .5f, size.height * .16f), Offset(size.width * .5f, size.height * .84f), stroke)
                                    }
                                    Text(course?.code ?: stringResource(R.string.academics_focus), color = SpacesTokens.tangerine,
                                        style = MaterialTheme.typography.labelMedium, maxLines = 1)
                                    Text(course?.title ?: stringResource(R.string.academics_choose_course), color = SpacesTokens.primary,
                                        style = MaterialTheme.typography.labelSmall, maxLines = 2)
                                }
                            }
                        }
                        val inner = listOf(-.23f to -.25f, .23f to -.25f, -.23f to .25f, .23f to .25f)
                        val outer = listOf(-.40f to -.32f, .40f to -.32f, -.40f to .32f, .40f to .32f)
                        visible.forEachIndexed { index, course ->
                            val (dx, dy) = if (largeOrbit && index >= 4) outer[index - 4] else inner[index % 4]
                            CourseSatellite(course, index, false, satelliteSize,
                            Modifier.offset(x = (orbitWidth.value / 2 + dx * orbitWidth.value - satelliteSize.value / 2).dp,
                                    y = (100 + dy * 200 - satelliteSize.value / 2).dp), onClick = { onSelect(course) })
                        }
                    }
                    if (overflow.isNotEmpty()) {
                        val overflowLabel = stringResource(R.string.academics_more_courses, overflow.size)
                        Box(Modifier.fillMaxWidth().padding(top = Spacing.space1), contentAlignment = Alignment.CenterEnd) {
                            Box {
                                Surface(onClick = onShowAll, modifier = Modifier.sizeIn(minWidth = Spacing.touch, minHeight = Spacing.touch)
                                .semantics { contentDescription = overflowLabel },
                                shape = CircleShape, color = SpacesTokens.amber, contentColor = SpacesTokens.canvas) {
                                    Box(contentAlignment = Alignment.Center) { Text("+${overflow.size}", style = MaterialTheme.typography.labelMedium) }
                                }
                                DropdownMenu(expanded = overflowOpen, onDismissRequest = onShowAll) {
                                    overflow.forEach { course ->
                                        DropdownMenuItem(text = { Text(course.title) }, onClick = { onSelect(course); onShowAll() })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (courses.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    Surface(modifier = Modifier.size(88.dp), shape = RoundedCornerShape(24.dp),
                        color = SpacesTokens.tangerine.copy(alpha = .16f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SpacesTokens.tangerine.copy(alpha = .55f))) {
                        Column(Modifier.fillMaxSize().padding(Spacing.space2), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center) {
                            Canvas(Modifier.size(16.dp)) {
                                val ink = SpacesTokens.tangerine
                                val stroke = 1.4.dp.toPx()
                                drawRoundRect(ink, Offset(size.width * .12f, size.height * .12f),
                                    androidx.compose.ui.geometry.Size(size.width * .76f, size.height * .76f),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()), style = Stroke(stroke))
                                drawLine(ink, Offset(size.width * .5f, size.height * .16f), Offset(size.width * .5f, size.height * .84f), stroke)
                            }
                            Text(stringResource(R.string.academics_choose_course), color = SpacesTokens.primary,
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            } else Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(Spacing.space1)) {
                courses.forEach { course ->
                    Row(Modifier.fillMaxWidth().heightIn(min = Spacing.touch).clip(RoundedCornerShape(16.dp))
                        .background(SpacesTokens.row).selectable(selected = course.id == selected?.id, role = Role.RadioButton,
                            onClick = { onSelect(course) }).semantics { contentDescription = course.title }.padding(horizontal = Spacing.space3),
                        verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = course.id == selected?.id, onClick = null)
                        Text(course.code ?: course.title, Modifier.weight(1f), color = SpacesTokens.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseSatellite(course: Course, index: Int, selected: Boolean, diameter: androidx.compose.ui.unit.Dp,
                            position: Modifier, onClick: () -> Unit) {
    Box(position.sizeIn(minWidth = Spacing.touch, minHeight = Spacing.touch)
        .clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = course.title },
        contentAlignment = Alignment.Center) {
      Surface(
        modifier = Modifier.size(diameter),
        shape = CircleShape,
        color = if (selected) SpacesTokens.tangerine else SpacesTokens.row,
        contentColor = if (selected) SpacesTokens.canvas else SpacesTokens.primary,
        border = androidx.compose.foundation.BorderStroke(1.dp, SpacesTokens.tangerine.copy(alpha = if (selected) .85f else .25f)),
      ) {
        Column(Modifier.fillMaxSize().padding(3.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Canvas(Modifier.size(15.dp)) {
                val c = if (selected) SpacesTokens.canvas else SpacesTokens.tangerine
                val sw = 1.5.dp.toPx()
                when (index % 4) {
                    0 -> {
                        drawRect(c, Offset(size.width * .2f, size.height * .22f), androidx.compose.ui.geometry.Size(size.width * .6f, size.height * .62f), style = Stroke(sw))
                        drawLine(c, Offset(size.width * .5f, size.height * .22f), Offset(size.width * .5f, size.height * .84f), sw)
                    }
                    1 -> {
                        drawLine(c, Offset(size.width * .36f, size.height * .2f), Offset(size.width * .17f, size.height * .5f), sw)
                        drawLine(c, Offset(size.width * .17f, size.height * .5f), Offset(size.width * .36f, size.height * .8f), sw)
                        drawLine(c, Offset(size.width * .64f, size.height * .2f), Offset(size.width * .83f, size.height * .5f), sw)
                        drawLine(c, Offset(size.width * .83f, size.height * .5f), Offset(size.width * .64f, size.height * .8f), sw)
                    }
                    2 -> {
                        drawCircle(c, size.minDimension * .13f, Offset(size.width * .3f, size.height * .33f))
                        drawCircle(c, size.minDimension * .13f, Offset(size.width * .7f, size.height * .33f))
                        drawLine(c, Offset(size.width * .22f, size.height * .7f), Offset(size.width * .78f, size.height * .7f), sw)
                    }
                    else -> {
                        drawLine(c, Offset(size.width * .22f, size.height * .8f), Offset(size.width * .22f, size.height * .2f), sw)
                        drawLine(c, Offset(size.width * .22f, size.height * .8f), Offset(size.width * .82f, size.height * .8f), sw)
                        drawLine(c, Offset(size.width * .38f, size.height * .7f), Offset(size.width * .38f, size.height * .53f), sw)
                        drawLine(c, Offset(size.width * .57f, size.height * .7f), Offset(size.width * .57f, size.height * .39f), sw)
                        drawLine(c, Offset(size.width * .76f, size.height * .7f), Offset(size.width * .76f, size.height * .24f), sw)
                    }
                }
            }
            Text(course.code?.take(4) ?: course.title.take(4), style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
      }
    }
}

@Composable
private fun FocusCourseCard(course: Course, assignments: AssignmentLoad, now: Long, zone: ZoneId, compact: Boolean, onEditNext: (Task) -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    val brush = Brush.linearGradient(listOf(SpacesTokens.tangerine.copy(alpha = .16f), SpacesTokens.surface))
    val local = Instant.ofEpochMilli(now).atZone(zone)
    val currentSlot = course.weeklySlots.firstOrNull { it.dayOfWeek == local.dayOfWeek.value &&
        local.hour * 60 + local.minute in it.startMinute until it.endMinute }
    val nextSlot = currentSlot ?: course.weeklySlots.sortedWith(compareBy({ it.dayOfWeek }, { it.startMinute })).firstOrNull()
    Column(Modifier.fillMaxWidth().clip(shape).background(brush).border(1.5.dp, SpacesTokens.tangerine.copy(alpha = .3f), shape)
        .padding(Spacing.space4), verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
        Text(stringResource(R.string.academics_focus), style = MaterialTheme.typography.labelLarge, color = SpacesTokens.tangerine)
        Text(course.title, style = MaterialTheme.typography.headlineSmall, color = SpacesTokens.primary)
        val schedule = nextSlot?.let { slot ->
            val day = DayOfWeek.of(slot.dayOfWeek).getDisplayName(TextStyle.SHORT, java.util.Locale.getDefault())
            val nowLabel = if (slot == currentSlot) stringResource(R.string.academics_now) else null
            buildString {
                nowLabel?.let { append(it).append(" · ") }
                append(day).append(" · ").append(formatMinute(slot.startMinute)).append("–").append(formatMinute(slot.endMinute))
                slot.location?.let { append(" · ").append(it) }
            }
        }
            Text(schedule ?: stringResource(R.string.academics_no_schedule), style = MaterialTheme.typography.bodyMedium, color = SpacesTokens.secondary)
        when {
            assignments.loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
            assignments.failed -> Text(stringResource(R.string.academics_assignments_failed), color = SpacesTokens.secondary)
            assignments.items.isNotEmpty() -> {
                val completed = assignments.items.count { it.assignment.state == id.riko.hub.domain.model.AcademicState.COMPLETED || it.task.status == TaskStatus.DONE }
                val progress = completed.toFloat() / assignments.items.size
                Text(stringResource(R.string.academics_assignments_progress, completed, assignments.items.size),
                    style = MaterialTheme.typography.labelMedium, color = SpacesTokens.secondary)
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = SpacesTokens.tangerine,
                    trackColor = SpacesTokens.row)
            }
        }
        val nextTask = assignments.items.filter {
            it.task.status != TaskStatus.DONE && it.assignment.state !in setOf(
                id.riko.hub.domain.model.AcademicState.SUBMITTED,
                id.riko.hub.domain.model.AcademicState.COMPLETED,
            )
        }
            .sortedWith(compareBy({ it.task.deadlineAt ?: Long.MAX_VALUE }, { it.task.title })).firstOrNull()
        if (nextTask != null) {
            TextButton(onClick = { onEditNext(nextTask.task) }, modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touch),
                colors = ButtonDefaults.textButtonColors(containerColor = SpacesTokens.row.copy(alpha = .8f))) {
                Column(Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.academics_next_action), color = SpacesTokens.secondary, style = MaterialTheme.typography.labelSmall)
                    Text(nextTask.task.title, color = SpacesTokens.primary, maxLines = if (compact) 2 else 3)
                }
            }
        }
    }
}

@Composable
private fun DeadlineSection(state: AssignmentLoad, now: Long, zone: ZoneId, onRetry: () -> Unit,
                            onTaskClick: (Task) -> Unit, noCourse: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
        Text(stringResource(R.string.academics_deadlines), style = MaterialTheme.typography.titleMedium, color = SpacesTokens.primary)
        when {
            state.loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
            state.failed -> Column {
                Text(stringResource(R.string.academics_deadlines_failed), color = SpacesTokens.secondary)
                TextButton(onClick = onRetry, modifier = Modifier.heightIn(min = Spacing.touch)) { Text(stringResource(R.string.retry)) }
            }
            else -> {
                val deadlines = state.items.filter {
                    it.task.deadlineAt != null && it.task.status != TaskStatus.DONE &&
                        it.assignment.state !in setOf(id.riko.hub.domain.model.AcademicState.SUBMITTED,
                            id.riko.hub.domain.model.AcademicState.COMPLETED)
                }
                    .sortedBy { it.task.deadlineAt }.take(3)
                if (deadlines.isEmpty()) {
                    Surface(shape = CircleShape, color = SpacesTokens.row, modifier = Modifier.heightIn(min = Spacing.touch)) {
                        Text(stringResource(if (noCourse) R.string.academics_deadlines_no_course else R.string.academics_no_deadlines),
                            Modifier.padding(horizontal = Spacing.space3, vertical = Spacing.space2),
                            color = SpacesTokens.secondary)
                    }
                } else LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                    items(deadlines, key = { it.task.id }) { item ->
                        val targetDate = Instant.ofEpochMilli(item.task.deadlineAt!!).atZone(zone).toLocalDate()
                        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
                        val days = java.time.temporal.ChronoUnit.DAYS.between(today, targetDate)
                        val tierColor = when {
                            days <= 1L -> SpacesTokens.magenta
                            days <= 5L -> SpacesTokens.tangerine
                            else -> SpacesTokens.jade
                        }
                        val badge = when {
                            days < 0L -> stringResource(R.string.academics_overdue)
                            days == 0L -> stringResource(R.string.academics_today)
                            days == 1L -> stringResource(R.string.academics_tomorrow)
                            else -> stringResource(R.string.academics_days_left, days)
                        }
                        Surface(onClick = { onTaskClick(item.task) }, modifier = Modifier.widthIn(min = 160.dp).heightIn(min = 72.dp),
                            shape = RoundedCornerShape(20.dp), color = tierColor.copy(alpha = .09f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, tierColor.copy(alpha = .4f))) {
                            Row(Modifier.padding(Spacing.space2), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                                Column(Modifier.width(40.dp).heightIn(min = 44.dp).clip(RoundedCornerShape(12.dp))
                                    .background(tierColor.copy(alpha = .16f)), horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center) {
                                    Text(DateTimeFormatter.ofPattern("d", java.util.Locale.getDefault()).withZone(zone)
                                        .format(Instant.ofEpochMilli(item.task.deadlineAt)), color = SpacesTokens.primary, style = MaterialTheme.typography.titleMedium)
                                    Text(DateTimeFormatter.ofPattern("MMM", java.util.Locale.getDefault()).withZone(zone)
                                        .format(Instant.ofEpochMilli(item.task.deadlineAt)), color = SpacesTokens.secondary, style = MaterialTheme.typography.labelSmall)
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(item.task.title, color = SpacesTokens.primary, style = MaterialTheme.typography.labelLarge, maxLines = 2)
                                    Text(badge, color = SpacesTokens.secondary, style = MaterialTheme.typography.labelSmall)
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
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
        Text(stringResource(R.string.academics_exams), style = MaterialTheme.typography.titleMedium, color = SpacesTokens.primary)
        when {
            state.loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
            state.failed -> Column {
                Text(stringResource(R.string.academics_exams_failed), color = SpacesTokens.secondary)
                TextButton(onClick = onRetry, modifier = Modifier.heightIn(min = Spacing.touch)) { Text(stringResource(R.string.retry)) }
            }
            state.items.isEmpty() -> Text(stringResource(R.string.academics_no_exams), color = SpacesTokens.secondary)
            else -> state.items.take(3).forEach { event ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SpacesTokens.surface).padding(Spacing.space3),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(event.title, color = SpacesTokens.primary)
                        event.location?.let { Text(it, color = SpacesTokens.secondary, style = MaterialTheme.typography.bodySmall) }
                    }
                    Text(formatDate(event.startAt, zone), color = SpacesTokens.tangerine, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun MaterialsPreview(state: VaultUiState, onOpen: ((DriveFile) -> Unit)?, onVault: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(SpacesTokens.surface).padding(Spacing.space4),
        verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.academics_vault_preview), Modifier.weight(1f), color = SpacesTokens.primary,
                style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onVault, modifier = Modifier.heightIn(min = Spacing.touch)) { Text(stringResource(R.string.academics_open_vault)) }
        }
        Text(stringResource(R.string.academics_vault_preview_note), color = SpacesTokens.secondary, style = MaterialTheme.typography.bodySmall)
        val files = state.files.filterNot { it.isFolder }.take(3)
        if (state.connectedEmail == null) Text(stringResource(R.string.academics_vault_not_connected), color = SpacesTokens.secondary)
        else if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        else if (files.isEmpty()) Text(stringResource(R.string.academics_vault_empty), color = SpacesTokens.secondary)
        else files.forEach { file ->
            TextButton(onClick = { onOpen?.invoke(file) ?: onVault() }, modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touch)) {
                Text(file.name, Modifier.fillMaxWidth(), maxLines = 2)
            }
        }
    }
}

@Composable
private fun QuickActions(compact: Boolean, onManageCourses: () -> Unit, onAddTask: () -> Unit, onPlan: () -> Unit, onVault: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
        Text(stringResource(R.string.academics_quick_actions), style = MaterialTheme.typography.titleMedium, color = SpacesTokens.primary)
        if (compact) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                ActionButton(R.string.academics_manage, Modifier.weight(1f), onManageCourses)
                ActionButton(R.string.academics_add_task, Modifier.weight(1f), onAddTask)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                ActionButton(R.string.academics_plan, Modifier.weight(1f), onPlan)
                ActionButton(R.string.academics_open_vault, Modifier.weight(1f), onVault)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                ActionButton(R.string.academics_manage, Modifier.fillMaxWidth(), onManageCourses)
                ActionButton(R.string.academics_add_task, Modifier.fillMaxWidth(), onAddTask)
                ActionButton(R.string.academics_plan, Modifier.fillMaxWidth(), onPlan)
                ActionButton(R.string.academics_open_vault, Modifier.fillMaxWidth(), onVault)
            }
        }
    }
}

@Composable
private fun ActionButton(label: Int, modifier: Modifier, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = modifier.heightIn(min = Spacing.touch)) {
        Text(stringResource(label), maxLines = 2)
    }
}

private fun formatDate(value: Long, zone: ZoneId): String =
    DateTimeFormatter.ofPattern("MMM d", java.util.Locale.getDefault()).withZone(zone).format(Instant.ofEpochMilli(value))

private fun formatMinute(value: Int): String = "%02d:%02d".format(value / 60, value % 60)
