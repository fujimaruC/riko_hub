package id.riko.hub.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import id.riko.hub.R
import id.riko.hub.RikoHubApp
import id.riko.hub.domain.model.*
import id.riko.hub.ui.tasks.ChecklistFields
import id.riko.hub.ui.tasks.TaskExtensionFields
import id.riko.hub.ui.tasks.label
import id.riko.hub.ui.theme.*
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorSheet(task: Task, now: Long, zone: ZoneId, busy: Boolean, snackbar: SnackbarHostState,
                    onChange: (Task) -> Unit, onSave: () -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit, quickAdd: Boolean = false,
                    reminderPermission: ReminderPermissionState? = null) {
    val app = LocalContext.current.applicationContext as RikoHubApp
    val permission = reminderPermission ?: rememberReminderPermissionState { app.container.reminders.requestReconciliation() }
    var details by remember(task.id) { mutableStateOf(!quickAdd) }
    var estimateRaw by rememberSaveable(task.id) { mutableStateOf(task.estimatedMinutes?.toString().orEmpty()) }
    var reminderRaw by rememberSaveable(task.id) { mutableStateOf(task.reminderMinutesBefore?.toString().orEmpty()) }
    val changeTask: (Task) -> Unit = { changed ->
        if (task.deadlineAt != null && changed.deadlineAt == null) reminderRaw = ""
        onChange(changed)
    }
    ModalBottomSheet(onDismissRequest = { if (!busy) onDismiss() }, shape = RikoShapes.sheet,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().imePadding()) {
            SnackbarHost(snackbar)
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.space5).padding(bottom = Spacing.space6),
                verticalArrangement = Arrangement.spacedBy(Spacing.space4)) {
                Text(stringResource(if (task.id == 0L) R.string.new_task else R.string.edit_task), style = MaterialTheme.typography.titleLarge)
                TaskTitle(task, busy, validTaskInput(task, estimateRaw, reminderRaw), onChange, onSave)
                TaskChoices(stringResource(R.string.category), Category.entries, task.category, { it.label() }, !busy) {
                    onChange(task.copy(category = it))
                }
                TaskDeadlineFields(task, now, zone, !busy, changeTask)
                if (quickAdd) TextButton(onClick = { details = !details }) { Text(stringResource(R.string.home_details)) }
                if (details) {
                TaskExtensionFields(task, !busy, estimateRaw, { raw ->
                    estimateRaw = raw
                    val value = raw.toIntOrNull()
                    if (raw.isBlank() || value?.let { it in 1..1440 } == true) onChange(task.copy(estimatedMinutes = value))
                }, reminderRaw, { raw ->
                    reminderRaw = raw
                    val value = raw.toIntOrNull()
                    if (raw.isBlank() || value?.let { it in 0..10080 } == true) onChange(task.copy(reminderMinutesBefore = value))
                })
                if (reminderRaw.isNotBlank()) {
                    Text(stringResource(R.string.reminder_timing_help), style = MaterialTheme.typography.bodySmall)
                    ReminderPermissionNotice(permission)
                }
                TaskChoices(stringResource(R.string.recurrence), Recurrence.entries, task.recurrence,
                    { it.label() }, !busy && task.deadlineAt != null) { onChange(task.copy(recurrence = it)) }
                ChecklistFields(task, !busy, onChange)
                TaskChoices(stringResource(R.string.priority), Priority.entries, task.priority, { it.label() }, !busy) {
                    onChange(task.copy(priority = it))
                }
                TaskChoices(stringResource(R.string.status), TaskStatus.entries, task.status, { it.label() }, !busy) {
                    onChange(task.copy(status = it))
                }
                OutlinedTextField(value = task.notes.orEmpty(), onValueChange = { onChange(task.copy(notes = it)) },
                    label = { Text(stringResource(R.string.task_notes)) }, enabled = !busy,
                    isError = (task.notes?.length ?: 0) > 2000, supportingText = { Text(stringResource(R.string.notes_help)) },
                    modifier = Modifier.fillMaxWidth().testTag("task-notes"), minLines = 2)
                }
                Button(onClick = onSave, enabled = !busy && validTaskInput(task, estimateRaw, reminderRaw), shape = RikoShapes.pill,
                    modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touch).testTag("task-save")) {
                    Text(stringResource(if (busy) R.string.saving else R.string.save))
                }
                if (task.id != 0L) TextButton(onClick = onDelete, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.delete_task))
                }
                TextButton(onClick = onDismiss, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.close)) }
            }
        }
    }
}

private fun validTaskInput(task: Task, estimate: String, reminder: String) =
    task.title.trim().length in 1..120 && (task.notes?.length ?: 0) <= 2000 &&
        (estimate.isBlank() || estimate.toIntOrNull()?.let { it in 1..1440 } == true) &&
        (reminder.isBlank() || reminder.toIntOrNull()?.let { it in 0..10080 } == true) &&
        (reminder.isBlank() || task.deadlineAt != null) && (task.recurrence == Recurrence.NONE || task.deadlineAt != null) &&
        task.checklist.size <= 100 && task.checklist.all { it.id.isNotBlank() && it.title.trim().length in 1..120 } &&
        task.checklist.map { it.id }.toSet().size == task.checklist.size

@Composable
private fun TaskTitle(task: Task, busy: Boolean, canSave: Boolean, onChange: (Task) -> Unit, onSave: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (task.id == 0L) focus.requestFocus() }
    OutlinedTextField(value = task.title, onValueChange = { onChange(task.copy(title = it)) }, enabled = !busy,
        label = { Text(stringResource(R.string.task_title)) },
        isError = task.title.isNotEmpty() && task.title.trim().length !in 1..120,
        supportingText = { Text(stringResource(R.string.title_help)) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { if (!busy && canSave) onSave() }),
        modifier = Modifier.fillMaxWidth().focusRequester(focus).testTag("task-title"))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> TaskChoices(label: String, choices: List<T>, selected: T, text: (T) -> Int, enabled: Boolean, onSelect: (T) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
            choices.forEach { choice ->
                FilterChip(shape = RikoShapes.pill, selected = selected == choice, onClick = { onSelect(choice) }, enabled = enabled,
                    label = { Text(stringResource(text(choice))) }, modifier = Modifier.heightIn(min = Spacing.touch))
            }
        }
    }
}

@Preview @Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TaskChoicesPreview() = RikoHubTheme {
    Surface { TaskChoices(stringResource(R.string.category), Category.entries, Category.PERSONAL, { it.label() }, true) {} }
}
