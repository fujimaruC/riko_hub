package id.riko.hub.ui.vault

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import id.riko.hub.R
import id.riko.hub.RikoHubApp
import id.riko.hub.data.remote.drive.DriveFile
import id.riko.hub.navigation.Destination
import id.riko.hub.ui.components.ScreenFrame
import id.riko.hub.ui.theme.RikoShapes
import id.riko.hub.ui.theme.Spacing
import kotlinx.coroutines.CancellationException

@Composable
fun VaultScreen(onNavigate: (Destination) -> Unit, onComingSoon: () -> Unit) {
    val app = LocalContext.current.applicationContext as RikoHubApp
    val model: VaultViewModel = viewModel(factory = viewModelFactory {
        initializer { VaultViewModel(app.container.materials, app.container.driveAuthorization) }
    })
    VaultRoute(model, onNavigate)
}

@Composable
fun VaultRoute(model: VaultViewModel, onNavigate: (Destination) -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val messageHost = remember { SnackbarHostState() }
    val authorizationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        model.authorizationResult(result.resultCode == android.app.Activity.RESULT_OK, result.data)
    }
    val documentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) model.upload(uri) else model.cancelUpload()
    }
    val unableToOpen = stringResource(R.string.vault_open_unavailable)
    val openError = stringResource(R.string.vault_open_failed)
    LaunchedEffect(model) {
        model.effects.collect { effect ->
            when (effect) {
                is VaultEffect.LaunchAuthorization -> runCatching {
                    authorizationLauncher.launch(IntentSenderRequest.Builder(effect.pendingIntent).build())
                }.onFailure { model.authorizationLaunchFailed(); messageHost.showSnackbar(openError) }
                VaultEffect.SelectUploadFile -> runCatching { documentLauncher.launch(UPLOAD_MIME_TYPES) }
                    .onFailure { model.cancelUpload(); messageHost.showSnackbar(unableToOpen) }
                is VaultEffect.OpenFile -> openDriveFile(context, model, effect.file, effect.session, messageHost, unableToOpen, openError)
            }
        }
    }
    BackHandler(state.breadcrumbs.size > 1) { model.backFolder() }
    ScreenFrame(Destination.Vault, onNavigate) {
        Text(stringResource(R.string.vault_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        if (state.connectedEmail == null) {
            VaultPanel {
                Text(stringResource(R.string.vault_disconnected), style = MaterialTheme.typography.bodyLarge)
                state.error?.let { Text(stringResource(errorString(it)), color = MaterialTheme.colorScheme.error) }
                if (state.authenticating) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(stringResource(R.string.vault_authenticating), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = model::connect, modifier = Modifier.heightIn(min = Spacing.touch)) { Text(stringResource(R.string.vault_connect)) }
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                Column(Modifier.weight(1f)) {
                    Text(state.connectedName ?: stringResource(R.string.vault_connected), style = MaterialTheme.typography.titleMedium)
                    Text(state.connectedEmail!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = model::refresh, modifier = Modifier.heightIn(min = Spacing.touch)) { Text(stringResource(R.string.vault_refresh)) }
                TextButton(onClick = model::disconnect, modifier = Modifier.heightIn(min = Spacing.touch)) { Text(stringResource(R.string.vault_disconnect)) }
            }
            OutlinedTextField(value = state.search, onValueChange = model::search, singleLine = true,
                label = { Text(stringResource(R.string.vault_search)) }, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Spacing.space1), verticalAlignment = Alignment.CenterVertically) {
                state.breadcrumbs.forEachIndexed { index, crumb ->
                    if (index > 0) Text("/", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { model.navigateToBreadcrumb(index) }, modifier = Modifier.heightIn(min = Spacing.touch)) { Text(crumb.second) }
                }
            }
            Button(onClick = model::startUpload, enabled = !state.uploading && !state.authenticating, modifier = Modifier.heightIn(min = Spacing.touch)) {
                Text(stringResource(R.string.vault_upload))
            }
            if (state.authenticating) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(stringResource(R.string.vault_authenticating), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            state.error?.let { code ->
                val needsReconnect = code == "reauth" || !model.hasActiveSession()
                VaultStatus(stringResource(errorString(code)), action = if (needsReconnect) stringResource(R.string.vault_reconnect) else stringResource(R.string.vault_retry)) {
                    if (needsReconnect) model.reconnect() else model.refresh()
                }
            }
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            when {
                state.loading -> Text(stringResource(R.string.vault_loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
                state.error != null -> Unit
                state.files.isEmpty() && state.error == null -> Text(stringResource(R.string.vault_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> LazyColumn(Modifier.fillMaxWidth().heightIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                    items(state.files, key = DriveFile::id) { file -> VaultFileRow(file, onClick = { model.open(file) }) }
                }
            }
            if (state.nextPageToken != null) TextButton(onClick = { model.loadMore() }, enabled = !state.loadingMore,
                modifier = Modifier.heightIn(min = Spacing.touch)) {
                if (state.loadingMore) CircularProgressIndicator(Modifier.size(Spacing.space4), strokeWidth = Spacing.pathStroke)
                else Text(stringResource(R.string.vault_load_more))
            }
            if (state.pageLimitReached) Text(stringResource(R.string.vault_page_limit), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (state.uploading) {
                val uploadTotal = state.uploadTotal
                val uploadProgress = state.uploadProgress
                if (uploadTotal != null && uploadTotal > 0L) LinearProgressIndicator(
                    progress = { (uploadProgress ?: 0L).toFloat() / uploadTotal.toFloat() }, Modifier.fillMaxWidth())
                else LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(stringResource(R.string.vault_uploading), color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = model::cancelUpload, modifier = Modifier.heightIn(min = Spacing.touch)) { Text(stringResource(R.string.vault_cancel)) }
            }
        }
        state.uploadMessage?.let { msg -> Text(when {
            msg == "disconnected" -> stringResource(R.string.vault_disconnected_local)
            msg == "disconnected_revoked" -> stringResource(R.string.vault_disconnected_revoked)
            msg == "cancelled" -> stringResource(R.string.vault_upload_cancelled)
            msg.startsWith("success:") -> stringResource(R.string.vault_upload_success, msg.removePrefix("success:"))
            else -> stringResource(R.string.vault_upload_failure, stringResource(errorString(msg.removePrefix("failure:"))))
        }, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        SnackbarHost(messageHost)
        state.pickerDestination?.let { destination ->
            AlertDialog(onDismissRequest = model::cancelPickerDestination,
                title = { Text(stringResource(R.string.vault_destination_title)) },
                text = { Text(stringResource(R.string.vault_destination_body, destination.second)) },
                confirmButton = { TextButton(onClick = model::confirmPickerDestination) { Text(stringResource(R.string.vault_continue)) } },
                dismissButton = { TextButton(onClick = model::cancelPickerDestination) { Text(stringResource(R.string.vault_cancel)) } })
        }
    }
}

@Composable
private fun VaultPanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RikoShapes.asymA, color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.fillMaxWidth().padding(Spacing.space5), verticalArrangement = Arrangement.spacedBy(Spacing.space3), content = content)
    }
}

@Composable
private fun VaultStatus(message: String, action: String, onRetry: () -> Unit) {
    VaultPanel {
        Text(message, style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onRetry, modifier = Modifier.heightIn(min = Spacing.touch)) { Text(action) }
    }
}

@Composable
private fun VaultFileRow(file: DriveFile, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RikoShapes.small, color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(Modifier.fillMaxWidth().heightIn(min = Spacing.touch).padding(horizontal = Spacing.space3, vertical = Spacing.space2),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.space3)) {
            val (icon, tint) = when {
                file.isFolder -> R.drawable.ic_vault_folder to MaterialTheme.colorScheme.primary
                file.mimeType == "application/pdf" -> R.drawable.ic_vault_pdf to MaterialTheme.colorScheme.tertiary
                file.mimeType.startsWith("image/") -> R.drawable.ic_vault_image to MaterialTheme.colorScheme.secondary
                file.mimeType.startsWith("application/vnd.google-apps.") -> R.drawable.ic_vault_document to MaterialTheme.colorScheme.primary
                else -> R.drawable.ic_vault_file to MaterialTheme.colorScheme.onSurfaceVariant
            }
            Icon(painter = androidx.compose.ui.res.painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(Spacing.space6))
            Column(Modifier.weight(1f)) {
                Text(file.name, style = MaterialTheme.typography.titleSmall)
                Text(file.modifiedTime ?: fileTypeLabel(file.mimeType), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (file.isAppAuthorized) Text(stringResource(R.string.vault_authorized), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun fileTypeLabel(mimeType: String) = when {
    mimeType == "application/pdf" -> "PDF"
    mimeType.startsWith("image/") -> "Gambar"
    mimeType.startsWith("application/vnd.google-apps.document") -> "Dokumen Google"
    mimeType.startsWith("application/vnd.google-apps.spreadsheet") -> "Spreadsheet Google"
    mimeType.startsWith("application/vnd.google-apps.presentation") -> "Presentasi Google"
    else -> mimeType
}

private fun errorString(code: String): Int = when (code) {
    "reauth" -> R.string.vault_error_auth
    "permission" -> R.string.vault_error_permission
    "offline" -> R.string.vault_error_offline
    "not_found" -> R.string.vault_error_not_found
    "quota" -> R.string.vault_error_quota
    else -> R.string.vault_error_general
}

private suspend fun openDriveFile(context: android.content.Context, model: VaultViewModel, file: DriveFile,
    session: Int, host: SnackbarHostState, unavailable: String, failed: String) {
    try {
        if (!model.hasActiveSession(session)) return
        if (file.mimeType.startsWith("application/vnd.google-apps.")) {
            if (model.hasActiveSession(session) && openWorkspaceLink(context, file)) return
            throw ActivityNotFoundException()
        }
        try {
            val cached = model.download(file, session) ?: return
            if (!model.hasActiveSession(session)) return
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.materials.files", cached)
            val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, file.mimeType)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).apply {
                    clipData = ClipData.newUri(context.contentResolver, file.name, uri)
                }
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: ActivityNotFoundException) { if (!model.hasActiveSession(session)) return else if (!openWorkspaceLink(context, file)) throw ActivityNotFoundException() }
        catch (error: Exception) { if (!model.hasActiveSession(session)) return else if (!openWorkspaceLink(context, file)) throw error }
    } catch (_: ActivityNotFoundException) { if (model.hasActiveSession(session)) host.showSnackbar(unavailable) }
    catch (cancelled: CancellationException) { throw cancelled }
    catch (_: SecurityException) { if (model.hasActiveSession(session)) host.showSnackbar(failed) }
    catch (_: Exception) { if (model.hasActiveSession(session)) host.showSnackbar(failed) }
}

private val UPLOAD_MIME_TYPES = arrayOf(
    "application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "application/vnd.oasis.opendocument.text", "application/vnd.oasis.opendocument.spreadsheet", "application/vnd.oasis.opendocument.presentation",
    "application/rtf", "application/epub+zip", "text/plain", "text/markdown", "text/csv", "image/*",
)

private fun openWorkspaceLink(context: android.content.Context, file: DriveFile): Boolean {
    val link = file.webViewLink?.let(Uri::parse) ?: return false
    if (link.scheme != "https" || link.host !in setOf("drive.google.com", "docs.google.com")) return false
    return try {
        context.startActivity(Intent(Intent.ACTION_VIEW, link).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) { false }
}
