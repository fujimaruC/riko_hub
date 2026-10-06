package id.riko.hub.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import id.riko.hub.R
import id.riko.hub.domain.logic.HomeSnapshot
import id.riko.hub.domain.model.Task
import id.riko.hub.ui.tasks.*
import id.riko.hub.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BubblePeekSheet(task: Task, snapshot: HomeSnapshot, busy: Boolean, snackbar: SnackbarHostState,
                    onDismiss: () -> Unit, onComplete: () -> Unit, onStart: () -> Unit, onEdit: () -> Unit, onPin: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, shape = RikoShapes.sheet,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(Spacing.space5),
            verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
            SnackbarHost(snackbar)
            Text(task.title, style = MaterialTheme.typography.titleLarge)
            Text(stringResource(task.category.label()), style = MaterialTheme.typography.labelLarge)
            Text(deadlineLabel(task, snapshot.now, snapshot.zone))
            Text(stringResource(task.priority.label()))
            Text(stringResource(task.status.label()))
            task.notes?.takeIf { it.isNotBlank() }?.let { Text(it) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                Button(onClick = onComplete, enabled = !busy) { Text(stringResource(R.string.home_complete)) }
                OutlinedButton(onClick = onStart, enabled = !busy) { Text(stringResource(R.string.home_start)) }
                OutlinedButton(onClick = onEdit, enabled = !busy) { Text(stringResource(R.string.home_edit)) }
            }
            TextButton(onClick = onPin, enabled = !busy) { Text(stringResource(
                if (task.id == snapshot.validPin) R.string.home_unpin else R.string.home_pin)) }
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        }
    }
}
