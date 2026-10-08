package id.riko.hub.ui.tasks

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import id.riko.hub.R
import id.riko.hub.domain.model.*
import id.riko.hub.ui.theme.*
import java.time.ZoneId
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskRow(task: Task, index: Int, showCategory: Boolean, now: Long, zone: ZoneId, enabled: Boolean,
            onEdit: () -> Unit, onComplete: (Boolean) -> Unit, onDelete: () -> Unit) {
    val checkboxLabel = stringResource(if (task.status == TaskStatus.DONE) R.string.mark_not_done else R.string.mark_done, task.title)
    val haptic = LocalHapticFeedback.current
    val complete: (Boolean) -> Unit = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onComplete(it)
    }
    val swipe = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()
    SwipeToDismissBox(state = swipe, gesturesEnabled = enabled, onDismiss = { value ->
        if (value != SwipeToDismissBoxValue.Settled) scope.launch {
            swipe.snapTo(SwipeToDismissBoxValue.Settled)
            if (enabled) when (value) {
                SwipeToDismissBoxValue.StartToEnd -> complete(true)
                SwipeToDismissBoxValue.EndToStart -> onDelete()
                else -> Unit
            }
        }
    },
        backgroundContent = {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant, RikoShapes.pill)
                .padding(Spacing.space4), contentAlignment = if (swipe.dismissDirection == SwipeToDismissBoxValue.StartToEnd)
                Alignment.CenterStart else Alignment.CenterEnd) {
                Text(stringResource(if (swipe.dismissDirection == SwipeToDismissBoxValue.StartToEnd) R.string.done else R.string.delete_task))
            }
        }) {
        Surface(onClick = onEdit, enabled = enabled, shape = if (index % 2 == 0) RikoShapes.asymA else RikoShapes.asymB,
            color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth().testTag("task-${task.id}")) {
            Row(Modifier.heightIn(min = Spacing.row).padding(Spacing.space3), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = task.status == TaskStatus.DONE, onCheckedChange = complete, enabled = enabled,
                    modifier = Modifier.semantics { contentDescription = checkboxLabel }.testTag("check-${task.id}"))
                TaskRowText(task, showCategory, now, zone, Modifier.weight(1f))
                if (task.priority == Priority.HIGH) PriorityFlag()
            }
        }
    }
}

@Composable
private fun TaskRowText(task: Task, showCategory: Boolean, now: Long, zone: ZoneId, modifier: Modifier) {
    val colors = LocalRikoColors.current
    val accent = when (task.category) {
        Category.ACADEMIC -> colors.academic
        Category.ORGANIZATION -> colors.organization
        Category.PERSONAL -> colors.personal
        Category.PROJECTS -> colors.projects
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.space1)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
            Canvas(Modifier.size(Spacing.space2)) { drawCircle(accent.color) }
            Text(task.title, style = MaterialTheme.typography.titleMedium,
                maxLines = if (LocalDensity.current.fontScale >= 1.3f) Int.MAX_VALUE else 1, overflow = TextOverflow.Ellipsis)
        }
        Text(deadlineLabel(task, now, zone), style = MaterialTheme.typography.bodyMedium)
        if (task.checklist.isNotEmpty()) {
            Text(stringResource(R.string.task_checklist_progress,
                task.checklist.count { it.done }, task.checklist.size), style = MaterialTheme.typography.labelMedium)
        }
        if (showCategory) Text(stringResource(task.category.label()), style = MaterialTheme.typography.labelMedium)
        if (task.status == TaskStatus.IN_PROGRESS) Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
            Canvas(Modifier.size(Spacing.space3)) { drawArc(accent.color, 90f, 180f, useCenter = true) }
            Text(stringResource(R.string.task_in_progress), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun PriorityFlag() {
    val colors = LocalRikoColors.current
    val description = stringResource(R.string.high_priority_description)
    Canvas(Modifier.size(Spacing.space6).semantics { contentDescription = description }) {
        drawLine(colors.urgent.color, Offset(size.width * .2f, 0f), Offset(size.width * .2f, size.height), strokeWidth = size.width * .08f)
        drawPath(Path().apply { moveTo(size.width * .2f, 0f); lineTo(size.width, size.height * .3f)
            lineTo(size.width * .2f, size.height * .6f); close() }, colors.urgent.color)
    }
}

@Preview @Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TaskRowPreview() = RikoHubTheme {
    TaskRow(Task(id = 1, title = "Persiapan UTS", priority = Priority.HIGH), 0, true, 0, ZoneId.of("UTC"), true, {}, {}, {})
}
