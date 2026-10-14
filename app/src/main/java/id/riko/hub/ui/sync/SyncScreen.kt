package id.riko.hub.ui.sync

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import id.riko.hub.R
import id.riko.hub.RikoHubApp
import id.riko.hub.data.remote.device.Laptop
import id.riko.hub.data.remote.device.canRetryHandoffUpload
import id.riko.hub.navigation.Destination
import id.riko.hub.ui.components.ScreenFrame
import id.riko.hub.ui.spaces.DevicesConnectionBridge
import id.riko.hub.ui.spaces.SpacesPageHeader
import id.riko.hub.ui.spaces.SpacesSectionHeader
import id.riko.hub.ui.spaces.deviceTransferProgress
import id.riko.hub.ui.theme.RikoShapes
import id.riko.hub.ui.theme.SpacesTokens
import id.riko.hub.ui.theme.Spacing
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.text.DateFormat
import java.util.Date

private const val LOCAL_NETWORK_PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"
@Composable
fun SyncScreen(onNavigate: (Destination) -> Unit, inSpace: Boolean = false) {
    val app = LocalContext.current.applicationContext as RikoHubApp
    val model: SyncViewModel = viewModel(factory = viewModelFactory {
        initializer { SyncViewModel(app.container.companion, app.container.handoff) }
    })
    SyncRoute(model, onNavigate, inSpace)
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun SyncRoute(model: SyncViewModel, onNavigate: (Destination) -> Unit, inSpace: Boolean = false) {
    val context = LocalContext.current
    val state by model.state.collectAsStateWithLifecycle(SyncUiState())
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    var pairingTarget by remember { mutableStateOf<Laptop?>(null) }
    var payload by remember { mutableStateOf("") }
    var textDraft by remember { mutableStateOf("") }
    var permissionAsked by remember { mutableStateOf(false) }
    var receiveTransferId by rememberSaveable { mutableStateOf<String?>(null) }
    var receivePeerId by rememberSaveable { mutableStateOf<String?>(null) }
    var hideTextPreview by remember { mutableStateOf(false) }
    val localPermission = if (Build.VERSION.SDK_INT >= 37) LOCAL_NETWORK_PERMISSION else null
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) model.setActive(true)
    }
    val openDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            lifecycle.currentStateFlow.first { it.isAtLeast(Lifecycle.State.RESUMED) }
            model.prepareSend(uri)
        }
    }
    val createDocument = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        val transferId = receiveTransferId
        val peerId = receivePeerId
        receiveTransferId = null; receivePeerId = null
        if (uri != null && transferId != null && peerId != null) model.receive(transferId, uri, peerId)
        else if (uri != null) model.removeFreshDestination(uri)
    }
    DisposableEffect(lifecycle, model) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (localPermission == null || ContextCompat.checkSelfPermission(context, localPermission) == PackageManager.PERMISSION_GRANTED) model.setActive(true)
                else if (!permissionAsked) { permissionAsked = true; permissionLauncher.launch(localPermission) }
                model.load()
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                payload = ""; textDraft = ""; pairingTarget = null; hideTextPreview = false
                model.setActive(false)
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            if (localPermission == null || ContextCompat.checkSelfPermission(context, localPermission) == PackageManager.PERMISSION_GRANTED) model.setActive(true)
            else if (!permissionAsked) { permissionAsked = true; permissionLauncher.launch(localPermission) }
            model.load()
        }
        onDispose { lifecycle.removeObserver(observer); model.setActive(false) }
    }

    val pageContent: @Composable ColumnScope.() -> Unit = {
        if (inSpace) {
            DevicesConnectionBridge(state, model::connect, model::disconnect, model::requestForget)
        } else {
            TextButton(onClick = { onNavigate(Destination.DevicesSpace) }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.devices_back_to_space))
            }
            Text(stringResource(R.string.companion_title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.companion_intro), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (localPermission != null && ContextCompat.checkSelfPermission(context, localPermission) != PackageManager.PERMISSION_GRANTED) {
            Text(stringResource(R.string.companion_permission_denied), color = MaterialTheme.colorScheme.error)
            TextButton(onClick = { permissionAsked = true; permissionLauncher.launch(localPermission) }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.companion_retry_permission)) }
        }
        if (!inSpace) state.companion.trusted?.let { trusted ->
            Card(Modifier.fillMaxWidth(), shape = RikoShapes.asymB) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(trusted.name, style = MaterialTheme.typography.titleMedium)
                    Text(if (state.companion.connected) stringResource(R.string.companion_connected) else stringResource(R.string.companion_waiting), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (state.companion.connected) OutlinedButton(onClick = model::disconnect, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.companion_disconnect)) }
                        else Button(onClick = model::connect, enabled = !state.companion.busy, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.companion_connect)) }
                        TextButton(onClick = model::requestForget, enabled = !state.companion.busy, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.companion_forget)) }
                    }
                }
            }
        }
        Text(stringResource(R.string.companion_nearby), style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = model::refresh, enabled = state.companion.networkReady && !state.companion.busy, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.companion_refresh)) }
        if (state.companion.pairing) Text(stringResource(R.string.companion_pairing_progress), color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (state.companion.laptops.isEmpty()) Text(
            when { !state.companion.networkReady -> stringResource(R.string.companion_no_wifi); state.companion.scanning -> stringResource(R.string.companion_searching); else -> stringResource(R.string.companion_empty) },
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        state.companion.laptops.forEach { laptop ->
            Card(Modifier.fillMaxWidth(), shape = RikoShapes.asymB) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) { Text(laptop.name, style = MaterialTheme.typography.titleSmall); Text(laptop.address.hostAddress.orEmpty(), style = MaterialTheme.typography.bodySmall) }
                    TextButton(enabled = state.companion.trusted == null && !state.companion.busy, onClick = { payload = ""; pairingTarget = laptop }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.companion_pair)) }
                }
            }
        }
        state.companion.error?.let { message ->
            if (!inSpace) Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            if (message.startsWith("Penyimpanan kepercayaan")) TextButton(onClick = model::requestResetTrust, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.companion_reset_trust)) }
        }

        if (state.companion.trusted != null) {
            Spacer(Modifier.height(8.dp))
            Text("File and text handoff", style = MaterialTheme.typography.titleLarge)
            Text("Transfers use the currently trusted laptop. Choose what to send or save each item explicitly.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(onClick = { model.refreshHandoff() }, enabled = state.handoff.connected && !state.handoff.busy, modifier = Modifier.heightIn(min = 48.dp)) { Text("Refresh handoff") }
                TextButton(onClick = model::requestClearHandoff, enabled = !state.handoff.busy, modifier = Modifier.heightIn(min = 48.dp)) { Text("Clear local storage") }
            }
            if (!state.handoff.connected) Text("Connect to the trusted laptop before using handoff.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else if (!state.handoff.supported) Text("Refresh handoff to check laptop support.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else if (!state.handoff.handoffAllowed) Text("Enable handoff for this paired phone in the laptop Companion.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (state.handoff.busy) {
                state.handoff.progress?.let { progress ->
                    Text("${progress.displayName}: ${progress.completedBytes} / ${progress.totalBytes} bytes")
                    if (progress.totalBytes > 0) {
                        LinearProgressIndicator(progress = { deviceTransferProgress(progress) ?: 0f }, modifier = Modifier.fillMaxWidth())
                    } else LinearProgressIndicator(Modifier.fillMaxWidth())
                } ?: LinearProgressIndicator(Modifier.fillMaxWidth())
                TextButton(onClick = model::cancelTransfer, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel handoff") }
            }
            if (!inSpace) state.handoff.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

            Text("Send a file", style = MaterialTheme.typography.titleMedium)
            state.handoff.preparedFile?.let { prepared ->
                Card(Modifier.fillMaxWidth(), shape = RikoShapes.asymB) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Review before sending", style = MaterialTheme.typography.titleSmall)
                        Text(prepared.displayName)
                        Text("${prepared.mimeType} · ${prepared.sizeBytes} bytes · SHA-256 ${prepared.sha256}", style = MaterialTheme.typography.bodySmall)
                        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(onClick = model::sendPrepared, enabled = state.handoff.connected && state.handoff.handoffAllowed && !state.handoff.busy, modifier = Modifier.heightIn(min = 48.dp)) { Text("Send to laptop") }
                            TextButton(onClick = model::discardPrepared, enabled = !state.handoff.busy, modifier = Modifier.heightIn(min = 48.dp)) { Text("Discard staged file") }
                        }
                    }
                }
            }
            OutlinedButton(onClick = { openDocument.launch(arrayOf("*/*")) }, enabled = state.handoff.connected && state.handoff.handoffAllowed && !state.handoff.busy, modifier = Modifier.heightIn(min = 48.dp)) { Text("Choose file to preview") }

            Text("Laptop offers", style = MaterialTheme.typography.titleMedium)
            val peerId = state.companion.trusted?.id
            val incomingOffers = state.handoff.offers.filter { it.transfer.direction == "laptop_to_phone" }
            if (incomingOffers.isEmpty()) Text("No files are waiting for your approval.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            incomingOffers.forEach { offer ->
                Card(Modifier.fillMaxWidth(), shape = RikoShapes.asymB) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(offer.transfer.displayName, style = MaterialTheme.typography.titleSmall)
                        Text("${offer.transfer.mimeType} · ${offer.transfer.sizeBytes} bytes")
                        Text("Saving creates a new file. The laptop is asked to send it after you choose a destination.", style = MaterialTheme.typography.bodySmall)
                        Button(onClick = {
                            if (peerId != null) {
                                receiveTransferId = offer.transfer.transferId; receivePeerId = peerId
                                createDocument.launch(offer.transfer.displayName)
                            }
                        }, enabled = peerId != null && !state.handoff.busy, modifier = Modifier.heightIn(min = 48.dp)) { Text("Choose destination and accept") }
                    }
                }
            }

            Text("Send text", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(value = textDraft, onValueChange = { textDraft = it.take(2048) }, label = { Text("Text to send") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            Button(onClick = { model.sendText(textDraft) { textDraft = "" } }, enabled = textDraft.isNotBlank() && state.handoff.connected && state.handoff.handoffAllowed && !state.handoff.busy, modifier = Modifier.heightIn(min = 48.dp)) { Text("Send text to laptop") }
            Text("Text offers", style = MaterialTheme.typography.titleMedium)
            val incomingText = state.handoff.textOffers.filter { it.direction == "laptop_to_phone" }
            if (incomingText.isEmpty()) Text("No text is waiting for your review.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            incomingText.forEach { item ->
                Card(Modifier.fillMaxWidth(), shape = RikoShapes.asymB) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) { Text("Text offer · ${item.sizeBytes} bytes", style = MaterialTheme.typography.titleSmall); Text(dateLabel(item.createdAt), style = MaterialTheme.typography.bodySmall) }
                        TextButton(onClick = { hideTextPreview = false; model.previewText(item.textId) }, enabled = !state.handoff.busy, modifier = Modifier.heightIn(min = 48.dp)) { Text("Preview") }
                    }
                }
            }
            state.handoff.textPreview?.takeUnless { hideTextPreview }?.let { preview ->
                AlertDialog(onDismissRequest = { hideTextPreview = true },
                    title = { Text("Review text before consuming") },
                    text = { Text(preview.text, Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) },
                        confirmButton = { TextButton(onClick = { model.consumeText(preview.textId) }, enabled = !state.handoff.busy, modifier = Modifier.heightIn(min = 48.dp)) { Text("Consume text") } },
                    dismissButton = { TextButton(onClick = { hideTextPreview = true }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Keep for now") } })
            }

            Text("Transfer history", style = MaterialTheme.typography.titleMedium)
            if (state.handoff.history.isEmpty()) Text("No local transfer history yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            state.handoff.history.forEach { transfer ->
                Card(Modifier.fillMaxWidth(), shape = RikoShapes.asymB) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(transfer.displayName, style = MaterialTheme.typography.titleSmall)
                        Text("${if (transfer.direction == "phone_to_laptop") "Sent" else "Received"} · ${transfer.state} · ${transfer.sizeBytes} bytes · ${dateLabel(transfer.createdAt)}")
                        transfer.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                        if (canRetryHandoffUpload(transfer, state.handoff.pendingOutgoingTransferId)) TextButton(
                            onClick = { model.retryUpload(transfer.transferId) },
                            enabled = state.handoff.connected && state.handoff.handoffAllowed && !state.handoff.busy,
                            modifier = Modifier.heightIn(min = 48.dp)) { Text("Retry upload") }
                        if (transfer.direction == "laptop_to_phone" && transfer.interrupted && transfer.available &&
                            transfer.state in setOf("offered", "ready", "transferring") && peerId != null) {
                            Text("Retry saves to a fresh destination.", style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = {
                                receiveTransferId = transfer.transferId; receivePeerId = peerId
                                createDocument.launch(transfer.displayName)
                            }, enabled = state.handoff.connected && state.handoff.handoffAllowed && !state.handoff.busy,
                            modifier = Modifier.heightIn(min = 48.dp)) { Text("Choose destination and retry") }
                        }
                    }
                }
            }
            if (state.handoff.listsTruncated) Text("Some older laptop records are hidden.", color = MaterialTheme.colorScheme.onSurfaceVariant)

            Text("Workspace sessions", style = MaterialTheme.typography.titleMedium)
            if (state.handoff.workspaces.isEmpty()) Text("No workspace sessions are shared.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            state.handoff.workspaces.forEach { workspace ->
                Card(Modifier.fillMaxWidth(), shape = RikoShapes.asymB) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(workspace.title, style = MaterialTheme.typography.titleSmall)
                        Text("Created ${dateLabel(workspace.createdAt)} · ${if (workspace.transferAvailable) "files available" else "metadata only"}")
                        workspace.task?.let { task ->
                            Text("Task: ${task.title}")
                            Text("Sync ID ${task.syncId} · updated ${dateLabel(task.updatedAt)}${task.deadline?.let { " · deadline ${dateLabel(it)}" } ?: ""}", style = MaterialTheme.typography.bodySmall)
                            Text("Fingerprint ${task.fingerprint}", style = MaterialTheme.typography.bodySmall)
                        }
                        workspace.materials.forEach { material ->
                            Text("${material.title}: ${material.reference}${material.transferId?.let { " · transfer $it" } ?: ""}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (workspace.transferIds.isNotEmpty()) Text("Transfers: ${workspace.transferIds.joinToString()}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } else if (inSpace) {
            UnpairedDeviceSections(state.handoff.history, state.companion.networkReady)
        }
    }
    if (inSpace) {
        Column(Modifier.fillMaxSize().background(SpacesTokens.canvas).testTag("screen-DevicesSpace")) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.space5, vertical = Spacing.space4),
                verticalArrangement = Arrangement.spacedBy(Spacing.space5),
            ) {
                SpacesPageHeader(
                    title = stringResource(R.string.spaces_devices),
                    subtitle = stringResource(R.string.devices_space_subtitle),
                    onBack = { onNavigate(Destination.Spaces) },
                    trailing = { TextButton(onClick = { onNavigate(Destination.Sync) },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("devices-open-sync")) {
                        Text(stringResource(R.string.spaces_open_sync))
                    } },
                )
                pageContent()
                Spacer(Modifier.height(Spacing.space5))
            }
        }
    } else {
        ScreenFrame(Destination.Sync, pageContent)
    }
    if (pairingTarget != null) AlertDialog(
        onDismissRequest = { pairingTarget = null; payload = "" }, title = { Text(stringResource(R.string.companion_code_title)) },
        text = { OutlinedTextField(value = payload, onValueChange = { payload = it.take(256) }, label = { Text(stringResource(R.string.companion_code_hint)) }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false)) },
        confirmButton = { TextButton(onClick = { pairingTarget?.let { model.pair(it, payload) }; pairingTarget = null; payload = "" }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.companion_pair)) } },
        dismissButton = { TextButton(onClick = { pairingTarget = null; payload = "" }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.companion_cancel)) } })
    if (state.forgetConfirmation) AlertDialog(onDismissRequest = model::cancelForget, title = { Text(stringResource(R.string.companion_forget)) }, text = { Text(stringResource(R.string.companion_forget_confirm)) },
        confirmButton = { TextButton(onClick = model::forget, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.companion_confirm)) } }, dismissButton = { TextButton(onClick = model::cancelForget, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.companion_cancel)) } })
    if (state.resetConfirmation) AlertDialog(onDismissRequest = model::cancelResetTrust, title = { Text(stringResource(R.string.companion_reset_trust)) }, text = { Text(stringResource(R.string.companion_reset_confirm)) },
        confirmButton = { TextButton(onClick = model::resetLocalTrust, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.companion_reset_confirm_button)) } }, dismissButton = { TextButton(onClick = model::cancelResetTrust, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.companion_cancel)) } })
    if (state.clearHandoffConfirmation) AlertDialog(onDismissRequest = model::cancelClearHandoff, title = { Text("Clear local handoff storage?") },
        text = { Text("This permanently removes staged files and local transfer history from this phone.") },
        confirmButton = { TextButton(onClick = model::clearHandoffStorage, enabled = !state.handoff.busy, modifier = Modifier.heightIn(min = 48.dp)) { Text("Clear local files and history") } },
        dismissButton = { TextButton(onClick = model::cancelClearHandoff, modifier = Modifier.heightIn(min = 48.dp)) { Text("Keep storage") } })
}

@Composable
private fun UnpairedDeviceSections(history: List<id.riko.hub.data.remote.device.HandoffTransfer>, networkReady: Boolean) {
    val text = MaterialTheme.colorScheme.onSurfaceVariant
    SpacesSectionHeader(stringResource(R.string.devices_handoff_queue), R.drawable.ic_dock_plan)
    DeviceInfoCard(stringResource(if (networkReady) R.string.devices_pair_to_load_handoff else R.string.renaissance_devices_queue_offline))
    SpacesSectionHeader(stringResource(R.string.devices_quick_send), R.drawable.ic_dock_capture)
    DeviceInfoCard(stringResource(R.string.devices_pair_for_handoff))
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SpacesTokens.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, SpacesTokens.edge)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.devices_clipboard_concept), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.devices_clipboard_future), color = text, style = MaterialTheme.typography.bodySmall)
        }
    }
    SpacesSectionHeader(stringResource(R.string.devices_trusted_devices), R.drawable.ic_smartphone)
    DeviceInfoCard(stringResource(R.string.devices_no_trusted_devices))
    Text(stringResource(R.string.devices_same_network_only), color = text, style = MaterialTheme.typography.bodySmall)
    SpacesSectionHeader(stringResource(R.string.devices_recent_activity), R.drawable.ic_event)
    if (history.isEmpty()) DeviceInfoCard(stringResource(R.string.devices_no_recent_activity))
    history.forEach { transfer ->
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SpacesTokens.surface)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(transfer.displayName, style = MaterialTheme.typography.titleSmall)
                Text("${transfer.state} · ${transfer.sizeBytes} bytes · ${dateLabel(transfer.createdAt)}",
                    color = text, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun DeviceInfoCard(message: String) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SpacesTokens.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, SpacesTokens.edge)) {
        Text(message, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium)
    }
}

private fun dateLabel(epochMillis: Long): String = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(epochMillis))
