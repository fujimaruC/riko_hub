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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
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
import id.riko.hub.ui.theme.RikoShapes
import id.riko.hub.ui.theme.Spacing
import kotlinx.coroutines.CancellationException

@Composable
fun VaultScreen(onNavigate: (Destination) -> Unit, onComingSoon: () -> Unit, sharedModel: VaultViewModel? = null) {
    val app = LocalContext.current.applicationContext as RikoHubApp
    val model: VaultViewModel = sharedModel ?: viewModel(factory = viewModelFactory {
        initializer { VaultViewModel(app.container.materials, app.container.driveAuthorization) }
    })
    VaultRoute(model, onNavigate)
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
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
    var fileFilter by rememberSaveable(state.folderId, state.search, state.connectedEmail) { mutableStateOf("all") }
    // shortcut: type filters cover loaded pages only; use Drive-side filters if the connector gains them.
    val visibleFiles = state.files.filter { file ->
        when (fileFilter) {
            "folder" -> file.isFolder
            "pdf" -> file.mimeType == "application/pdf"
            "image" -> file.mimeType.startsWith("image/")
            else -> true
        }
    }
    val folders = visibleFiles.filter { it.isFolder }
    val documents = visibleFiles.filterNot { it.isFolder }
    Box(Modifier.fillMaxSize().testTag("screen-Vault")) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = Spacing.space5, vertical = Spacing.space4),
            verticalArrangement = Arrangement.spacedBy(Spacing.space3),
        ) {
            item("library-hero") { VaultLibraryHero() }
            item("source") {
                VaultPanel {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                        Icon(painterResource(R.drawable.ic_cloud), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(Spacing.space6))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.library_drive), style = MaterialTheme.typography.titleMedium)
                            Text(state.connectedEmail ?: stringResource(R.string.library_not_connected),
                                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (state.connectedEmail == null) {
                        Text(stringResource(R.string.library_connect_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        state.error?.let { Text(stringResource(errorString(it)), color = MaterialTheme.colorScheme.error) }
                        Button(onClick = model::connect, enabled = !state.authenticating, modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touch)) {
                            Text(stringResource(R.string.vault_connect))
                        }
                    } else {
                        state.connectedName?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.space2),
                            verticalArrangement = Arrangement.spacedBy(Spacing.space1)) {
                            TextButton(onClick = model::refresh, enabled = !state.authenticating, modifier = Modifier.heightIn(min = Spacing.touch)) {
                                Text(stringResource(R.string.vault_refresh))
                            }
                            TextButton(onClick = model::disconnect, modifier = Modifier.heightIn(min = Spacing.touch)) {
                                Text(stringResource(R.string.vault_disconnect))
                            }
                        }
                    }
                    if (state.authenticating) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text(stringResource(R.string.vault_authenticating), color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    }
                }
            }
            item("local-source") {
                Row(Modifier.fillMaxWidth().padding(vertical = Spacing.space2),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.space3), verticalAlignment = Alignment.Top) {
                    Icon(painterResource(R.drawable.ic_smartphone), null, modifier = Modifier.size(Spacing.space5), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.space1)) {
                        Text(stringResource(R.string.library_local_title), style = MaterialTheme.typography.labelLarge)
                        Text(stringResource(R.string.library_local_body), color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            state.uploadMessage?.let { msg ->
                item("upload-message") {
                    Text(when {
                        msg == "disconnected" -> stringResource(R.string.vault_disconnected_local)
                        msg == "disconnected_revoked" -> stringResource(R.string.vault_disconnected_revoked)
                        msg == "cancelled" -> stringResource(R.string.vault_upload_cancelled)
                        msg.startsWith("success:") -> stringResource(R.string.vault_upload_success, msg.removePrefix("success:"))
                        else -> stringResource(R.string.vault_upload_failure, stringResource(errorString(msg.removePrefix("failure:"))))
                    }, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
            }
            if (state.connectedEmail != null) {
                if (state.uploading) item("upload-progress") {
                    VaultPanel {
                        Text(stringResource(R.string.vault_uploading), style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                        val total = state.uploadTotal
                        if (total != null && total > 0L) LinearProgressIndicator(
                            progress = { ((state.uploadProgress ?: 0L).toFloat() / total.toFloat()).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                        else LinearProgressIndicator(Modifier.fillMaxWidth())
                        TextButton(onClick = model::cancelUpload, modifier = Modifier.heightIn(min = Spacing.touch)) { Text(stringResource(R.string.vault_cancel)) }
                    }
                }
                item("search") {
                    OutlinedTextField(value = state.search, onValueChange = model::search, singleLine = true,
                        label = { Text(stringResource(R.string.library_search)) },
                        supportingText = { Text(stringResource(R.string.library_search_scope)) },
                        shape = RikoShapes.small, modifier = Modifier.fillMaxWidth())
                }
                item("browse-location") {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space1)) {
                        Text(stringResource(if (state.search.isBlank()) R.string.library_browse else R.string.library_search_results),
                            style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.space1), verticalAlignment = Alignment.CenterVertically) {
                            state.breadcrumbs.forEachIndexed { index, crumb ->
                                if (index > 0) Text("/", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                TextButton(onClick = { model.navigateToBreadcrumb(index) }, modifier = Modifier.heightIn(min = Spacing.touch)) {
                                    Text(crumb.second)
                                }
                            }
                        }
                        Button(onClick = model::startUpload, enabled = !state.uploading && !state.authenticating,
                            modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touch)) {
                            Text(stringResource(R.string.vault_upload))
                        }
                    }
                }
                item("browse-types") {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space1)) {
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                            listOf("all" to R.string.library_all, "folder" to R.string.library_folders,
                                "pdf" to R.string.library_pdf, "image" to R.string.library_images).forEach { (key, label) ->
                                FilterChip(selected = fileFilter == key, onClick = { fileFilter = key },
                                    label = { Text(stringResource(label)) }, modifier = Modifier.heightIn(min = Spacing.touch))
                            }
                        }
                        Text(stringResource(R.string.library_filter_scope), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                state.error?.let { code ->
                    item("error") {
                        val needsReconnect = code == "reauth" || !model.hasActiveSession()
                        VaultStatus(stringResource(errorString(code)),
                            action = stringResource(if (needsReconnect) R.string.vault_reconnect else R.string.vault_retry)) {
                            if (needsReconnect) model.reconnect() else model.refresh()
                        }
                    }
                }
                if (state.loading) item("loading") {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text(stringResource(R.string.vault_loading), color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    }
                }
                if (!state.loading && state.error == null && visibleFiles.isEmpty()) item("empty") {
                    VaultPanel {
                        Icon(painterResource(R.drawable.ic_vault_folder), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(Spacing.space8))
                        Text(stringResource(when {
                            state.files.isNotEmpty() -> R.string.library_filter_empty
                            state.search.isNotBlank() -> R.string.library_search_empty
                            else -> R.string.library_folder_empty
                        }), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(when {
                            state.files.isNotEmpty() -> R.string.library_filter_empty_body
                            state.search.isNotBlank() -> R.string.library_search_empty_body
                            else -> R.string.library_folder_empty_body
                        }), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (fileFilter != "all") TextButton(onClick = { fileFilter = "all" }) { Text(stringResource(R.string.library_show_all)) }
                        else if (state.search.isNotBlank()) TextButton(onClick = { model.search("") }) { Text(stringResource(R.string.library_clear_search)) }
                    }
                }
                // Retain already-loaded entries during refresh and page failures.
                if (folders.isNotEmpty()) {
                    item("folder-heading") { VaultSectionHeading(stringResource(R.string.library_folders), folders.size) }
                    items(folders, key = { "file-${it.id}" }) { file -> VaultFileRow(file, onClick = { model.open(file) }) }
                }
                if (documents.isNotEmpty()) {
                    item("file-heading") { VaultSectionHeading(stringResource(R.string.library_files), documents.size) }
                    items(documents, key = { "file-${it.id}" }) { file -> VaultFileRow(file, onClick = { model.open(file) }) }
                }
                if (state.nextPageToken != null) item("load-more") {
                    OutlinedButton(onClick = model::loadMore, enabled = !state.loadingMore && !state.loading && model.hasActiveSession(),
                        modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touch)) {
                        if (state.loadingMore) CircularProgressIndicator(Modifier.size(Spacing.space5), strokeWidth = Spacing.pathStroke)
                        else Text(stringResource(R.string.vault_load_more))
                    }
                }
                if (state.pageLimitReached) item("page-limit") {
                    Text(stringResource(R.string.vault_page_limit), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

            }

            item("footer-space") { Spacer(Modifier.height(Spacing.space6)) }
        }
        SnackbarHost(messageHost, Modifier.align(Alignment.BottomCenter))
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
private fun VaultLibraryHero() {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                Text(stringResource(R.string.library_eyebrow), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.library_hero_note), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            VaultShelfArt(Modifier.width(88.dp).height(108.dp))
        }
        Text(stringResource(R.string.library_title), style = MaterialTheme.typography.displayLarge, modifier = Modifier.semantics { heading() })
        Text(stringResource(R.string.library_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider(Modifier.padding(top = Spacing.space3), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun VaultShelfArt(modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawOval(scheme.primary.copy(alpha = .07f), Offset(w * .05f, h * .02f), Size(w * .94f, h * .94f))
        drawRoundRect(scheme.surfaceVariant, Offset(w * .12f, h * .07f), Size(w * .82f, h * .84f), CornerRadius(12.dp.toPx()))
        drawRoundRect(scheme.outlineVariant, Offset(w * .12f, h * .07f), Size(w * .82f, h * .84f), CornerRadius(12.dp.toPx()), style = Stroke(1.dp.toPx()))
        for (shelf in listOf(.47f, .85f)) {
            drawLine(scheme.primary.copy(alpha = .55f), Offset(w * .1f, h * shelf), Offset(w * .96f, h * shelf), 3.dp.toPx(), StrokeCap.Round)
            val colors = listOf(scheme.primary, scheme.secondary, scheme.onSurfaceVariant, scheme.tertiary)
            for (i in 0..3) {
                val x = w * (.2f + .16f * i)
                val bookHeight = h * (if (i % 2 == 0) .23f else .29f)
                val y = h * shelf - bookHeight - 2.dp.toPx()
                drawRoundRect(colors[i].copy(alpha = .64f), Offset(x, y), Size(w * .12f, bookHeight), CornerRadius(2.dp.toPx()))
                drawLine(scheme.surface.copy(alpha = .6f), Offset(x + w * .025f, y + bookHeight * .2f),
                    Offset(x + w * .095f, y + bookHeight * .2f), 1.dp.toPx())
                drawLine(scheme.surface.copy(alpha = .6f), Offset(x + w * .025f, y + bookHeight * .78f),
                    Offset(x + w * .095f, y + bookHeight * .78f), 1.dp.toPx())
            }
        }
    }
}

@Composable
private fun VaultPanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RikoShapes.asymA, color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.fillMaxWidth().padding(Spacing.space4), verticalArrangement = Arrangement.spacedBy(Spacing.space3), content = content)
    }
}

@Composable
private fun VaultStatus(message: String, action: String, onRetry: () -> Unit) {
    VaultPanel {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        TextButton(onClick = onRetry, modifier = Modifier.heightIn(min = Spacing.touch)) { Text(action) }
    }
}

@Composable
private fun VaultSectionHeading(title: String, count: Int) {
    Row(Modifier.fillMaxWidth().padding(top = Spacing.space3), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        Text(stringResource(R.string.library_loaded_count, count), style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun VaultFileRow(file: DriveFile, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RikoShapes.small, color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(Modifier.fillMaxWidth().heightIn(min = Spacing.touch).padding(Spacing.space3),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.space3)) {
            val icon = when {
                file.isFolder -> R.drawable.ic_vault_folder
                file.mimeType == "application/pdf" -> R.drawable.ic_vault_pdf
                file.mimeType.startsWith("image/") -> R.drawable.ic_vault_image
                file.mimeType.startsWith("application/vnd.google-apps.") -> R.drawable.ic_vault_document
                else -> R.drawable.ic_vault_file
            }
            Surface(shape = RoundedCornerShape(Spacing.space2), color = MaterialTheme.colorScheme.primary.copy(alpha = .1f)) {
                Icon(painterResource(icon), null, tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(Spacing.space2).size(Spacing.space6))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.space1)) {
                Text(file.name, style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.library_file_source, stringResource(fileTypeLabel(file))),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                file.modifiedTime?.take(10)?.let { modified ->
                    Text(stringResource(R.string.library_modified, modified), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (file.isAppAuthorized) Text(stringResource(R.string.vault_authorized), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(painterResource(R.drawable.ic_chevron_right), null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(Spacing.space5))
        }
    }
}

private fun fileTypeLabel(file: DriveFile): Int = when {
    file.isFolder -> R.string.library_folder_type
    file.mimeType == "application/pdf" -> R.string.library_pdf
    file.mimeType.startsWith("image/") -> R.string.library_image_type
    file.mimeType == "application/vnd.google-apps.document" -> R.string.library_google_document
    file.mimeType == "application/vnd.google-apps.spreadsheet" -> R.string.library_google_sheet
    file.mimeType == "application/vnd.google-apps.presentation" -> R.string.library_google_slides
    else -> R.string.library_file_type
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
