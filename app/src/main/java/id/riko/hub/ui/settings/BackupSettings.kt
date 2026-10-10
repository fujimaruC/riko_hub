package id.riko.hub.ui.settings

import android.net.Uri
import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import id.riko.hub.R
import id.riko.hub.data.backup.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class BackupUiState(
    val busy: Boolean = false,
    val bytes: ByteArray? = null,
    val pickerIssued: Boolean = false,
    val preview: BackupPreview? = null,
    val message: String? = null,
    val failure: BackupFailure? = null,
    val restoreSettings: Boolean = false,
    val result: BackupImportResult? = null,
)

private class BackupViewModel(private val service: LocalBackupService) : ViewModel() {
    private val mutableState = MutableStateFlow(BackupUiState())
    val state = mutableState.asStateFlow()

    fun export() {
        if (mutableState.value.busy) return
        mutableState.update { BackupUiState(busy = true) }
        viewModelScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) { service.export() }
                mutableState.value = BackupUiState(bytes = bytes, busy = true)
            } catch (error: BackupException) {
                mutableState.value = BackupUiState(failure = error.reason)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                mutableState.value = BackupUiState(message = "export")
            }
        }
    }

    fun claimExportPicker() {
        if (mutableState.value.bytes != null && !mutableState.value.pickerIssued) {
            mutableState.update { it.copy(pickerIssued = true) }
        }
    }

    fun pickerFailed() = mutableState.update { BackupUiState(message = "pickerFailed") }

    fun writeExport(uri: Uri?, write: (ByteArray) -> Unit) {
        val bytes = mutableState.value.bytes
        if (bytes == null) {
            if (uri != null) {
                mutableState.update { it.copy(busy = false, message = "exportLost") }
            }
            return
        }
        mutableState.update { it.copy(bytes = null, busy = uri != null) }
        if (uri == null) return
        viewModelScope.launch {
            val message = try {
                withContext(Dispatchers.IO) { write(bytes) }
                "exported"
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                "exportWrite"
            }
            mutableState.update { it.copy(busy = false, message = message) }
        }
    }

    fun startImportPicker() = mutableState.update { it.copy(busy = true) }

    fun chooseImport(uri: Uri?, read: (Uri) -> java.io.InputStream?) {
        if (uri == null) { mutableState.update { it.copy(busy = false) }; return }
        mutableState.update { BackupUiState(busy = true) }
        viewModelScope.launch {
            try {
                val preview = withContext(Dispatchers.IO) { read(uri)?.use { service.preview(it) } ?: throw java.io.IOException() }
                mutableState.value = BackupUiState(preview = preview)
            } catch (error: BackupException) {
                mutableState.value = BackupUiState(failure = error.reason)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                mutableState.value = BackupUiState(message = "importRead")
            }
        }
    }

    fun restoreSettings(enabled: Boolean) = mutableState.update { it.copy(restoreSettings = enabled) }
    fun dismissPreview() = mutableState.update { it.copy(preview = null, restoreSettings = false) }

    fun confirmImport() {
        val preview = mutableState.value.preview ?: return
        if (mutableState.value.busy) return
        val restore = mutableState.value.restoreSettings
        mutableState.update { it.copy(busy = true, preview = null) }
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { service.importBackup(preview, restore) }
                mutableState.value = BackupUiState(result = result)
            } catch (error: BackupException) {
                mutableState.value = BackupUiState(failure = error.reason)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                mutableState.value = BackupUiState(message = "importFailed")
            }
        }
    }

}

@Composable
fun BackupSettingsSection(service: LocalBackupService) {
    val context = LocalContext.current
    val model: BackupViewModel = viewModel(factory = viewModelFactory { initializer { BackupViewModel(service) } })
    val state by model.state.collectAsStateWithLifecycle()
    val exportPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        model.writeExport(uri, { bytes ->
            (context.contentResolver.openOutputStream(requireNotNull(uri), "wt") ?: throw java.io.IOException()).use { stream -> stream.write(bytes) }
        })
    }
    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        model.chooseImport(uri) { context.contentResolver.openInputStream(it) }
    }
    LaunchedEffect(state.bytes) {
        if (state.bytes != null && !state.pickerIssued) {
            model.claimExportPicker()
            try {
                exportPicker.launch("riko-hub-backup.json")
            } catch (_: ActivityNotFoundException) {
                model.pickerFailed()
            } catch (_: SecurityException) {
                model.pickerFailed()
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.backup_description), style = MaterialTheme.typography.bodyMedium)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = model::export, enabled = !state.busy) { Text(stringResource(R.string.backup_export)) }
            OutlinedButton(onClick = {
                model.startImportPicker()
                try { importPicker.launch(arrayOf("application/json")) }
                catch (_: ActivityNotFoundException) { model.pickerFailed() }
                catch (_: SecurityException) { model.pickerFailed() }
            }, enabled = !state.busy) {
                Text(stringResource(R.string.backup_import))
            }
        }
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.message?.let { key ->
            val resultText = stringResource(when (key) {
                    "export" -> R.string.backup_export_failed
                    "exported" -> R.string.backup_export_done
                    "exportWrite" -> R.string.backup_export_write_failed
                    "exportLost" -> R.string.backup_export_lost
                    "importRead" -> R.string.backup_read_failed
                    "pickerFailed" -> R.string.backup_picker_failed
                    else -> R.string.backup_import_failed
                })
            Text(resultText, color = MaterialTheme.colorScheme.error.takeIf { key != "exported" } ?: MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        state.result?.let { result ->
            Text(stringResource(R.string.backup_imported, result.insertedTasks, result.insertedEvents), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            if (result.preferencesFailed) Text(stringResource(R.string.backup_preferences_failed), color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            if (result.remindersFailed) Text(stringResource(R.string.backup_reminders_failed), color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        state.failure?.let { reason ->
            Text(stringResource(when (reason) {
                BackupFailure.INVALID -> R.string.backup_invalid
                BackupFailure.TOO_LARGE -> R.string.backup_too_large
                BackupFailure.UNSUPPORTED_VERSION -> R.string.backup_version
                BackupFailure.STALE_PREVIEW -> R.string.backup_stale
            }), color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
    }

    state.preview?.let { preview ->
        AlertDialog(
            onDismissRequest = model::dismissPreview,
            title = { Text(stringResource(R.string.backup_preview_title)) },
            text = {
                Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState())) {
                    Text(stringResource(R.string.backup_preview_counts, preview.totalTasks, preview.totalEvents))
                    Text(stringResource(R.string.backup_preview_new, preview.newTasks, preview.newEvents))
                    Text(stringResource(R.string.backup_preview_duplicates, preview.duplicates))
                    Text(stringResource(R.string.backup_preview_conflicts, preview.conflicts))
                    Text(stringResource(R.string.backup_existing_wins))
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth().toggleable(state.restoreSettings, role = Role.Checkbox, onValueChange = model::restoreSettings), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(checked = state.restoreSettings, onCheckedChange = null)
                        Text(stringResource(R.string.backup_restore_settings))
                    }
                    Text(stringResource(R.string.backup_restore_explanation), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = model::confirmImport, enabled = !state.busy) { Text(stringResource(R.string.backup_confirm)) } },
            dismissButton = { TextButton(onClick = model::dismissPreview) { Text(stringResource(R.string.backup_cancel)) } },
        )
    }
}
