package id.riko.hub.ui.tasks

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    val isDone = task.status == TaskStatus.DONE
    val isHighPriority = task.priority == Priority.HIGH && !isDone

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
            Box(Modifier.fillMaxSize().background(PreviewRaised, RoundedCornerShape(16.dp))
                .padding(Spacing.space4), contentAlignment = if (swipe.dismissDirection == SwipeToDismissBoxValue.StartToEnd)
                Alignment.CenterStart else Alignment.CenterEnd) {
                Text(stringResource(if (swipe.dismissDirection == SwipeToDismissBoxValue.StartToEnd) R.string.done else R.string.delete_task),
                    color = PreviewInk)
            }
        }) {
        Surface(
            onClick = onEdit,
            enabled = enabled,
            shape = RoundedCornerShape(16.dp),
            color = if (isHighPriority) PreviewOrange else PreviewPanel,
            contentColor = if (isHighPriority) PreviewGround else PreviewInk,
            border = if (isHighPriority) null else androidx.compose.foundation.BorderStroke(1.dp, PreviewLine.copy(alpha = .6f)),
            modifier = Modifier.fillMaxWidth().testTag("task-${task.id}"),
        ) {
            Row(
                Modifier.heightIn(min = 68.dp).padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                IconButton(
                    onClick = { complete(!isDone) },
                    enabled = enabled,
                    modifier = Modifier.size(28.dp).semantics { contentDescription = checkboxLabel }.testTag("check-${task.id}"),
                ) {
                    Box(
                        Modifier.size(22.dp).clip(CircleShape)
                            .background(if (isDone) PreviewLime else Color.Transparent)
                            .border(1.5.dp, if (isDone) PreviewLime else if (isHighPriority) PreviewGround else Color(0xFFA0A197), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isDone) Text("✓", color = PreviewGround, style = MaterialTheme.typography.labelSmall)
                    }
                }
                TaskRowText(task, showCategory, isHighPriority, isDone, now, zone, Modifier.weight(1f))
                Box(
                    Modifier.size(28.dp).clip(CircleShape)
                        .background(if (isHighPriority) PreviewGround else Color(0xFF474841)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("↗", color = if (isHighPriority) PreviewOrange else PreviewInk, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun TaskRowText(task: Task, showCategory: Boolean, isHighPriority: Boolean, isDone: Boolean, now: Long, zone: ZoneId, modifier: Modifier) {
    val metaColor = if (isHighPriority) PreviewGround.copy(alpha = .75f) else PreviewMuted
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            task.title,
            style = MaterialTheme.typography.titleMedium,
            color = if (isHighPriority) PreviewGround else PreviewInk,
            textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
            maxLines = if (LocalDensity.current.fontScale >= 1.3f) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showCategory) {
                Text(
                    stringResource(task.category.label()).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = metaColor,
                )
                Text("·", style = MaterialTheme.typography.labelSmall, color = metaColor)
            }
            Text(deadlineLabel(task, now, zone), style = MaterialTheme.typography.labelSmall, color = metaColor)
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
