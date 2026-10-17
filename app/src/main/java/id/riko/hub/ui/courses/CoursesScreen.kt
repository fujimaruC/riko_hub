package id.riko.hub.ui.courses

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
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
    Column(Modifier.fillMaxSize().background(ChefCanvas).testTag("screen-Courses")) {
        Box(Modifier.weight(1f)) {
            if (course != null) CourseDetail(course, state.busy, model::edit, { model.archive(course, course.archivedAt == null) }, model::select)
            else CourseList(state, model::retryRead, model::showArchived, model::select, model::edit, model::move, model::archive, onNavigate)
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
                      archive: (Course, Boolean) -> Unit, onNavigate: (Destination) -> Unit) {
    val courses = state.courses.filter { (it.archivedAt != null) == state.showArchived }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f).testTag("course-list"), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(ChefPanel)
                                    .border(1.dp, ChefLine, CircleShape)
                                    .clickable { onNavigate(Destination.AcademicsSpace) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("←", color = ChefInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                stringResource(R.string.courses_eyebrow),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = ChefMuted
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(ChefLime)
                                .clickable(enabled = !state.busy) { edit(null) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("＋", color = ChefGround, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        stringResource(R.string.courses).uppercase(),
                        fontFamily = NotoCondensedBlack,
                        fontWeight = FontWeight.Black,
                        fontSize = 26.sp,
                        color = ChefInk,
                        modifier = Modifier.semantics { heading() }
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !state.showArchived,
                        onClick = { showArchived(false) },
                        label = {
                            Text(
                                stringResource(R.string.course_show_active) + " ${state.courses.count { it.archivedAt == null }}",
                                fontWeight = FontWeight.Bold,
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ChefLime,
                            selectedLabelColor = ChefGround,
                            containerColor = ChefPanel,
                            labelColor = ChefInk
                        ),
                        modifier = Modifier.heightIn(min = 40.dp)
                    )
                    FilterChip(
                        selected = state.showArchived,
                        onClick = { showArchived(true) },
                        label = {
                            Text(
                                stringResource(R.string.course_show_archived) + " ${state.courses.count { it.archivedAt != null }}",
                                fontWeight = FontWeight.Bold,
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ChefLime,
                            selectedLabelColor = ChefGround,
                            containerColor = ChefPanel,
                            labelColor = ChefInk
                        ),
                        modifier = Modifier.heightIn(min = 40.dp)
                    )
                    Spacer(Modifier.weight(1f))
                    Surface(
                        onClick = { onNavigate(Destination.Timeline) },
                        shape = RoundedCornerShape(16.dp),
                        color = ChefPanel,
                        border = BorderStroke(1.dp, ChefLine),
                        modifier = Modifier.heightIn(min = 40.dp)
                    ) {
                        Box(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.academics_plan) + " ↗", color = ChefInk, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            if (state.readFailed) item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = ChefPanel,
                    border = BorderStroke(1.dp, ChefLine),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.read_failed), color = ChefInk)
                        Button(onClick = retryRead, modifier = Modifier.heightIn(min = 44.dp), shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ChefLime, contentColor = ChefGround)) {
                            Text(stringResource(R.string.retry), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (state.loading) item { CircularProgressIndicator(color = ChefLime) }
            else if (courses.isEmpty()) item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = ChefPanel,
                    border = BorderStroke(1.dp, ChefLine),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            stringResource(if (state.showArchived) R.string.course_archived_empty else R.string.course_empty),
                            style = MaterialTheme.typography.titleMedium,
                            color = ChefInk
                        )
                        if (!state.showArchived) {
                            Button(
                                onClick = { edit(null) },
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ChefLime,
                                    contentColor = ChefGround
                                )
                            ) {
                                Text(stringResource(R.string.course_add), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            else itemsIndexed(courses, key = { _, course -> course.id }) { index, course ->
                val moveEarlierLabel = stringResource(R.string.course_move_earlier_accessibility, course.title)
                val moveLaterLabel = stringResource(R.string.course_move_later_accessibility, course.title)
                val cardColor = when (index % 4) {
                    0 -> ChefButter
                    1 -> ChefCoral
                    2 -> ChefSage
                    else -> ChefCyan
                }
                val rotation = when (index % 4) {
                    0 -> -1.2f
                    1 -> 1.0f
                    2 -> -0.6f
                    else -> 0.6f
                }
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = cardColor,
                    border = BorderStroke(1.5.dp, ChefGround),
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { rotationZ = rotation }
                        .testTag("course-${course.id}")
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ChefGround),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    course.code?.take(3)?.uppercase() ?: course.title.take(2).uppercase(),
                                    fontFamily = NotoCondensedBlack,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = cardColor
                                )
                            }
                            Column(Modifier.weight(1f).clickable { select(course.id) }) {
                                course.academicPeriod?.takeIf(String::isNotBlank)?.let {
                                    Text(it.uppercase(), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = ChefGround.copy(alpha = 0.7f))
                                }
                                Text(
                                    course.title,
                                    fontFamily = NotoCondensedBlack,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = ChefGround
                                )
                                course.instructor?.takeIf(String::isNotBlank)?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall, color = ChefGround.copy(alpha = 0.75f))
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(ChefGround)
                                    .clickable { select(course.id) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("↗", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = { move(course, -1) }, enabled = !state.busy && index > 0,
                                modifier = Modifier.heightIn(min = 40.dp).testTag("course-earlier-${course.id}")
                                    .semantics { contentDescription = moveEarlierLabel },
                                colors = ButtonDefaults.textButtonColors(contentColor = ChefGround)) {
                                Text(stringResource(R.string.course_move_earlier), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            TextButton(onClick = { move(course, 1) }, enabled = !state.busy && index < courses.lastIndex,
                                modifier = Modifier.heightIn(min = 40.dp).testTag("course-later-${course.id}")
                                    .semantics { contentDescription = moveLaterLabel },
                                colors = ButtonDefaults.textButtonColors(contentColor = ChefGround)) {
                                Text(stringResource(R.string.course_move_later), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            TextButton(onClick = { archive(course, !state.showArchived) }, enabled = !state.busy,
                                modifier = Modifier.heightIn(min = 40.dp),
                                colors = ButtonDefaults.textButtonColors(contentColor = ChefGround)) {
                                Text(stringResource(if (state.showArchived) R.string.course_restore else R.string.course_archive), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
        Button(
            onClick = { edit(null) },
            enabled = !state.busy,
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ChefLime, contentColor = ChefGround),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).heightIn(min = 48.dp)
        ) {
            Text("＋ " + stringResource(R.string.course_add).uppercase(), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CourseDetail(course: Course, busy: Boolean, edit: (Course?) -> Unit, archive: () -> Unit, back: (Long?) -> Unit) {
    val accent = courseAccent(course.color, LocalRikoColors.current)
    LazyColumn(Modifier.fillMaxSize().background(ChefCanvas).testTag("course-detail"), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = ChefPanel,
                border = BorderStroke(1.dp, ChefLine),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        course.icon?.takeIf(String::isNotBlank)?.let { Text(it) }
                        Text(
                            course.title,
                            fontFamily = NotoCondensedBlack,
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            color = ChefInk,
                            modifier = Modifier.semantics { heading() }
                        )
                    }
                    course.code?.takeIf(String::isNotBlank)?.let { Text("${stringResource(R.string.course_code_label)}: $it", color = ChefMuted, style = MaterialTheme.typography.bodyMedium) }
                    course.instructor?.takeIf(String::isNotBlank)?.let { Text("${stringResource(R.string.course_instructor_label)}: $it", color = ChefMuted, style = MaterialTheme.typography.bodyMedium) }
                    course.academicPeriod?.takeIf(String::isNotBlank)?.let { Text("${stringResource(R.string.course_period_label)}: $it", color = ChefMuted, style = MaterialTheme.typography.bodyMedium) }
                    course.icon?.takeIf(String::isNotBlank)?.let { Text("${stringResource(R.string.course_icon_label)}: $it", color = ChefMuted, style = MaterialTheme.typography.bodyMedium) }
                    course.color?.let { color ->
                        val name = stringResource(color.label())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            accent?.let { Surface(Modifier.size(16.dp), shape = CircleShape, color = it.container) {} }
                            Text("${stringResource(R.string.course_color)}: $name", color = ChefMuted, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { edit(course) }, enabled = !busy, shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ChefLime, contentColor = ChefGround),
                            modifier = Modifier.heightIn(min = 44.dp)) { Text(stringResource(R.string.course_edit), fontWeight = FontWeight.Bold) }
                        FilledTonalButton(onClick = archive, enabled = !busy, shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = ChefRaised, contentColor = ChefInk),
                            modifier = Modifier.heightIn(min = 44.dp)) {
                            Text(stringResource(if (course.archivedAt == null) R.string.course_archive else R.string.course_restore), fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = { back(null) }, modifier = Modifier.heightIn(min = 44.dp)) {
                            Text(stringResource(R.string.course_back_list), color = ChefMuted, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        item {
            Text(
                stringResource(R.string.course_timetable).uppercase(),
                fontFamily = NotoCondensedBlack,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                color = ChefInk,
                modifier = Modifier.semantics { heading() }
            )
        }
        if (course.weeklySlots.isEmpty()) item {
            Text(stringResource(R.string.course_no_slots), color = ChefMuted, style = MaterialTheme.typography.bodyMedium)
        }
        else itemsIndexed(course.weeklySlots) { _, slot ->
            val day = stringResource(slot.dayOfWeek.dayLabel())
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ChefPanel,
                border = BorderStroke(1.dp, ChefLine),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(buildString {
                    append(day); append("  "); append(formatTime(slot.startMinute)); append("–"); append(formatTime(slot.endMinute))
                    slot.location?.takeIf(String::isNotBlank)?.let { append(" · "); append(it) }
                }, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = ChefInk, modifier = Modifier.padding(14.dp))
            }
        }
        item {
            CourseAssignmentsSection(course.id)
        }
        item { RelatedPlaceholder(R.string.course_related_materials, R.string.course_related_materials_later) }
    }
}

@Composable
private fun RelatedPlaceholder(title: Int, detail: Int) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ChefPanel,
        border = BorderStroke(1.dp, ChefLine),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = ChefInk)
            Text(stringResource(detail), color = ChefMuted, style = MaterialTheme.typography.bodySmall)
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
        val hour = hourText.toIntOrNull() ?: return@let null
        val minute = minuteText.toIntOrNull() ?: return@let null
        when {
            hour == 24 && minute == 0 -> 1440
            hour in 0..23 && minute in 0..59 -> hour * 60 + minute
            else -> null
        }
    }
