package id.riko.hub.ui.capture

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import id.riko.hub.R
import id.riko.hub.RikoHubApp
import id.riko.hub.domain.model.Task
import id.riko.hub.navigation.Destination
import id.riko.hub.ui.components.TaskEditorSheet
import id.riko.hub.ui.tasks.TaskEffect
import id.riko.hub.ui.tasks.TasksViewModel
import id.riko.hub.ui.tasks.deadlineLabel
import id.riko.hub.ui.theme.RikoShapes
import id.riko.hub.ui.theme.Spacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CaptureScreen(onNavigate: (Destination) -> Unit) {
    val app = LocalContext.current.applicationContext as RikoHubApp
    val model: TasksViewModel = viewModel(key = "capture-tasks", factory = viewModelFactory {
        initializer { TasksViewModel(app.container.tasks, app.container.clock) }
    })
    val state by model.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val failed = stringResource(R.string.save_failed)
    val retry = stringResource(R.string.retry)
    val saved = stringResource(R.string.capture_saved)
    val noteSaved = stringResource(R.string.capture_studio_note_saved)
    val deleted = stringResource(R.string.deleted_task)
    val undo = stringResource(R.string.undo)
    var draft by rememberSaveable { mutableStateOf("") }
    var saveRequested by rememberSaveable { mutableStateOf(false) }
    var editorUsesDraft by rememberSaveable { mutableStateOf(false) }
    val notes: CaptureNotesViewModel = viewModel(key = "capture-notes", factory = viewModelFactory {
        initializer { CaptureNotesViewModel { body -> app.container.spaceNotes.saveIdea(body = body); Unit } }
    })
    val noteState by notes.state.collectAsStateWithLifecycle()
    val noteSaving = noteState.busy
    val noteFailed = noteState.failed
    val taskDraft = remember(draft) { captureTaskDraft(draft) }
    val noteTooLong = draft.trim().length > 10_000
    LaunchedEffect(noteState.savedBody) {
        val body = noteState.savedBody ?: return@LaunchedEffect
        if (draft.trim() == body) draft = ""
        notes.consumeSaved()
        snackbarScope.launch { snackbar.showSnackbar(noteSaved) }
    }
    LaunchedEffect(model) {
        model.effects.collect { effect -> when (effect) {
            is TaskEffect.Failed -> if (snackbar.showSnackbar(failed, retry, withDismissAction = true,
                duration = SnackbarDuration.Indefinite) == SnackbarResult.ActionPerformed) effect.retry()
            is TaskEffect.Deleted -> if (snackbar.showSnackbar(deleted, undo, withDismissAction = true,
                duration = SnackbarDuration.Indefinite) == SnackbarResult.ActionPerformed) model.restore(effect.id)
        } }
    }
    LaunchedEffect(state.editor) {
        if (state.editor == null && saveRequested) {
            saveRequested = false
            if (editorUsesDraft) draft = ""
            editorUsesDraft = false
            snackbar.showSnackbar(saved)
        }
    }
    val recent = remember(state.tasks) { state.tasks.filter { it.deletedAt == null }.sortedByDescending { it.createdAt }.take(3) }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).testTag("screen-Capture")) {
        LazyColumn(Modifier.fillMaxSize().imePadding(), contentPadding = PaddingValues(Spacing.space5),
            verticalArrangement = Arrangement.spacedBy(Spacing.space4)) {
            item { CaptureStudioHeader() }
            item {
                val edge = MaterialTheme.colorScheme.primary
                val ink = MaterialTheme.colorScheme.onSurface
                Column(Modifier.fillMaxWidth().clip(RikoShapes.asymA)
                    .background(MaterialTheme.colorScheme.surface).border(1.dp, edge.copy(alpha = .35f), RikoShapes.asymA)
                    .drawWithCache {
                        val lines = Path()
                        val step = 28.dp.toPx()
                        var y = step
                        while (y < size.height) { lines.moveTo(0f, y); lines.lineTo(size.width, y); y += step }
                        onDrawBehind { drawPath(lines, ink.copy(alpha = .035f), style = Stroke(1.dp.toPx())) }
                    }.padding(Spacing.space4), verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.ic_vault_document), null, tint = edge, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(Spacing.space2))
                        Text(stringResource(R.string.capture_studio_draft), style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.semantics { heading() })
                    }
                    OutlinedTextField(value = draft, onValueChange = { draft = it },
                        label = { Text(stringResource(R.string.capture_studio_input)) },
                        placeholder = { Text(stringResource(R.string.capture_studio_placeholder)) },
                        enabled = !noteSaving && !state.busy, isError = noteTooLong, minLines = 3, maxLines = 7,
                        modifier = Modifier.fillMaxWidth().testTag("capture-draft"),
                        supportingText = { Text(stringResource(if (noteTooLong) R.string.capture_studio_note_limit else R.string.capture_studio_destinations)) })
                    if (taskDraft == null) Text(stringResource(R.string.capture_studio_task_limit),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.space2), verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                        Button(onClick = { taskDraft?.let { editorUsesDraft = true; model.edit(it) } },
                            enabled = taskDraft != null && !state.busy && !noteSaving,
                            shape = RikoShapes.pill, modifier = Modifier.heightIn(min = Spacing.touch).testTag("capture-task")) {
                            Icon(painterResource(R.drawable.ic_personal_check), null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(Spacing.space2))
                            Text(stringResource(R.string.capture_studio_task))
                        }
                        OutlinedButton(onClick = { notes.save(draft) }, enabled = draft.isNotBlank() && !noteTooLong && !state.busy && !noteSaving,
                            shape = RikoShapes.pill, modifier = Modifier.heightIn(min = Spacing.touch).testTag("capture-note")) {
                            Icon(painterResource(R.drawable.ic_personal_send), null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(Spacing.space2))
                            Text(stringResource(if (noteSaving) R.string.saving else R.string.capture_studio_note))
                        }
                    }
                    if (noteFailed) {
                        Text(failed, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { notes.save(draft) }, enabled = !noteSaving && !state.busy && !noteTooLong && draft.isNotBlank(),
                            modifier = Modifier.heightIn(min = Spacing.touch)) { Text(retry) }
                    }
                }
            }
            item {
                Surface(onClick = { onNavigate(Destination.Vault) }, shape = RikoShapes.wide,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touch).testTag("capture-vault")) {
                    Row(Modifier.padding(Spacing.space4), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                        Icon(painterResource(R.drawable.ic_vault_file), null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.capture_studio_file), style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(R.string.capture_studio_file_detail), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(painterResource(R.drawable.ic_chevron_right), null)
                    }
                }
            }
            item {
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(Spacing.space1), itemVerticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.capture_studio_recent), style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() })
                    TextButton(onClick = { onNavigate(Destination.ProjectsSpace) }, modifier = Modifier.heightIn(min = Spacing.touch)) {
                        Text(stringResource(R.string.capture_studio_idea_board))
                    }
                }
                Text(stringResource(R.string.capture_studio_recent_scope), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            when {
                state.readFailed -> item {
                    Text(stringResource(R.string.read_failed))
                    TextButton(onClick = model::retryRead) { Text(retry) }
                }
                state.loading -> item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                recent.isEmpty() -> item { Text(stringResource(R.string.capture_studio_empty), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                else -> items(recent, key = { it.id }) { task ->
                    CaptureRecentTask(task, deadlineLabel(task, state.now, state.zone), !state.busy && !noteSaving) {
                        editorUsesDraft = false
                        model.edit(task)
                    }
                }
            }
            item { Spacer(Modifier.height(Spacing.space6)) }
        }
        if (state.editor == null) SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
    state.editor?.let { task ->
        TaskEditorSheet(task, state.now, state.zone, state.busy, snackbar, model::change,
            { saveRequested = true; model.save() },
            { saveRequested = false; editorUsesDraft = false; model.delete(task) },
            { saveRequested = false; editorUsesDraft = false; model.dismissEditor() }, quickAdd = task.id == 0L)
    }
}

@Composable
private fun CaptureRecentTask(task: Task, deadline: String, enabled: Boolean, onOpen: () -> Unit) {
    Surface(onClick = onOpen, enabled = enabled, shape = RikoShapes.wide,
        color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touch)) {
        Row(Modifier.padding(Spacing.space4), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.space3)) {
            Icon(painterResource(R.drawable.ic_personal_check), null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(task.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(deadline, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CaptureStudioHeader() {
    val accent = MaterialTheme.colorScheme.primary
    val background = MaterialTheme.colorScheme.background
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
        Text(stringResource(R.string.capture_studio_title), style = MaterialTheme.typography.displayMedium,
            modifier = Modifier.semantics { heading() })
        Text(stringResource(R.string.capture_studio_subtitle), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.fillMaxWidth().height(76.dp)) {
            Canvas(Modifier.matchParentSize()) {
                val w = size.width; val h = size.height
                drawRect(Brush.radialGradient(listOf(accent.copy(alpha = .18f), Color.Transparent), Offset(w * .5f, h * .6f), w * .48f))
                val well = Path().apply {
                    moveTo(w * .08f, h * .56f)
                    cubicTo(w * .12f, h * .20f, w * .35f, h * .46f, w * .48f, h * .25f)
                    cubicTo(w * .64f, h * -.03f, w * .91f, h * .28f, w * .92f, h * .58f)
                    cubicTo(w * .96f, h * .94f, w * .65f, h * .87f, w * .49f, h * .96f)
                    cubicTo(w * .28f, h * 1.05f, w * .04f, h * .85f, w * .08f, h * .56f)
                    close()
                }
                drawPath(well, Brush.linearGradient(listOf(accent.copy(alpha = .25f), background), Offset.Zero, Offset(w, h)))
                drawPath(well, accent.copy(alpha = .45f), style = Stroke(1.5.dp.toPx()))
                drawOval(accent.copy(alpha = .12f), Offset(w * .27f, h * .59f), Size(w * .46f, h * .26f), style = Stroke(2.dp.toPx()))
                drawLine(accent.copy(alpha = .55f), Offset(w * .45f, h * .82f), Offset(w * .56f, h * .80f), 2.dp.toPx())
            }
            FloatingCaptureGlyph(R.drawable.ic_vault_document, Modifier.align(Alignment.TopCenter).rotate(-12f), accent)
            FloatingCaptureGlyph(R.drawable.ic_personal_check, Modifier.align(Alignment.CenterStart).padding(start = 40.dp).rotate(9f), MaterialTheme.colorScheme.secondary)
            FloatingCaptureGlyph(R.drawable.ic_vault_file, Modifier.align(Alignment.CenterEnd).padding(end = 40.dp).rotate(-8f), MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
private fun FloatingCaptureGlyph(icon: Int, modifier: Modifier, tint: Color) {
    Surface(modifier.size(48.dp), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, tint.copy(alpha = .45f))) {
        Box(contentAlignment = Alignment.Center) { Icon(painterResource(icon), null, tint = tint, modifier = Modifier.size(24.dp)) }
    }
}
