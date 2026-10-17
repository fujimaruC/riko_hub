package id.riko.hub.ui.sync

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import id.riko.hub.navigation.Destination
import id.riko.hub.ui.components.ScreenFrame
import id.riko.hub.ui.components.rikoFormFieldColors
import id.riko.hub.ui.spaces.DevicesConnectionBridge
import id.riko.hub.ui.spaces.SpacesPageHeader
import id.riko.hub.ui.spaces.SpacesSectionHeader
import id.riko.hub.ui.spaces.deviceTransferProgress
import id.riko.hub.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
            SyncStatusCard(connected = state.companion.connected, scanning = state.companion.scanning,
                trustedName = state.companion.trusted?.name)
            TextButton(onClick = { onNavigate(Destination.DevicesSpace) }, modifier = Modifier.heightIn(min = 44.dp)) {
                Text(stringResource(R.string.devices_back_to_space), color = ChefLime, fontWeight = FontWeight.Bold)
            }
        }
        if (localPermission != null && ContextCompat.checkSelfPermission(context, localPermission) != PackageManager.PERMISSION_GRANTED) {
            Text(stringResource(R.string.companion_permission_denied), color = ChefCoral)
            TextButton(onClick = { permissionAsked = true; permissionLauncher.launch(localPermission) }, modifier = Modifier.heightIn(min = 44.dp)) { Text(stringResource(R.string.companion_retry_permission), color = ChefLime) }
        }
        if (!inSpace) state.companion.trusted?.let { trusted ->
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = ChefPanel, border = BorderStroke(1.dp, ChefLine)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(trusted.name, style = MaterialTheme.typography.titleMedium, color = ChefInk, fontWeight = FontWeight.Bold)
                    Text(if (state.companion.connected) stringResource(R.string.companion_connected) else stringResource(R.string.companion_waiting), color = ChefMuted)
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (state.companion.connected) OutlinedButton(onClick = model::disconnect, modifier = Modifier.heightIn(min = 44.dp), shape = RoundedCornerShape(999.dp), border = BorderStroke(1.dp, ChefLine)) { Text(stringResource(R.string.companion_disconnect), color = ChefInk) }
                        else Button(onClick = model::connect, enabled = !state.companion.busy, modifier = Modifier.heightIn(min = 44.dp), shape = RoundedCornerShape(999.dp), colors = ButtonDefaults.buttonColors(containerColor = ChefLime, contentColor = ChefGround)) { Text(stringResource(R.string.companion_connect), fontWeight = FontWeight.Bold) }
                        TextButton(onClick = model::requestForget, enabled = !state.companion.busy, modifier = Modifier.heightIn(min = 44.dp)) { Text(stringResource(R.string.companion_forget), color = ChefMuted) }
                    }
                }
            }
        }
        Text(
            stringResource(R.string.companion_nearby),
            fontFamily = NotoCondensedBlack,
            fontWeight = FontWeight.Black,
            fontSize = 16.sp,
            color = ChefInk,
        )
        OutlinedButton(
            onClick = model::refresh,
            enabled = state.companion.networkReady && !state.companion.busy,
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(1.dp, ChefLine),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ChefInk),
            modifier = Modifier.heightIn(min = 44.dp),
        ) {
            Text(stringResource(R.string.companion_refresh), fontWeight = FontWeight.Bold)
        }
        if (state.companion.pairing) Text(stringResource(R.string.companion_pairing_progress), color = ChefMuted)
        if (state.companion.laptops.isEmpty()) Text(
            when { !state.companion.networkReady -> stringResource(R.string.companion_no_wifi); state.companion.scanning -> stringResource(R.string.companion_searching); else -> stringResource(R.string.companion_empty) },
            color = ChefMuted)
        state.companion.laptops.forEach { laptop ->
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = ChefPanel, border = BorderStroke(1.dp, ChefLine)) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(laptop.name, style = MaterialTheme.typography.titleSmall, color = ChefInk, fontWeight = FontWeight.Bold)
                        Text(laptop.address.hostAddress.orEmpty(), style = MaterialTheme.typography.bodySmall, color = ChefMuted)
                    }
                    Button(
                        enabled = state.companion.trusted == null && !state.companion.busy,
                        onClick = { payload = ""; pairingTarget = laptop },
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ChefLime, contentColor = ChefGround),
                        modifier = Modifier.heightIn(min = 40.dp),
                    ) {
                        Text(stringResource(R.string.companion_pair), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        state.companion.error?.let { message ->
            if (!inSpace) Text(message, color = ChefCoral, style = MaterialTheme.typography.bodySmall)
            if (message.startsWith("Penyimpanan kepercayaan")) TextButton(onClick = model::requestResetTrust, modifier = Modifier.heightIn(min = 44.dp)) { Text(stringResource(R.string.companion_reset_trust), color = ChefLime) }
        }

        if (state.companion.trusted != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                "FILE DAN TEKS",
                fontFamily = NotoCondensedBlack,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = ChefInk,
            )
            Text("Transfer menggunakan laptop tepercaya. Pilih file atau teks yang akan dikirim.", color = ChefMuted, style = MaterialTheme.typography.bodySmall)
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(onClick = { model.refreshHandoff() }, enabled = state.handoff.connected && !state.handoff.busy, modifier = Modifier.heightIn(min = 44.dp), shape = RoundedCornerShape(999.dp), colors = ButtonDefaults.buttonColors(containerColor = ChefLime, contentColor = ChefGround)) { Text("Perbarui status", fontWeight = FontWeight.Bold) }
                TextButton(onClick = model::requestClearHandoff, enabled = !state.handoff.busy, modifier = Modifier.heightIn(min = 44.dp)) { Text("Hapus data transfer lokal", color = ChefMuted) }
            }
            if (!state.handoff.connected) Text("Hubungkan laptop tepercaya untuk menggunakan transfer.", color = ChefMuted, style = MaterialTheme.typography.bodySmall)
            else if (!state.handoff.supported) Text("Perbarui status untuk memeriksa dukungan laptop.", color = ChefMuted, style = MaterialTheme.typography.bodySmall)
            else if (!state.handoff.handoffAllowed) Text("Aktifkan handoff untuk ponsel ini di aplikasi Companion pada laptop.", color = ChefMuted, style = MaterialTheme.typography.bodySmall)
            if (state.handoff.busy) {
                state.handoff.progress?.let { progress ->
                    Text("${progress.displayName}: ${progress.completedBytes} / ${progress.totalBytes} byte", color = ChefInk, style = MaterialTheme.typography.bodySmall)
                    if (progress.totalBytes > 0) {
                        LinearProgressIndicator(progress = { deviceTransferProgress(progress) ?: 0f }, modifier = Modifier.fillMaxWidth(), color = ChefLime)
                    } else LinearProgressIndicator(Modifier.fillMaxWidth(), color = ChefLime)
                } ?: LinearProgressIndicator(Modifier.fillMaxWidth(), color = ChefLime)
                TextButton(onClick = model::cancelTransfer, modifier = Modifier.heightIn(min = 44.dp)) { Text("Batalkan transfer", color = ChefCoral) }
            }
            if (!inSpace) state.handoff.error?.let { Text(it, color = ChefCoral, style = MaterialTheme.typography.bodySmall) }

            Text("Kirim file", fontFamily = NotoCondensedBlack, fontWeight = FontWeight.Black, fontSize = 16.sp, color = ChefInk)
            state.handoff.preparedFile?.let { prepared ->
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = ChefPanel, border = BorderStroke(1.dp, ChefLine)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Tinjau file sebelum dikirim", style = MaterialTheme.typography.titleSmall, color = ChefInk, fontWeight = FontWeight.Bold)
                        Text(prepared.displayName, color = ChefInk)
                        Text("${prepared.mimeType} · ${prepared.sizeBytes} byte · SHA-256 ${prepared.sha256}", color = ChefMuted, style = MaterialTheme.typography.bodySmall)
                        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(onClick = model::sendPrepared, enabled = state.handoff.connected && state.handoff.handoffAllowed && !state.handoff.busy, modifier = Modifier.heightIn(min = 44.dp), shape = RoundedCornerShape(999.dp), colors = ButtonDefaults.buttonColors(containerColor = ChefLime, contentColor = ChefGround)) { Text("Kirim ke laptop", fontWeight = FontWeight.Bold) }
                            TextButton(onClick = model::discardPrepared, enabled = !state.handoff.busy, modifier = Modifier.heightIn(min = 44.dp)) { Text("Batalkan file", color = ChefMuted) }
                        }
                    }
                }
            }
            OutlinedButton(onClick = { openDocument.launch(arrayOf("*/*")) }, enabled = state.handoff.connected && state.handoff.handoffAllowed && !state.handoff.busy, modifier = Modifier.heightIn(min = 44.dp), shape = RoundedCornerShape(999.dp), border = BorderStroke(1.dp, ChefLine)) { Text("Pilih file untuk ditinjau", color = ChefInk) }

            Text("File dari laptop", fontFamily = NotoCondensedBlack, fontWeight = FontWeight.Black, fontSize = 16.sp, color = ChefInk)
            val peerId = state.companion.trusted?.id
            val incomingOffers = state.handoff.offers.filter { it.transfer.direction == "laptop_to_phone" }
            if (incomingOffers.isEmpty()) Text("Tidak ada file dari laptop yang menunggu persetujuan.", color = ChefMuted, style = MaterialTheme.typography.bodySmall)
            incomingOffers.forEach { offer ->
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = ChefPanel, border = BorderStroke(1.dp, ChefLine)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(offer.transfer.displayName, style = MaterialTheme.typography.titleSmall, color = ChefInk, fontWeight = FontWeight.Bold)
                        Text("${offer.transfer.mimeType} · ${offer.transfer.sizeBytes} byte", color = ChefMuted, style = MaterialTheme.typography.bodySmall)
                        Text("File akan disimpan sebagai berkas baru setelah kamu memilih folder tujuan.", color = ChefMuted, style = MaterialTheme.typography.bodySmall)
                        Button(onClick = {
                            if (peerId != null) {
                                receiveTransferId = offer.transfer.transferId; receivePeerId = peerId
                                createDocument.launch(offer.transfer.displayName)
                            }
                        }, enabled = peerId != null && !state.handoff.busy, modifier = Modifier.heightIn(min = 44.dp), shape = RoundedCornerShape(999.dp), colors = ButtonDefaults.buttonColors(containerColor = ChefLime, contentColor = ChefGround)) { Text("Pilih folder dan terima", fontWeight = FontWeight.Bold) }
                    }
                }
            }

            Text("Kirim teks", fontFamily = NotoCondensedBlack, fontWeight = FontWeight.Black, fontSize = 16.sp, color = ChefInk)
            OutlinedTextField(
                value = textDraft,
                onValueChange = { textDraft = it.take(2048) },
                label = { Text("Teks", color = ChefMuted) },
                colors = rikoFormFieldColors(),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(onClick = { model.sendText(textDraft) { textDraft = "" } }, enabled = state.handoff.connected && state.handoff.handoffAllowed && !state.handoff.busy && textDraft.isNotBlank(), modifier = Modifier.heightIn(min = 44.dp), shape = RoundedCornerShape(999.dp), colors = ButtonDefaults.buttonColors(containerColor = ChefLime, contentColor = ChefGround)) { Text("Kirim teks", fontWeight = FontWeight.Bold) }
            }

            Text("Ruang kerja aktif", fontFamily = NotoCondensedBlack, fontWeight = FontWeight.Black, fontSize = 16.sp, color = ChefInk)
            state.handoff.workspaces.forEach { workspace ->
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = ChefPanel, border = BorderStroke(1.dp, ChefLine)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(workspace.title, style = MaterialTheme.typography.titleSmall, color = ChefInk, fontWeight = FontWeight.Bold)
                        Text("Dibuat ${dateLabel(workspace.createdAt)} · ${if (workspace.transferAvailable) "file tersedia" else "metadata saja"}", color = ChefMuted, style = MaterialTheme.typography.bodySmall)
                        workspace.task?.let { task ->
                            Text("Tugas: ${task.title}", color = ChefInk)
                            Text("ID sinkronisasi ${task.syncId} · diperbarui ${dateLabel(task.updatedAt)}${task.deadline?.let { " · tenggat ${dateLabel(it)}" } ?: ""}", style = MaterialTheme.typography.bodySmall, color = ChefMuted)
                            Text("Fingerprint ${task.fingerprint}", style = MaterialTheme.typography.bodySmall, color = ChefMuted)
                        }
                        workspace.materials.forEach { material ->
                            Text("${material.title}: ${material.reference}${material.transferId?.let { " · transfer $it" } ?: ""}", style = MaterialTheme.typography.bodySmall, color = ChefMuted)
                        }
                        if (workspace.transferIds.isNotEmpty()) Text("ID transfer: ${workspace.transferIds.joinToString()}", style = MaterialTheme.typography.bodySmall, color = ChefMuted)
                    }
                }
            }
        } else if (inSpace) {
            UnpairedDeviceSections(state.handoff.history, state.companion.networkReady)
        }
    }
    if (inSpace) {
        Column(Modifier.fillMaxSize().background(ChefCanvas).testTag("screen-DevicesSpace")) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SpacesPageHeader(
                    title = stringResource(R.string.spaces_devices),
                    subtitle = stringResource(R.string.devices_space_subtitle),
                    onBack = { onNavigate(Destination.Spaces) },
                    trailing = { TextButton(onClick = { onNavigate(Destination.Sync) },
                        modifier = Modifier.heightIn(min = 44.dp).testTag("devices-open-sync")) {
                        Text(stringResource(R.string.spaces_open_sync), color = ChefLime, fontWeight = FontWeight.Bold)
                    } },
                )
                pageContent()
                Spacer(Modifier.height(24.dp))
            }
        }
    } else {
        ScreenFrame(Destination.Sync, pageContent)
    }
    if (pairingTarget != null) AlertDialog(
        onDismissRequest = { pairingTarget = null; payload = "" },
        containerColor = ChefPanel,
        title = { Text(stringResource(R.string.companion_code_title), color = ChefInk) },
        text = { OutlinedTextField(value = payload, onValueChange = { payload = it.take(256) }, label = { Text(stringResource(R.string.companion_code_hint), color = ChefMuted) },
            colors = rikoFormFieldColors(), singleLine = true, shape = RoundedCornerShape(14.dp),
            visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false)) },
        confirmButton = { TextButton(onClick = { pairingTarget?.let { model.pair(it, payload) }; pairingTarget = null; payload = "" }, modifier = Modifier.heightIn(min = 44.dp)) { Text(stringResource(R.string.companion_pair), color = ChefLime, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = { pairingTarget = null; payload = "" }, modifier = Modifier.heightIn(min = 44.dp)) { Text(stringResource(R.string.companion_cancel), color = ChefMuted) } })
    if (state.forgetConfirmation) AlertDialog(
        onDismissRequest = model::cancelForget,
        containerColor = ChefPanel,
        title = { Text(stringResource(R.string.companion_forget), color = ChefInk) },
        text = { Text(stringResource(R.string.companion_forget_confirm), color = ChefMuted) },
        confirmButton = { TextButton(onClick = model::forget, modifier = Modifier.heightIn(min = 44.dp)) { Text(stringResource(R.string.companion_confirm), color = ChefCoral) } },
        dismissButton = { TextButton(onClick = model::cancelForget, modifier = Modifier.heightIn(min = 44.dp)) { Text(stringResource(R.string.companion_cancel), color = ChefMuted) } })
    if (state.resetConfirmation) AlertDialog(
        onDismissRequest = model::cancelResetTrust,
        containerColor = ChefPanel,
        title = { Text(stringResource(R.string.companion_reset_trust), color = ChefInk) },
        text = { Text(stringResource(R.string.companion_reset_confirm), color = ChefMuted) },
        confirmButton = { TextButton(onClick = model::resetLocalTrust, modifier = Modifier.heightIn(min = 44.dp)) { Text(stringResource(R.string.companion_reset_confirm_button), color = ChefCoral) } },
        dismissButton = { TextButton(onClick = model::cancelResetTrust, modifier = Modifier.heightIn(min = 44.dp)) { Text(stringResource(R.string.companion_cancel), color = ChefMuted) } })
    if (state.clearHandoffConfirmation) AlertDialog(
        onDismissRequest = model::cancelClearHandoff,
        containerColor = ChefPanel,
        title = { Text("Hapus data transfer lokal?", color = ChefInk) },
        text = { Text("File sementara dan riwayat transfer di ponsel ini akan dihapus permanen.", color = ChefMuted) },
        confirmButton = { TextButton(onClick = model::clearHandoffStorage, enabled = !state.handoff.busy, modifier = Modifier.heightIn(min = 44.dp)) { Text("Hapus file dan riwayat", color = ChefCoral) } },
        dismissButton = { TextButton(onClick = model::cancelClearHandoff, modifier = Modifier.heightIn(min = 44.dp)) { Text("Batal", color = ChefMuted) } })
}

@Composable
private fun SyncStatusCard(connected: Boolean, scanning: Boolean, trustedName: String?) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = ChefPanel,
        border = BorderStroke(1.dp, ChefLine)
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("STATUS PERANGKAT", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = ChefMuted)
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (connected) ChefLime.copy(alpha = 0.2f) else ChefRaised,
                    border = BorderStroke(1.dp, if (connected) ChefLime else ChefLine)
                ) {
                    Text(if (connected) "TERHUBUNG" else if (scanning) "MENCARI" else "OFFLINE",
                        Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (connected) ChefLime else ChefMuted)
                }
            }
            Text("KIRIM\nANTAR RUANG.", fontFamily = NotoCondensedBlack, fontWeight = FontWeight.Black, fontSize = 28.sp, color = ChefInk,
                modifier = Modifier.semantics { heading() })
            Text(trustedName ?: "Mulai dengan memasangkan perangkat di jaringan yang sama.",
                style = MaterialTheme.typography.bodyMedium, color = ChefMuted)
        }
    }
}

@Composable
private fun UnpairedDeviceSections(history: List<id.riko.hub.data.remote.device.HandoffTransfer>, networkReady: Boolean) {
    SpacesSectionHeader(stringResource(R.string.devices_handoff_queue), R.drawable.ic_dock_plan)
    DeviceInfoCard(stringResource(if (networkReady) R.string.devices_pair_to_load_handoff else R.string.renaissance_devices_queue_offline))
    SpacesSectionHeader(stringResource(R.string.devices_quick_send), R.drawable.ic_dock_capture)
    DeviceInfoCard(stringResource(R.string.devices_pair_for_handoff))
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = ChefPanel,
        border = BorderStroke(1.dp, ChefLine)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.devices_clipboard_concept), style = MaterialTheme.typography.titleSmall, color = ChefInk, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.devices_clipboard_future), color = ChefMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
    SpacesSectionHeader(stringResource(R.string.devices_trusted_devices), R.drawable.ic_smartphone)
    DeviceInfoCard(stringResource(R.string.devices_no_trusted_devices))
    Text(stringResource(R.string.devices_same_network_only), color = ChefMuted, style = MaterialTheme.typography.bodySmall)
    SpacesSectionHeader(stringResource(R.string.devices_recent_activity), R.drawable.ic_event)
    if (history.isEmpty()) DeviceInfoCard(stringResource(R.string.devices_no_recent_activity))
    history.forEach { transfer ->
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = ChefPanel,
            border = BorderStroke(1.dp, ChefLine)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(transfer.displayName, style = MaterialTheme.typography.titleSmall, color = ChefInk, fontWeight = FontWeight.Bold)
                Text("${transfer.state} · ${transfer.sizeBytes} bytes · ${dateLabel(transfer.createdAt)}",
                    color = ChefMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun DeviceInfoCard(message: String) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = ChefPanel,
        border = BorderStroke(1.dp, ChefLine)) {
        Text(message, Modifier.padding(16.dp), color = ChefMuted,
            style = MaterialTheme.typography.bodyMedium)
    }
}

private fun dateLabel(epochMillis: Long): String = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(epochMillis))
