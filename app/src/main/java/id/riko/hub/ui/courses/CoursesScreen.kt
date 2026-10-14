package id.riko.hub.ui.courses

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import id.riko.hub.R
import id.riko.hub.RikoHubApp
import id.riko.hub.domain.model.Course
import id.riko.hub.navigation.Destination
import id.riko.hub.ui.academic.CourseAssignmentsSection
import id.riko.hub.ui.theme.*
import java.util.Locale

@Composable
fun CoursesScreen(onNavigate: (Destination) -> Unit) {
    val app = LocalContext.current.applicationContext as RikoHubApp
    val model: CoursesViewModel = viewModel(factory = viewModelFactory {
        initializer { CoursesViewModel(app.container.courses) }
    })
    CoursesRoute(model, onNavigate)
}

@Composable
fun CoursesRoute(model: CoursesViewModel, onNavigate: (Destination) -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val failed = stringResource(R.string.course_save_failed)
    val retry = stringResource(R.string.retry)
    LaunchedEffect(model) {
        model.writeFailures.collect { action ->
            if (snackbar.showSnackbar(failed, action?.let { retry }, withDismissAction = true,
                    duration = SnackbarDuration.Indefinite) == SnackbarResult.ActionPerformed) action?.invoke()
        }
    }
    BackHandler(state.editor != null || state.selectedId != null) { model.back() }
    val course = state.courses.firstOrNull { it.id == state.selectedId }
    Column(Modifier.fillMaxSize().testTag("screen-Courses")) {
        TextButton(onClick = { onNavigate(Destination.AcademicsSpace) }, modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touch)) {
            Text(stringResource(R.string.courses_back_academics))
        }
        Box(Modifier.weight(1f)) {
            if (course != null) CourseDetail(course, state.busy, model::edit, { model.archive(course, course.archivedAt == null) }, model::select)
            else CourseList(state, model::retryRead, model::showArchived, model::select, model::edit, model::move, model::archive)
            if (state.editor == null) SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
    }
    state.editor?.let { edited ->
        CourseEditorSheet(edited, state.busy, snackbar, model::change, model::save, model::dismissEditor)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CourseList(state: CoursesUiState, retryRead: () -> Unit, showArchived: (Boolean) -> Unit,
                      select: (Long) -> Unit, edit: (Course?) -> Unit, move: (Course, Int) -> Unit,
                      archive: (Course, Boolean) -> Unit) {
    val courses = state.courses.filter { (it.archivedAt != null) == state.showArchived }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f).testTag("course-list"), contentPadding = PaddingValues(Spacing.space5),
            verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
            item {
                Text(stringResource(R.string.courses), style = MaterialTheme.typography.displayMedium, modifier = Modifier.semantics { heading() })
                TextButton(onClick = { showArchived(!state.showArchived) }, modifier = Modifier.heightIn(min = Spacing.touch)) {
                    Text(stringResource(if (state.showArchived) R.string.course_show_active else R.string.course_show_archived))
                }
            }
            if (state.readFailed) item {
                Text(stringResource(R.string.read_failed))
                TextButton(onClick = retryRead, modifier = Modifier.heightIn(min = Spacing.touch)) { Text(stringResource(R.string.retry)) }
            } else if (state.loading) item { CircularProgressIndicator() }
            else if (courses.isEmpty()) item { Text(stringResource(if (state.showArchived) R.string.course_archived_empty else R.string.course_empty)) }
            else itemsIndexed(courses, key = { _, course -> course.id }) { index, course ->
                val moveEarlierLabel = stringResource(R.string.course_move_earlier_accessibility, course.title)
                val moveLaterLabel = stringResource(R.string.course_move_later_accessibility, course.title)
                val accent = courseAccent(course.color, LocalRikoColors.current)
                val colorName = course.color?.let { stringResource(it.label()) }
                Surface(shape = RikoShapes.asymA, color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().testTag("course-${course.id}")) {
                    Column(Modifier.padding(Spacing.space3), verticalArrangement = Arrangement.spacedBy(Spacing.space1)) {
                        TextButton(onClick = { select(course.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touch), enabled = !state.busy) {
                            Column(Modifier.fillMaxWidth()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                                    course.icon?.takeIf(String::isNotBlank)?.let { Text(it) }
                                    accent?.let { Surface(Modifier.size(Spacing.space4).align(Alignment.CenterVertically), shape = RikoShapes.pill, color = it.container) {} }
                                    Text(course.title, style = MaterialTheme.typography.titleMedium)
                                }
                                course.code?.takeIf(String::isNotBlank)?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                                colorName?.let { Text("${stringResource(R.string.course_color)}: $it", style = MaterialTheme.typography.bodySmall) }
                            }
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.space1)) {
                            TextButton(onClick = { move(course, -1) }, enabled = !state.busy && index > 0,
                                modifier = Modifier.heightIn(min = Spacing.touch).testTag("course-earlier-${course.id}")
                                    .semantics { contentDescription = moveEarlierLabel }) {
                                Text(stringResource(R.string.course_move_earlier))
                            }
                            TextButton(onClick = { move(course, 1) }, enabled = !state.busy && index < courses.lastIndex,
                                modifier = Modifier.heightIn(min = Spacing.touch).testTag("course-later-${course.id}")
                                    .semantics { contentDescription = moveLaterLabel }) {
                                Text(stringResource(R.string.course_move_later))
                            }
                            TextButton(onClick = { archive(course, !state.showArchived) }, enabled = !state.busy,
                                modifier = Modifier.heightIn(min = Spacing.touch)) {
                                Text(stringResource(if (state.showArchived) R.string.course_restore else R.string.course_archive))
                            }
                        }
                    }
                }
            }
        }
        Button(onClick = { edit(null) }, enabled = !state.busy, shape = RikoShapes.pill,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.space5, vertical = Spacing.space2).heightIn(min = Spacing.touch)) {
            Text(stringResource(R.string.course_add))
        }
    }
}

@Composable
private fun CourseDetail(course: Course, busy: Boolean, edit: (Course?) -> Unit, archive: () -> Unit, back: (Long?) -> Unit) {
    val accent = courseAccent(course.color, LocalRikoColors.current)
    LazyColumn(Modifier.fillMaxSize().testTag("course-detail"), contentPadding = PaddingValues(Spacing.space5),
        verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                course.icon?.takeIf(String::isNotBlank)?.let { Text(it) }
                Text(course.title, style = MaterialTheme.typography.displaySmall, modifier = Modifier.semantics { heading() })
            }
            course.code?.takeIf(String::isNotBlank)?.let { Text("${stringResource(R.string.course_code_label)}: $it") }
            course.instructor?.takeIf(String::isNotBlank)?.let { Text("${stringResource(R.string.course_instructor_label)}: $it") }
            course.academicPeriod?.takeIf(String::isNotBlank)?.let { Text("${stringResource(R.string.course_period_label)}: $it") }
            course.icon?.takeIf(String::isNotBlank)?.let { Text("${stringResource(R.string.course_icon_label)}: $it") }
            course.color?.let { color ->
                val name = stringResource(color.label())
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                    accent?.let { Surface(Modifier.size(Spacing.space4).align(Alignment.CenterVertically), shape = RikoShapes.pill, color = it.container) {} }
                    Text("${stringResource(R.string.course_color)}: $name")
                }
            }
            TextButton(onClick = { edit(course) }, enabled = !busy, modifier = Modifier.heightIn(min = Spacing.touch)) { Text(stringResource(R.string.course_edit)) }
            TextButton(onClick = archive, enabled = !busy, modifier = Modifier.heightIn(min = Spacing.touch)) {
                Text(stringResource(if (course.archivedAt == null) R.string.course_archive else R.string.course_restore))
            }
            TextButton(onClick = { back(null) }, modifier = Modifier.heightIn(min = Spacing.touch)) { Text(stringResource(R.string.course_back_list)) }
        }
        item { Text(stringResource(R.string.course_timetable), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() }) }
        if (course.weeklySlots.isEmpty()) item { Text(stringResource(R.string.course_no_slots)) }
        else itemsIndexed(course.weeklySlots) { _, slot ->
            val day = stringResource(slot.dayOfWeek.dayLabel())
            Text(buildString {
                append(day); append("  "); append(formatTime(slot.startMinute)); append("–"); append(formatTime(slot.endMinute))
                slot.location?.takeIf(String::isNotBlank)?.let { append(" · "); append(it) }
            }, style = MaterialTheme.typography.bodyLarge)
        }
        item { CourseAssignmentsSection(course.id) }
        item { RelatedPlaceholder(R.string.course_related_materials, R.string.course_related_materials_later) }
    }
}

@Composable
private fun RelatedPlaceholder(title: Int, detail: Int) {
    Surface(shape = RikoShapes.asymA, color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.fillMaxWidth().padding(Spacing.space4), verticalArrangement = Arrangement.spacedBy(Spacing.space1)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(detail), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

internal fun Int.dayLabel() = when (this) {
    1 -> R.string.course_monday; 2 -> R.string.course_tuesday; 3 -> R.string.course_wednesday
    4 -> R.string.course_thursday; 5 -> R.string.course_friday; 6 -> R.string.course_saturday
    else -> R.string.course_sunday
}

private fun String.label() = when (this) {
    "BLUE" -> R.string.course_color_blue; "GREEN" -> R.string.course_color_green
    "PURPLE" -> R.string.course_color_purple; else -> R.string.course_color_orange
}

private fun courseAccent(color: String?, colors: RikoColors) = when (color) {
    "BLUE" -> colors.organization; "GREEN" -> colors.personal
    "PURPLE" -> colors.academic; "ORANGE" -> colors.peach; else -> null
}

internal fun formatTime(minute: Int): String = "%02d:%02d".format(Locale.ROOT, minute / 60, minute % 60)
internal fun parseTime(value: String): Int? = value.takeIf { it.matches(Regex("\\d{2}:\\d{2}")) }
    ?.split(':')?.let { (hourText, minuteText) ->
        val hour = hourText.toInt(); val minute = minuteText.toInt()
        if (minute !in 0..59 || hour !in 0..24 || (hour == 24 && minute != 0)) null else hour * 60 + minute
    }
    ?.takeIf { it in 0..1440 }
