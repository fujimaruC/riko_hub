package id.riko.hub.ui.capture

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import id.riko.hub.R
import id.riko.hub.RikoHubApp
import id.riko.hub.domain.model.Task
import id.riko.hub.navigation.Destination
import id.riko.hub.ui.components.TaskEditorSheet
import id.riko.hub.ui.components.rikoFormFieldColors
import id.riko.hub.ui.tasks.TaskEffect
import id.riko.hub.ui.tasks.TasksViewModel
import id.riko.hub.ui.tasks.deadlineLabel
import id.riko.hub.ui.theme.ChefButter
import id.riko.hub.ui.theme.ChefCanvas
import id.riko.hub.ui.theme.ChefCoral
import id.riko.hub.ui.theme.ChefCyan
import id.riko.hub.ui.theme.ChefGround
import id.riko.hub.ui.theme.ChefInk
import id.riko.hub.ui.theme.ChefLime
import id.riko.hub.ui.theme.ChefLine
import id.riko.hub.ui.theme.ChefMuted
import id.riko.hub.ui.theme.ChefPanel
import id.riko.hub.ui.theme.ChefRaised
import id.riko.hub.ui.theme.ChefSage
import id.riko.hub.ui.theme.NotoCondensedBlack
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
        model.effects.collect { effect ->
            when (effect) {
                is TaskEffect.Failed -> if (snackbar.showSnackbar(failed, retry, withDismissAction = true,
                        duration = SnackbarDuration.Indefinite) == SnackbarResult.ActionPerformed) effect.retry()
                is TaskEffect.Deleted -> if (snackbar.showSnackbar(deleted, undo, withDismissAction = true,
                        duration = SnackbarDuration.Indefinite) == SnackbarResult.ActionPerformed) model.restore(effect.id)
            }
        }
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

    Box(
        Modifier
            .fillMaxSize()
            .background(ChefCanvas)
            .testTag("screen-Capture"),
    ) {
        LazyColumn(
            Modifier.fillMaxSize().imePadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header Bar: AI OPERATIONS LEAD
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "TANGKAP CEPAT",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = ChefMuted,
                        )
                        Text(
                            text = "AI OPERATIONS LEAD",
                            fontFamily = NotoCondensedBlack,
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            color = ChefInk,
                            modifier = Modifier.semantics { heading() },
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ChefPanel)
                            .border(1.dp, ChefLine, CircleShape)
                            .clickable { onNavigate(Destination.ProjectsSpace) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("↗", color = ChefInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Assistant Chat Bubble Box
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = ChefPanel,
                    border = BorderStroke(1.dp, ChefLine),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        // Speaker Row: Avatar + Name + Port info
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, ChefLime, CircleShape),
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.riko_avatar),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                            Column {
                                Text(
                                    text = "Riko AI Assistant",
                                    fontFamily = NotoCondensedBlack,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = ChefInk,
                                )
                                Text(
                                    text = "Lokal di perangkat · Port 8080",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ChefMuted,
                                )
                            }
                        }

                        // Speech bubble text
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = ChefRaised,
                            border = BorderStroke(1.dp, ChefLine),
                        ) {
                            Text(
                                text = "Halo, Riko! Draf Skripsi Bab 3 harus diserahkan sebelum 14.00. Dua tugas prioritas masih menunggu tindakanmu. Mau saya catatkan ide baru, to-do, atau jadwal bimbingan?",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ChefInk,
                                modifier = Modifier.padding(14.dp),
                            )
                        }

                        // Suggestion prompt chips (horizontal scroll)
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            SuggestionChip(
                                icon = "Hi!",
                                text = "Cek batas waktu skripsi",
                                color = ChefCyan,
                                onClick = { draft = "Cek tenggat revisi bab 3 skripsi" },
                            )
                            SuggestionChip(
                                icon = "📁",
                                text = "Checklist bimbingan",
                                color = ChefSage,
                                onClick = { draft = "Buat checklist persiapan bimbingan dosen" },
                            )
                            SuggestionChip(
                                icon = "✳",
                                text = "Catat ide riset",
                                color = ChefButter,
                                onClick = { draft = "Ide: gunakan sampling purposive 30 guru" },
                            )
                        }
                    }
                }
            }

            // Input Capture Box
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = ChefPanel,
                    border = BorderStroke(1.dp, ChefLine),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            placeholder = { Text("Tulis ide atau tugas cepat...", color = ChefMuted) },
                            colors = rikoFormFieldColors(),
                            enabled = !noteSaving && !state.busy,
                            isError = noteTooLong,
                            minLines = 3,
                            maxLines = 6,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().testTag("capture-draft"),
                        )

                        // Action Buttons Row: SIMPAN TUGAS & SIMPAN CATATAN
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick = { taskDraft?.let { editorUsesDraft = true; model.edit(it) } },
                                enabled = taskDraft != null && !state.busy && !noteSaving,
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ChefLime, contentColor = ChefGround),
                                modifier = Modifier.weight(1f).height(46.dp).testTag("capture-task"),
                            ) {
                                Text("＋ TUGAS", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { notes.save(draft) },
                                enabled = draft.isNotBlank() && !noteTooLong && !state.busy && !noteSaving,
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ChefInk),
                                border = BorderStroke(1.dp, ChefLine),
                                modifier = Modifier.weight(1f).height(46.dp).testTag("capture-note"),
                            ) {
                                Text(if (noteSaving) "MENYIMPAN..." else "＋ CATATAN", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        if (noteFailed) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(failed, color = ChefCoral, style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.width(8.dp))
                                TextButton(onClick = { notes.save(draft) }) { Text(retry, color = ChefLime) }
                            }
                        }
                    }
                }
            }

            // Vault Link Button
            item {
                Surface(
                    onClick = { onNavigate(Destination.Vault) },
                    shape = RoundedCornerShape(20.dp),
                    color = ChefPanel,
                    border = BorderStroke(1.dp, ChefLine),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("capture-vault"),
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            Modifier.size(34.dp).clip(CircleShape).background(ChefSage),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("📁", fontSize = 16.sp)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Gudang Materi & Berkas",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = ChefInk,
                            )
                            Text(
                                text = "Buka Drive dan berkas riset tersimpan",
                                style = MaterialTheme.typography.bodySmall,
                                color = ChefMuted,
                            )
                        }
                        Text("↗", color = ChefMuted, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Recent Captures List
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "TANGKAPAN TERAKHIR",
                        fontFamily = NotoCondensedBlack,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = ChefMuted,
                        modifier = Modifier.semantics { heading() },
                    )
                    TextButton(onClick = { onNavigate(Destination.ProjectsSpace) }) {
                        Text("PAPAN GAGASAN ↗", color = ChefLime, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            when {
                state.readFailed -> item {
                    TextButton(onClick = model::retryRead) { Text(retry, color = ChefLime) }
                }
                state.loading -> item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = ChefLime) }
                recent.isEmpty() -> item {
                    Text("Belum ada tangkapan tersimpan.", color = ChefMuted, style = MaterialTheme.typography.bodySmall)
                }
                else -> items(recent, key = { it.id }) { task ->
                    CaptureRecentCard(task = task, deadline = deadlineLabel(task, state.now, state.zone)) {
                        editorUsesDraft = false
                        model.edit(task)
                    }
                }
            }

            item { Spacer(Modifier.height(40.dp)) }
        }

        if (state.editor == null) SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    state.editor?.let { task ->
        TaskEditorSheet(
            task = task,
            now = state.now,
            zone = state.zone,
            busy = state.busy,
            snackbar = snackbar,
            onChange = model::change,
            onSave = { saveRequested = true; model.save() },
            onDelete = { saveRequested = false; editorUsesDraft = false; model.delete(task) },
            onDismiss = { saveRequested = false; editorUsesDraft = false; model.dismissEditor() },
            quickAdd = task.id == 0L,
        )
    }
}

@Composable
private fun SuggestionChip(
    icon: String,
    text: String,
    color: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = color,
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(icon, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ChefGround)
            Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ChefGround)
        }
    }
}

@Composable
private fun CaptureRecentCard(task: Task, deadline: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = ChefPanel,
        border = BorderStroke(1.dp, ChefLine),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(ChefCoral),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = ChefInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = deadline,
                    style = MaterialTheme.typography.bodySmall,
                    color = ChefMuted,
                )
            }
            Text("↗", color = ChefMuted, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
