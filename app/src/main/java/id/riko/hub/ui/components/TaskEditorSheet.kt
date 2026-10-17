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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import id.riko.hub.R
import id.riko.hub.RikoHubApp
import id.riko.hub.domain.model.*
import id.riko.hub.ui.tasks.ChecklistFields
import id.riko.hub.ui.tasks.TaskExtensionFields
import id.riko.hub.ui.academic.AssignmentEditorDialog
import id.riko.hub.ui.tasks.label
import id.riko.hub.ui.tasks.deadlineLabel
import id.riko.hub.ui.theme.*
import java.time.ZoneId

enum class TaskEditorPresentation { TASK, PROJECT, NOTE, ROUTINE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorSheet(task: Task, now: Long, zone: ZoneId, busy: Boolean, snackbar: SnackbarHostState,
                    onChange: (Task) -> Unit, onSave: () -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit, quickAdd: Boolean = false,
                    reminderPermission: ReminderPermissionState? = null, onPin: (() -> Unit)? = null,
                    pinned: Boolean = false, presentation: TaskEditorPresentation = TaskEditorPresentation.TASK) {
    val app = LocalContext.current.applicationContext as RikoHubApp
    val permission = reminderPermission ?: rememberReminderPermissionState { app.container.reminders.requestReconciliation() }
    var details by remember(task.id, presentation) { mutableStateOf(!quickAdd && presentation !in setOf(
        TaskEditorPresentation.PROJECT, TaskEditorPresentation.NOTE, TaskEditorPresentation.ROUTINE)) }
    var showAcademic by remember(task.id) { mutableStateOf(false) }
    var estimateRaw by rememberSaveable(task.id) { mutableStateOf(task.estimatedMinutes?.toString().orEmpty()) }
    var reminderRaw by rememberSaveable(task.id) { mutableStateOf(task.reminderMinutesBefore?.toString().orEmpty()) }
    val changeTask: (Task) -> Unit = { changed ->
        if (task.deadlineAt != null && changed.deadlineAt == null) reminderRaw = ""
        onChange(if (presentation == TaskEditorPresentation.ROUTINE) changed.copy(recurrence = Recurrence.DAILY) else changed)
    }
    val canSave = validTaskInput(task, estimateRaw, reminderRaw, presentation)
    ModalBottomSheet(onDismissRequest = { if (!busy) onDismiss() }, shape = RikoShapes.sheet,
        containerColor = FormGround,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        MaterialTheme(colorScheme = rikoFormColorScheme()) {
        Column(Modifier.fillMaxWidth().imePadding()) {
            SnackbarHost(snackbar)
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.space5).padding(bottom = Spacing.space6),
                verticalArrangement = Arrangement.spacedBy(Spacing.space4)) {
                Surface(shape = RikoShapes.wide, color = FormPanel) {
                  Column(Modifier.fillMaxWidth().padding(Spacing.space4), verticalArrangement = Arrangement.spacedBy(Spacing.space1)) {
                    Text(stringResource(when (presentation) {
                        TaskEditorPresentation.PROJECT -> if (task.id == 0L) R.string.contextual_project_new else R.string.contextual_project_edit
                        TaskEditorPresentation.NOTE -> if (task.id == 0L) R.string.contextual_note_new else R.string.contextual_note_edit
                        TaskEditorPresentation.ROUTINE -> if (task.id == 0L) R.string.contextual_routine_new else R.string.contextual_routine_edit
                        TaskEditorPresentation.TASK -> if (task.id == 0L) R.string.new_task else R.string.edit_task
                    }),
                        style = MaterialTheme.typography.titleLarge, color = FormInk, modifier = Modifier.semantics { heading() })
                    if (task.id != 0L) {
                        Text(task.title, style = MaterialTheme.typography.titleLarge, color = FormInk)
                        if (presentation != TaskEditorPresentation.NOTE) {
                            Text(stringResource(task.status.label()), style = MaterialTheme.typography.labelMedium,
                                color = FormMuted)
                            Text(deadlineLabel(task, now, zone), style = MaterialTheme.typography.bodyMedium,
                                color = FormMuted)
                        }
                    } else Text(stringResource(task.category.label()), style = MaterialTheme.typography.bodyMedium,
                        color = FormMuted)
                  }
                }
                if (onPin != null) {
                    TextButton(onClick = onPin, enabled = !busy,
                        modifier = Modifier.heightIn(min = Spacing.touch).testTag("task-workspace-pin")) {
                        Text(stringResource(if (pinned) R.string.home_unpin else R.string.home_pin))
                    }
                }
                TaskTitle(task, busy, onChange, onSave, when (presentation) {
                    TaskEditorPresentation.PROJECT -> R.string.contextual_project_name
                    TaskEditorPresentation.NOTE -> R.string.contextual_note_title
                    TaskEditorPresentation.ROUTINE -> R.string.contextual_routine_name
                    else -> R.string.task_title
                }, canSave)
                if (presentation == TaskEditorPresentation.NOTE) {
                    OutlinedTextField(value = task.notes.orEmpty(), onValueChange = { onChange(task.copy(notes = it)) },
                        label = { Text(stringResource(R.string.contextual_note_body)) }, enabled = !busy,
                        colors = rikoFormFieldColors(),
                        isError = (task.notes?.length ?: 0) > 2000, supportingText = { Text(stringResource(R.string.notes_help)) },
                        modifier = Modifier.fillMaxWidth().testTag("task-notes"), minLines = 4)
                }
                if (presentation == TaskEditorPresentation.PROJECT) {
                    OutlinedTextField(value = task.notes.orEmpty(), onValueChange = { onChange(task.copy(notes = it)) },
                        label = { Text(stringResource(R.string.contextual_project_description)) }, enabled = !busy,
                        colors = rikoFormFieldColors(),
                        isError = (task.notes?.length ?: 0) > 2000, supportingText = { Text(stringResource(R.string.notes_help)) },
                        modifier = Modifier.fillMaxWidth().testTag("task-notes"), minLines = 2)
                }
                if (presentation == TaskEditorPresentation.TASK) TaskChoices(stringResource(R.string.category), Category.entries, task.category, { it.label() }, !busy) {
                    onChange(task.copy(category = it))
                }
                if (presentation == TaskEditorPresentation.TASK && task.category == Category.ACADEMIC) TextButton(onClick = { showAcademic = true }, enabled = !busy && task.id != 0L,
                    modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touch).testTag("task-academic-workflow")) {
                    Text(stringResource(R.string.assignment_open))
                }
                if (presentation == TaskEditorPresentation.TASK && task.category == Category.ACADEMIC && task.id == 0L) Text(stringResource(R.string.assignment_save_task_first), style = MaterialTheme.typography.bodySmall)
                if (presentation != TaskEditorPresentation.NOTE) TaskDeadlineFields(task, now, zone, !busy, changeTask,
                    if (presentation == TaskEditorPresentation.PROJECT) R.string.contextual_project_target_date else if (presentation == TaskEditorPresentation.ROUTINE) R.string.contextual_routine_schedule else R.string.deadline)
                if (presentation == TaskEditorPresentation.ROUTINE) Text(stringResource(R.string.contextual_routine_frequency,
                    stringResource(R.string.recurrence_daily)), style = MaterialTheme.typography.bodyMedium)
                if (presentation == TaskEditorPresentation.PROJECT) {
                    ChecklistFields(task, !busy, onChange, R.string.contextual_project_milestones,
                        R.string.contextual_project_milestone, R.string.contextual_project_add_milestone,
                        R.string.contextual_project_remove_milestone)
                    TaskChoices(stringResource(R.string.status), TaskStatus.entries, task.status, { it.label() }, !busy) {
                        onChange(task.copy(status = it))
                    }
                }
                if (quickAdd || presentation in setOf(TaskEditorPresentation.PROJECT, TaskEditorPresentation.NOTE, TaskEditorPresentation.ROUTINE))
                    TextButton(onClick = { details = !details }) { Text(stringResource(R.string.home_details)) }
                if (details) {
                if (presentation == TaskEditorPresentation.NOTE) TaskDeadlineFields(task, now, zone, !busy, changeTask)
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
                if (presentation != TaskEditorPresentation.ROUTINE) TaskChoices(stringResource(R.string.recurrence), Recurrence.entries, task.recurrence,
                    { it.label() }, !busy && task.deadlineAt != null) { onChange(task.copy(recurrence = it)) }
                if (presentation == TaskEditorPresentation.NOTE) ChecklistFields(task, !busy, onChange)
                if (presentation != TaskEditorPresentation.NOTE && presentation != TaskEditorPresentation.PROJECT) ChecklistFields(task, !busy, onChange)
                if (presentation != TaskEditorPresentation.NOTE || details) TaskChoices(stringResource(R.string.priority), Priority.entries, task.priority, { it.label() }, !busy) {
                    onChange(task.copy(priority = it))
                }
                if (presentation != TaskEditorPresentation.PROJECT) TaskChoices(stringResource(R.string.status), TaskStatus.entries, task.status, { it.label() }, !busy) {
                    onChange(task.copy(status = it))
                }
                if (presentation == TaskEditorPresentation.TASK || presentation == TaskEditorPresentation.ROUTINE) OutlinedTextField(value = task.notes.orEmpty(), onValueChange = { onChange(task.copy(notes = it)) },
                    label = { Text(stringResource(R.string.task_notes)) }, enabled = !busy,
                    colors = rikoFormFieldColors(),
                    isError = (task.notes?.length ?: 0) > 2000, supportingText = { Text(stringResource(R.string.notes_help)) },
                    modifier = Modifier.fillMaxWidth().testTag("task-notes"), minLines = 2)
                }
                Button(onClick = onSave, enabled = !busy && canSave, shape = RikoShapes.pill,
                    modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touch).testTag("task-save")) {
                    Text(stringResource(if (busy) R.string.saving else when (presentation) {
                        TaskEditorPresentation.PROJECT -> R.string.contextual_project_save
                        TaskEditorPresentation.NOTE -> R.string.contextual_note_save
                        TaskEditorPresentation.ROUTINE -> R.string.contextual_routine_save
                        TaskEditorPresentation.TASK -> R.string.save
                    }))
                }
                if (task.id != 0L && presentation != TaskEditorPresentation.NOTE) OutlinedButton(
                    onClick = { changeTask(task.copy(status = if (task.status == TaskStatus.DONE) TaskStatus.NOT_STARTED else TaskStatus.DONE)); onSave() },
                    enabled = !busy && canSave,
                    modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touch).testTag("task-completion-action"),
                ) {
                    Text(stringResource(if (presentation == TaskEditorPresentation.PROJECT) {
                        if (task.status == TaskStatus.DONE) R.string.contextual_project_reopen else R.string.contextual_project_complete
                    } else if (task.status == TaskStatus.DONE) R.string.renaissance_task_reopen else R.string.renaissance_task_complete))
                }
                if (task.id != 0L) TextButton(onClick = onDelete, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(when (presentation) {
                        TaskEditorPresentation.PROJECT -> R.string.contextual_project_delete
                        TaskEditorPresentation.NOTE -> R.string.contextual_note_delete
                        TaskEditorPresentation.ROUTINE -> R.string.contextual_routine_delete
                        TaskEditorPresentation.TASK -> R.string.delete_task
                    }))
                }
                TextButton(onClick = onDismiss, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.close)) }
            }
        }
        }
    }
    if (showAcademic && task.id != 0L) AssignmentEditorDialog(task) { showAcademic = false }
}

private val FormGround = Color(0xFF222320)
private val FormPanel = Color(0xFF30312D)
private val FormField = Color(0xFF242520)
private val FormInk = Color(0xFFF8F6EC)
private val FormMuted = Color(0xFFB9BAAE)
@Composable
internal fun rikoFormColorScheme() = MaterialTheme.colorScheme.copy(
    primary = Color(0xFFD9F35B), onPrimary = FormGround,
    secondary = Color(0xFFFA6892), onSecondary = FormGround,
    surface = FormPanel, onSurface = FormInk,
    surfaceVariant = FormField, onSurfaceVariant = FormMuted,
    background = FormGround, onBackground = FormInk,
    outline = Color(0xFF56574E), outlineVariant = Color(0xFF3B3C37),
)
@Composable
internal fun rikoFormFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = FormField, unfocusedContainerColor = FormField,
    disabledContainerColor = FormField, errorContainerColor = FormField,
    focusedBorderColor = Color(0xFFD9F35B), unfocusedBorderColor = Color(0xFF56574E),
    focusedLabelColor = Color(0xFFD9F35B), unfocusedLabelColor = FormMuted,
)

internal fun validTaskInput(task: Task, estimate: String, reminder: String,
                            presentation: TaskEditorPresentation = TaskEditorPresentation.TASK) =
    task.title.trim().length in 1..120 && (task.notes?.length ?: 0) <= 2000 &&
        (estimate.isBlank() || estimate.toIntOrNull()?.let { it in 1..1440 } == true) &&
        (reminder.isBlank() || reminder.toIntOrNull()?.let { it in 0..10080 } == true) &&
        (reminder.isBlank() || task.deadlineAt != null) && (task.recurrence == Recurrence.NONE || task.deadlineAt != null) &&
        task.checklist.size <= 100 && task.checklist.all { it.id.isNotBlank() && it.title.trim().length in 1..120 } &&
        task.checklist.map { it.id }.toSet().size == task.checklist.size && when (presentation) {
        TaskEditorPresentation.NOTE -> !task.notes.isNullOrBlank()
        TaskEditorPresentation.ROUTINE -> task.deadlineAt != null && task.recurrence == Recurrence.DAILY
        else -> true
    }

@Composable
private fun TaskTitle(task: Task, busy: Boolean, onChange: (Task) -> Unit, onSave: () -> Unit, label: Int, enabledToSave: Boolean) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (task.id == 0L) focus.requestFocus() }
    OutlinedTextField(value = task.title, onValueChange = { onChange(task.copy(title = it)) }, enabled = !busy,
        label = { Text(stringResource(label)) },
        colors = rikoFormFieldColors(),
        isError = task.title.isNotEmpty() && task.title.trim().length !in 1..120,
        supportingText = { Text(stringResource(R.string.title_help)) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { if (!busy && enabledToSave) onSave() }),
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
