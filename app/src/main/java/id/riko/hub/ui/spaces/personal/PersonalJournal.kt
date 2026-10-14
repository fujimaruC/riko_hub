package id.riko.hub.ui.spaces.personal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.riko.hub.R
import id.riko.hub.data.repository.SpaceNoteRepository
import id.riko.hub.domain.model.SpaceNote
import id.riko.hub.ui.spaces.SpacesBadge
import id.riko.hub.ui.theme.SpacesTokens
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.onEach
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun PersonalJournalSurface(
    repository: SpaceNoteRepository,
    now: Long,
    zone: ZoneId,
    modifier: Modifier = Modifier,
) {
    var readFailed by remember { mutableStateOf(false) }
    var readComplete by remember { mutableStateOf(false) }
    var readGeneration by rememberSaveable { mutableIntStateOf(0) }
    val entriesFlow = remember(repository, readGeneration) {
        repository.observeJournalHistory().onStart { readFailed = false; readComplete = false }
            .onEach { readComplete = true }
            .catch { error ->
                if (error is CancellationException) throw error
                readFailed = true
                emit(emptyList())
            }
    }
    val entries by entriesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val today = remember(now, zone) { Instant.ofEpochMilli(now).atZone(zone).toLocalDate() }
    val todayRaw = today.toString()
    val latestToday = entries.filter { it.journalDate == today }
        .maxWithOrNull(compareBy<SpaceNote> { it.updatedAt }.thenBy { it.syncId })
    var draftDate by rememberSaveable { mutableStateOf(todayRaw) }
    var draft by rememberSaveable { mutableStateOf("") }
    var selectedSyncId by rememberSaveable { mutableStateOf<String?>(null) }
    var dirty by rememberSaveable { mutableStateOf(false) }
    var fromHistory by rememberSaveable { mutableStateOf(false) }
    var saveError by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var deleteTargetSyncId by remember { mutableStateOf<String?>(null) }
    var pendingHistoryEntry by remember { mutableStateOf<SpaceNote?>(null) }
    var confirmDiscardDraft by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val failed = stringResource(R.string.personal_journal_save_failed)
    val deleteFailed = stringResource(R.string.personal_journal_delete_failed)
    val error = if (saveError) failed else null

    fun saveJournalDraft() {
        if (saving || !readComplete || readFailed || !dirty || draft.isBlank()) return
        val date = runCatching { LocalDate.parse(draftDate) }.getOrDefault(today)
        val syncId = selectedSyncId
        val body = draft
        saving = true
        scope.launch {
            try {
                val saved = if (syncId == null) repository.saveToday(date, body) else repository.update(syncId, body)
                draftDate = date.toString()
                selectedSyncId = saved.syncId
                draft = saved.body
                dirty = false
                fromHistory = false
                saveError = false
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                saveError = true
                snackbar.showSnackbar(failed)
            } finally { saving = false }
        }
    }

    LaunchedEffect(todayRaw, entries, readComplete, readFailed) {
        if (!readComplete || readFailed || dirty || fromHistory) return@LaunchedEffect
        val current = entries.filter { it.journalDate?.toString() == todayRaw }
            .maxWithOrNull(compareBy<SpaceNote> { it.updatedAt }.thenBy { it.syncId })
        draftDate = todayRaw
        draft = current?.body.orEmpty()
        selectedSyncId = current?.syncId
        saveError = false
    }

    val parsedDate = runCatching { LocalDate.parse(draftDate) }.getOrDefault(today)
    val dateLabel = parsedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.forLanguageTag("id-ID")))
    val shape = RoundedCornerShape(20.dp)
    Box(modifier.fillMaxWidth().clip(shape)
        .background(Brush.linearGradient(listOf(SpacesTokens.cream.copy(alpha = .09f), SpacesTokens.tangerine.copy(alpha = .05f))))
        .border(1.dp, SpacesTokens.cream.copy(alpha = .16f), shape).testTag("personal-journal-surface")) {
        Canvas(Modifier.matchParentSize()) {
            var y = 12.dp.toPx()
            while (y < size.height) {
                drawLine(SpacesTokens.cream.copy(alpha = .08f), Offset(0f, y), Offset(size.width, y), .5.dp.toPx())
                y += 22.dp.toPx()
            }
        }
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_vault_document), null,
                    tint = SpacesTokens.cream, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.personal_journal_title), color = SpacesTokens.primary,
                    style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f).semantics { heading() })
                val badge = when {
                    readFailed -> R.string.personal_journal_badge
                    !readComplete -> R.string.personal_journal_loading
                    !dirty && draft.isNotBlank() -> R.string.personal_journal_saved
                    else -> R.string.personal_journal_badge
                }
                SpacesBadge(stringResource(badge), if (!dirty && readComplete && draft.isNotBlank()) SpacesTokens.jade else SpacesTokens.secondary)
            }
            Text(dateLabel, color = SpacesTokens.secondary, style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.testTag("personal-journal-date"))
            Text(stringResource(R.string.personal_journal_prompt), color = SpacesTokens.cream.copy(alpha = .9f),
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic))
            OutlinedTextField(value = draft, onValueChange = { value ->
                if (value.length <= 10_000) {
                    draft = value
                    dirty = true
                    saveError = false
                }
            }, label = { Text(stringResource(R.string.personal_journal_field_label), color = SpacesTokens.secondary) },
                placeholder = { Text(stringResource(R.string.personal_journal_placeholder), color = SpacesTokens.secondary) },
                enabled = !saving, minLines = 3, maxLines = 8,
                supportingText = { Text(stringResource(R.string.personal_journal_character_count, draft.length), color = SpacesTokens.secondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = SpacesTokens.primary, unfocusedTextColor = SpacesTokens.primary,
                    focusedBorderColor = SpacesTokens.cream.copy(alpha = .5f), unfocusedBorderColor = SpacesTokens.cream.copy(alpha = .18f),
                    focusedContainerColor = Color.Black.copy(alpha = .14f), unfocusedContainerColor = Color.Black.copy(alpha = .14f),
                    cursorColor = SpacesTokens.tangerine,
                ), modifier = Modifier.fillMaxWidth().testTag("personal-journal-input"))
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            if (readFailed) {
                Text(stringResource(R.string.personal_journal_read_failed), color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { readGeneration++ }, enabled = !saving, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.retry))
                }
            }
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (fromHistory && !dirty) TextButton(onClick = {
                    draftDate = todayRaw
                    draft = latestToday?.body.orEmpty()
                    selectedSyncId = latestToday?.syncId
                    fromHistory = false
                    saveError = false
                }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.personal_journal_return_today), color = SpacesTokens.cream)
                }
                TextButton(onClick = { saveJournalDraft() }, enabled = readComplete && !readFailed && !saving && dirty && draft.isNotBlank(),
                    modifier = Modifier.heightIn(min = 48.dp).testTag("personal-journal-save")) {
                    Text(stringResource(if (saveError) R.string.personal_journal_retry else R.string.personal_journal_save), color = SpacesTokens.tangerine)
                }
                TextButton(onClick = { showHistory = true }, enabled = readComplete && !readFailed,
                    modifier = Modifier.heightIn(min = 48.dp)
                    .testTag("personal-journal-history")) { Text(stringResource(R.string.personal_journal_history), color = SpacesTokens.cream) }
                if (selectedSyncId != null) TextButton(onClick = { deleteTargetSyncId = selectedSyncId; confirmDelete = true }, enabled = !saving && readComplete && !readFailed,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("personal-journal-delete")) {
                    Text(stringResource(R.string.personal_journal_delete), color = MaterialTheme.colorScheme.error)
                }
            }
            Text(stringResource(when {
                !readComplete && !readFailed -> R.string.personal_journal_status_loading
                readFailed -> R.string.personal_journal_status_error
                saveError -> R.string.personal_journal_status_error
                saving -> R.string.personal_journal_status_saving
                dirty -> R.string.personal_journal_status_draft
                draft.isNotBlank() -> R.string.personal_journal_status_saved
                else -> R.string.personal_journal_status_empty
            }), color = if (saveError) MaterialTheme.colorScheme.error else SpacesTokens.secondary,
                style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("personal-journal-status"))
            SnackbarHost(snackbar)
        }
    }

    if (showHistory && readComplete && !readFailed) {
        ModalBottomSheet(onDismissRequest = { showHistory = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.personal_journal_history), style = MaterialTheme.typography.titleLarge,
                    color = SpacesTokens.primary, modifier = Modifier.semantics { heading() })
                if (entries.isEmpty()) Text(stringResource(R.string.personal_journal_history_empty), color = SpacesTokens.secondary)
                entries.forEach { note ->
                    val noteDate = note.journalDate ?: return@forEach
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(SpacesTokens.row)
                        .clickable {
                            if (dirty) {
                                pendingHistoryEntry = note
                                confirmDiscardDraft = true
                            } else {
                                draftDate = noteDate.toString()
                                draft = note.body
                                selectedSyncId = note.syncId
                                fromHistory = true
                                saveError = false
                                showHistory = false
                            }
                        }.heightIn(min = 48.dp).padding(14.dp).testTag("personal-journal-entry-${note.syncId}"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(noteDate.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.forLanguageTag("id-ID"))),
                            color = SpacesTokens.cream, style = MaterialTheme.typography.labelLarge)
                        Text(note.body, color = SpacesTokens.primary, style = MaterialTheme.typography.bodyMedium,
                            maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
    if (confirmDiscardDraft) AlertDialog(onDismissRequest = { if (!saving) { confirmDiscardDraft = false; pendingHistoryEntry = null } },
        title = { Text(stringResource(R.string.personal_journal_discard_title)) },
        text = { Text(stringResource(R.string.personal_journal_discard_body)) },
        confirmButton = { TextButton(enabled = !saving, onClick = {
            val note = pendingHistoryEntry
            val date = note?.journalDate
            if (date != null) {
                draftDate = date.toString()
                draft = note.body
                selectedSyncId = note.syncId
                dirty = false
                fromHistory = true
                saveError = false
                showHistory = false
            }
            pendingHistoryEntry = null
            confirmDiscardDraft = false
        }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.personal_journal_discard)) } },
        dismissButton = { TextButton(enabled = !saving, onClick = { pendingHistoryEntry = null; confirmDiscardDraft = false },
            modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.close)) } })
    if (confirmDelete) AlertDialog(onDismissRequest = { if (!saving) { confirmDelete = false; deleteTargetSyncId = null } },
        title = { Text(stringResource(R.string.personal_journal_delete_title)) },
        text = { Text(stringResource(R.string.personal_journal_delete_body)) },
        confirmButton = { TextButton(enabled = !saving, onClick = {
            val id = deleteTargetSyncId ?: return@TextButton
            if (!saving) {
                saving = true
                scope.launch {
                    try {
                        repository.softDelete(id)
                        confirmDelete = false
                        selectedSyncId = null
                        deleteTargetSyncId = null
                        draft = ""
                        dirty = false
                        fromHistory = false
                    } catch (error: CancellationException) { throw error }
                    catch (_: Exception) { snackbar.showSnackbar(deleteFailed) }
                    finally { saving = false }
                }
            }
        }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.personal_journal_delete)) } },
        dismissButton = { TextButton(enabled = !saving, onClick = { confirmDelete = false; deleteTargetSyncId = null },
            modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.close)) } })
}
