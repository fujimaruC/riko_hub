package id.riko.hub.ui.academic

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import id.riko.hub.R
import id.riko.hub.RikoHubApp
import id.riko.hub.domain.model.*
import id.riko.hub.ui.tasks.label
import id.riko.hub.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AssignmentEditorDialog(task: Task, onDismiss: () -> Unit) {
    val app = LocalContext.current.applicationContext as RikoHubApp
    val model: AssignmentViewModel = viewModel(key = "assignment-${task.id}", factory = viewModelFactory {
        initializer { AssignmentViewModel(task, app.container.assignments, app.container.courses, app.container.tasks) }
    })
    AssignmentEditorContent(task, model, onDismiss)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AssignmentEditorContent(task: Task, model: AssignmentViewModel, onDismiss: () -> Unit) {
    val app = LocalContext.current.applicationContext as RikoHubApp
    val state by model.state.collectAsStateWithLifecycle()
    val assignment = state.assignment
    var courseId by remember(task.id, assignment?.id) { mutableStateOf(assignment?.courseId) }
    var academicState by remember(task.id, assignment?.id) { mutableStateOf(assignment?.state ?: AcademicState.RECEIVED) }
    var reference by remember(task.id, assignment?.id) { mutableStateOf(assignment?.submissionReference.orEmpty()) }
    var courseMenu by remember { mutableStateOf(false) }
    val zone = app.container.clock.zone()
    Dialog(onDismissRequest = { if (!state.busy) onDismiss() }) {
        Surface(shape = RikoShapes.asymA, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().heightIn(max = 680.dp).verticalScroll(rememberScrollState()).padding(Spacing.space5),
                verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                Text(stringResource(R.string.assignment_title), style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() })
                Text(state.task?.title ?: task.title, style = MaterialTheme.typography.titleMedium)
                if (state.loading) CircularProgressIndicator()
                if (state.readFailed) {
                    Text(stringResource(R.string.assignment_read_failed), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    TextButton(onClick = model::retryRead, modifier = Modifier.heightIn(min = Spacing.touch)) { Text(stringResource(R.string.retry)) }
                } else if (!state.loading) {
                    if (state.task == null) Text(stringResource(R.string.assignment_task_unavailable),
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    Text(stringResource(if (assignment == null) R.string.assignment_create_hint else R.string.assignment_saved_separately),
                        style = MaterialTheme.typography.bodySmall)
                    Column {
                        Text(stringResource(R.string.assignment_course), style = MaterialTheme.typography.titleSmall)
                        OutlinedButton(onClick = { courseMenu = true }, enabled = !state.busy,
                            modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touch)) {
                            Text(state.courses.firstOrNull { it.id == courseId }?.title ?: stringResource(R.string.assignment_no_course))
                        }
                        DropdownMenu(expanded = courseMenu, onDismissRequest = { courseMenu = false }) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.assignment_no_course)) }, onClick = { courseId = null; courseMenu = false })
                            state.courses.filter { it.archivedAt == null || it.id == assignment?.courseId }.forEach { course ->
                                DropdownMenuItem(text = { Text(course.title) }, onClick = { courseId = course.id; courseMenu = false })
                            }
                        }
                        if (assignment?.state == AcademicState.SUBMITTED || assignment?.state == AcademicState.COMPLETED) {
                            TextButton(onClick = { academicState = AcademicState.IN_PROGRESS }, enabled = !state.busy,
                                modifier = Modifier.heightIn(min = Spacing.touch).testTag("assignment-reopen")) {
                                Text(stringResource(R.string.assignment_reopen))
                            }
                        }
                    }
                    Column {
                        Text(stringResource(R.string.assignment_lifecycle), style = MaterialTheme.typography.titleSmall)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.space1), verticalArrangement = Arrangement.spacedBy(Spacing.space1)) {
                            model.transitions(assignment?.state ?: AcademicState.RECEIVED).forEach { option ->
                                FilterChip(selected = academicState == option, onClick = { academicState = option }, enabled = !state.busy,
                                    label = { Text(stringResource(option.label())) }, modifier = Modifier.heightIn(min = Spacing.touch))
                            }
                        }
                    }
                    OutlinedTextField(value = reference, onValueChange = { if (it.length <= 2000) reference = it },
                        label = { Text(stringResource(R.string.assignment_submission_reference)) }, enabled = !state.busy,
                        supportingText = { Text("${reference.length}/2000") }, minLines = 2, modifier = Modifier.fillMaxWidth().testTag("assignment-reference"))
                    Text(stringResource(R.string.assignment_task_status_note, stringResource((state.task ?: task).status.label())), style = MaterialTheme.typography.bodySmall)
                    assignment?.submittedAt?.let { submittedAt ->
                        Text(stringResource(R.string.assignment_submission_recorded, timestamp(submittedAt, zone)), style = MaterialTheme.typography.bodySmall)
                    }
                    state.task?.let { DeadlineInfo(it, assignment, zone) }
                    assignment?.deadlineChanges?.takeIf { it.isNotEmpty() }?.let { changes ->
                        Text(stringResource(R.string.assignment_deadline_history), style = MaterialTheme.typography.titleSmall)
                        changes.forEach { change ->
                            Text(stringResource(R.string.assignment_deadline_change,
                                deadline(change.previousDeadlineAt, change.previousHasDeadlineTime, zone),
                                deadline(change.newDeadlineAt, change.newHasDeadlineTime, zone),
                                timestamp(change.changedAt, zone)), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (state.writeFailed) Text(stringResource(R.string.assignment_write_failed), color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    if (state.writeSucceeded) Text(stringResource(R.string.assignment_saved), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    Button(onClick = { model.save(courseId, academicState, reference) }, enabled = !state.busy && state.task != null && reference.length <= 2000,
                        modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touch).testTag("assignment-save")) {
                        Text(stringResource(if (state.busy) R.string.saving else R.string.assignment_save))
                    }
                }
                TextButton(onClick = onDismiss, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touch)) {
                    Text(stringResource(R.string.close))
                }
            }
        }
    }
}

@Composable
private fun DeadlineInfo(task: Task, assignment: AcademicAssignment?, zone: ZoneId) {
    val original = assignment?.let { it.originalDeadlineAt to it.originalHasDeadlineTime }
        ?: (task.deadlineAt to task.hasDeadlineTime)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space1)) {
        Text(stringResource(R.string.assignment_deadline), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.assignment_original_deadline, deadline(original.first, original.second, zone)))
        Text(stringResource(R.string.assignment_current_deadline, deadline(task.deadlineAt, task.hasDeadlineTime, zone)))
    }
}

private fun deadline(value: Long?, hasTime: Boolean, zone: ZoneId): String {
    if (value == null) return "—"
    val instant = Instant.ofEpochMilli(value).atZone(zone)
    val date = instant.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("id-ID")))
    return if (hasTime) "$date, ${instant.format(DateTimeFormatter.ofPattern("HH.mm", Locale.ROOT))}" else date
}

private fun timestamp(value: Long, zone: ZoneId) = Instant.ofEpochMilli(value).atZone(zone)
    .format(DateTimeFormatter.ofPattern("d MMM yyyy HH.mm", Locale.forLanguageTag("id-ID")))

private fun AcademicState.label() = when (this) {
    AcademicState.RECEIVED -> R.string.assignment_state_received
    AcademicState.PLANNED -> R.string.assignment_state_planned
    AcademicState.IN_PROGRESS -> R.string.assignment_state_in_progress
    AcademicState.REVIEW -> R.string.assignment_state_review
    AcademicState.SUBMITTED -> R.string.assignment_state_submitted
    AcademicState.COMPLETED -> R.string.assignment_state_completed
}
