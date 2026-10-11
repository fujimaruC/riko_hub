package id.riko.hub.ui.sync

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
import id.riko.hub.navigation.Destination
import id.riko.hub.ui.components.ScreenFrame
import id.riko.hub.ui.theme.RikoShapes

private const val LOCAL_NETWORK_PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"

@Composable
fun SyncScreen(onNavigate: (Destination) -> Unit) {
    val app = LocalContext.current.applicationContext as RikoHubApp
    val model: SyncViewModel = viewModel(factory = viewModelFactory { initializer { SyncViewModel(app.container.companion) } })
    SyncRoute(model, onNavigate)
}

@Composable
private fun SyncRoute(model: SyncViewModel, onNavigate: (Destination) -> Unit) {
    val context = LocalContext.current
    val state by model.state.collectAsStateWithLifecycle(SyncUiState())
    var pairingTarget by remember { mutableStateOf<Laptop?>(null) }
    var payload by remember { mutableStateOf("") }
    var permissionAsked by remember { mutableStateOf(false) }
    val localPermission = if (Build.VERSION.SDK_INT >= 37) LOCAL_NETWORK_PERMISSION else null
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) model.setActive(true)
    }
    DisposableEffect(lifecycle, model) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (localPermission == null || ContextCompat.checkSelfPermission(context, localPermission) == PackageManager.PERMISSION_GRANTED) model.setActive(true)
                else if (!permissionAsked) { permissionAsked = true; permissionLauncher.launch(localPermission) }
                model.load()
            } else if (event == Lifecycle.Event.ON_PAUSE) { payload = ""; pairingTarget = null; model.setActive(false) }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            if (localPermission == null || ContextCompat.checkSelfPermission(context, localPermission) == PackageManager.PERMISSION_GRANTED) model.setActive(true)
            else if (!permissionAsked) { permissionAsked = true; permissionLauncher.launch(localPermission) }
            model.load()
        }
        onDispose { lifecycle.removeObserver(observer); model.setActive(false) }
    }
    ScreenFrame(Destination.Sync, onNavigate) {
        Text(stringResource(R.string.companion_title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.companion_intro), color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (localPermission != null && ContextCompat.checkSelfPermission(context, localPermission) != PackageManager.PERMISSION_GRANTED) {
            Text(stringResource(R.string.companion_permission_denied), color = MaterialTheme.colorScheme.error)
            TextButton(onClick = { permissionAsked = true; permissionLauncher.launch(localPermission) }) { Text(stringResource(R.string.companion_retry_permission)) }
        }
        state.companion.trusted?.let { trusted ->
            Card(Modifier.fillMaxWidth(), shape = RikoShapes.asymB) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(trusted.name, style = MaterialTheme.typography.titleMedium)
                    Text(if (state.companion.connected) stringResource(R.string.companion_connected) else stringResource(R.string.companion_waiting),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (state.companion.connected) OutlinedButton(onClick = model::disconnect) { Text(stringResource(R.string.companion_disconnect)) }
                        else Button(onClick = model::connect, enabled = !state.companion.busy) { Text(stringResource(R.string.companion_connect)) }
                        TextButton(onClick = model::requestForget, enabled = !state.companion.busy) { Text(stringResource(R.string.companion_forget)) }
                    }
                }
            }
        }
        Text(stringResource(R.string.companion_nearby), style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = model::refresh, enabled = state.companion.networkReady && !state.companion.busy) { Text(stringResource(R.string.companion_refresh)) }
        if (state.companion.pairing) Text(stringResource(R.string.companion_pairing_progress), color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (state.companion.laptops.isEmpty()) Text(
            when { !state.companion.networkReady -> stringResource(R.string.companion_no_wifi); state.companion.scanning -> stringResource(R.string.companion_searching); else -> stringResource(R.string.companion_empty) },
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        state.companion.laptops.forEach { laptop ->
            Card(Modifier.fillMaxWidth(), shape = RikoShapes.asymB) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) { Text(laptop.name, style = MaterialTheme.typography.titleSmall); Text(laptop.address.hostAddress.orEmpty(), style = MaterialTheme.typography.bodySmall) }
                    TextButton(enabled = state.companion.trusted == null && !state.companion.busy,
                        onClick = { payload = ""; pairingTarget = laptop }) { Text(stringResource(R.string.companion_pair)) }
                }
            }
        }
        state.companion.error?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            if (message.startsWith("Penyimpanan kepercayaan")) TextButton(onClick = model::requestResetTrust) { Text(stringResource(R.string.companion_reset_trust)) }
        }
    }
    if (pairingTarget != null) AlertDialog(
        onDismissRequest = { pairingTarget = null; payload = "" },
        title = { Text(stringResource(R.string.companion_code_title)) },
        text = { OutlinedTextField(value = payload, onValueChange = { payload = it.take(256) },
            label = { Text(stringResource(R.string.companion_code_hint)) }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false)) },
        confirmButton = { TextButton(onClick = { pairingTarget?.let { model.pair(it, payload) }; pairingTarget = null; payload = "" }) { Text(stringResource(R.string.companion_pair)) } },
        dismissButton = { TextButton(onClick = { pairingTarget = null; payload = "" }) { Text(stringResource(R.string.companion_cancel)) } })
    if (state.forgetConfirmation) AlertDialog(
        onDismissRequest = model::cancelForget,
        title = { Text(stringResource(R.string.companion_forget)) },
        text = { Text(stringResource(R.string.companion_forget_confirm)) },
        confirmButton = { TextButton(onClick = model::forget) { Text(stringResource(R.string.companion_confirm)) } },
        dismissButton = { TextButton(onClick = model::cancelForget) { Text(stringResource(R.string.companion_cancel)) } })
    if (state.resetConfirmation) AlertDialog(
        onDismissRequest = model::cancelResetTrust,
        title = { Text(stringResource(R.string.companion_reset_trust)) },
        text = { Text(stringResource(R.string.companion_reset_confirm)) },
        confirmButton = { TextButton(onClick = model::resetLocalTrust) { Text(stringResource(R.string.companion_reset_confirm_button)) } },
        dismissButton = { TextButton(onClick = model::cancelResetTrust) { Text(stringResource(R.string.companion_cancel)) } })
}
