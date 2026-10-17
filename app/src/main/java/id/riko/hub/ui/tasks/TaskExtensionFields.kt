package id.riko.hub.ui.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import id.riko.hub.R
import id.riko.hub.domain.model.ChecklistItem
import id.riko.hub.domain.model.Task
import id.riko.hub.ui.components.rikoFormFieldColors
import id.riko.hub.ui.theme.RikoShapes
import id.riko.hub.ui.theme.Spacing

@Composable
fun TaskExtensionFields(task: Task, enabled: Boolean, estimate: String, onEstimate: (String) -> Unit,
                        reminder: String, onReminder: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
        OutlinedTextField(estimate, onEstimate, enabled = enabled, singleLine = true,
            label = { Text(stringResource(R.string.task_estimate)) },
            colors = rikoFormFieldColors(),
            supportingText = { Text(stringResource(R.string.task_estimate_help)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().testTag("task-estimate"))
        OutlinedTextField(reminder, onReminder, enabled = enabled && task.deadlineAt != null, singleLine = true,
            label = { Text(stringResource(R.string.task_reminder)) },
            colors = rikoFormFieldColors(),
            supportingText = { Text(stringResource(R.string.task_reminder_help)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().testTag("task-reminder"))
    }
}

@Composable
fun ChecklistFields(task: Task, enabled: Boolean, onChange: (Task) -> Unit, label: Int = R.string.task_checklist,
                    itemLabel: Int = R.string.task_checklist_item, addLabel: Int = R.string.add_task_checklist,
                    removeLabel: Int = R.string.delete_task) {
    var draft by remember(task.id) { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space1)) {
        Text(stringResource(label), style = MaterialTheme.typography.titleSmall)
        task.checklist.forEachIndexed { index, item ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(item.done, enabled = enabled, onCheckedChange = { done ->
                    onChange(task.copy(checklist = task.checklist.toMutableList().also { it[index] = item.copy(done = done) }))
                })
                OutlinedTextField(item.title, enabled = enabled, singleLine = true,
                    onValueChange = { title -> onChange(task.copy(checklist = task.checklist.toMutableList().also { it[index] = item.copy(title = title) })) },
                    colors = rikoFormFieldColors(),
                    modifier = Modifier.weight(1f).testTag("checklist-title-$index"))
                TextButton(enabled = enabled, onClick = { onChange(task.copy(checklist = task.checklist - item)) }) {
                    Text(stringResource(removeLabel))
                }
            }
        }
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            OutlinedTextField(draft, { draft = it }, enabled = enabled, singleLine = true,
                label = { Text(stringResource(itemLabel)) }, colors = rikoFormFieldColors(),
                modifier = Modifier.weight(1f).testTag("checklist-new"))
            TextButton(enabled = enabled && draft.trim().isNotEmpty() && task.checklist.size < 100,
                onClick = {
                    onChange(task.copy(checklist = task.checklist + ChecklistItem(title = draft.trim())))
                    draft = ""
                }) { Text(stringResource(addLabel)) }
        }
    }
}
