package id.riko.hub.ui.academic

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import id.riko.hub.R
import id.riko.hub.RikoHubApp
import id.riko.hub.domain.model.*
import id.riko.hub.ui.components.TaskEditorSheet
import id.riko.hub.ui.tasks.TaskEffect
import id.riko.hub.ui.tasks.TasksViewModel
import id.riko.hub.ui.tasks.deadlineLabel
import id.riko.hub.ui.tasks.label
import id.riko.hub.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*

private data class CourseAssignmentsState(val items: List<AssignmentTask> = emptyList(), val loading: Boolean = true, val failed: Boolean = false)

@Composable
fun CourseAssignmentsSection(courseId: Long) {
    val app = LocalContext.current.applicationContext as RikoHubApp
    var retryCount by remember(courseId) { mutableIntStateOf(0) }
    val assignments by remember(courseId, retryCount) {
        app.container.assignments.observeForCourse(courseId).map { CourseAssignmentsState(it, loading = false) }
            .onStart { emit(CourseAssignmentsState()) }
            .catch { error ->
                if (error is CancellationException) throw error
                Log.e("CourseAssignments", "Course assignment read failed", error)
                emit(CourseAssignmentsState(loading = false, failed = true))
            }
    }.collectAsStateWithLifecycle(initialValue = CourseAssignmentsState())
    val taskModel: TasksViewModel = viewModel(key = "course-assignment-tasks-$courseId", factory = viewModelFactory {
        initializer { TasksViewModel(app.container.tasks, app.container.clock) }
    })
    val taskState by taskModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val failedMessage = stringResource(R.string.save_failed)
    val deletedMessage = stringResource(R.string.deleted_task)
    val undoMessage = stringResource(R.string.undo)
    val retryMessage = stringResource(R.string.retry)
    LaunchedEffect(taskModel) {
        taskModel.effects.collect { effect ->
            when (effect) {
                is TaskEffect.Failed -> if (snackbar.showSnackbar(failedMessage, retryMessage, withDismissAction = true,
                    duration = SnackbarDuration.Indefinite) == SnackbarResult.ActionPerformed) effect.retry()
                is TaskEffect.Deleted -> if (snackbar.showSnackbar(deletedMessage, undoMessage, withDismissAction = true,
                    duration = SnackbarDuration.Indefinite) == SnackbarResult.ActionPerformed) taskModel.restore(effect.id)
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2), modifier = Modifier.fillMaxWidth().testTag("course-assignments-$courseId")) {
        Text(stringResource(R.string.course_assignments), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        when {
            assignments.loading -> CircularProgressIndicator()
            assignments.failed -> {
                Text(stringResource(R.string.assignment_read_failed))
                TextButton(onClick = { retryCount++ }, modifier = Modifier.heightIn(min = Spacing.touch)) { Text(stringResource(R.string.retry)) }
            }
            assignments.items.isEmpty() -> Text(stringResource(R.string.course_assignments_empty))
            else -> assignments.items.forEach { item ->
                Surface(shape = RikoShapes.asymA, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = { taskModel.edit(item.task) }, enabled = !taskState.busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touch).padding(Spacing.space2)) {
                        Column(Modifier.fillMaxWidth()) {
                            Text(item.task.title, style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(item.assignment.state.label()))
                            Text(deadlineLabel(item.task, taskState.now, taskState.zone), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        if (taskState.editor == null) SnackbarHost(snackbar)
    }
    taskState.editor?.let { task ->
        TaskEditorSheet(task, taskState.now, taskState.zone, taskState.busy, snackbar, taskModel::change, taskModel::save,
            { taskModel.delete(task) }, taskModel::dismissEditor)
    }
}

private fun AcademicState.label() = when (this) {
    AcademicState.RECEIVED -> R.string.assignment_state_received
    AcademicState.PLANNED -> R.string.assignment_state_planned
    AcademicState.IN_PROGRESS -> R.string.assignment_state_in_progress
    AcademicState.REVIEW -> R.string.assignment_state_review
    AcademicState.SUBMITTED -> R.string.assignment_state_submitted
    AcademicState.COMPLETED -> R.string.assignment_state_completed
}
