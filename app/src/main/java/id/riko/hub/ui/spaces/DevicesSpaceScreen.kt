package id.riko.hub.ui.spaces

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import id.riko.hub.R
import id.riko.hub.data.remote.device.CompanionSnapshot
import id.riko.hub.data.remote.device.HandoffProgress
import id.riko.hub.data.remote.device.HandoffSnapshot
import id.riko.hub.navigation.Destination
import id.riko.hub.ui.sync.SyncScreen
import id.riko.hub.ui.sync.SyncUiState
import id.riko.hub.ui.theme.SpacesTokens
import id.riko.hub.ui.theme.Spacing
import id.riko.hub.ui.theme.rememberReducedMotion

@Composable
fun DevicesSpaceScreen(onNavigate: (Destination) -> Unit) {
    SpacesTheme { SyncScreen(onNavigate, inSpace = true) }
}

enum class DeviceBridgeState { UNPAIRED, OFFLINE, PAIRING, DISCONNECTED, CONNECTED, TRANSFERRING, ERROR }

internal fun deviceBridgeState(companion: CompanionSnapshot, handoff: HandoffSnapshot): DeviceBridgeState = when {
    companion.error != null || handoff.error != null -> DeviceBridgeState.ERROR
    companion.pairing -> DeviceBridgeState.PAIRING
    !companion.networkReady -> DeviceBridgeState.OFFLINE
    companion.trusted == null -> DeviceBridgeState.UNPAIRED
    companion.connected && handoff.connected && handoff.busy && handoff.progress != null -> DeviceBridgeState.TRANSFERRING
    companion.connected -> DeviceBridgeState.CONNECTED
    else -> DeviceBridgeState.DISCONNECTED
}

internal fun deviceTransferProgress(progress: HandoffProgress?): Float? {
    progress ?: return null
    if (progress.totalBytes <= 0L) return null
    return (progress.completedBytes.toDouble() / progress.totalBytes.toDouble()).coerceIn(0.0, 1.0).toFloat()
}

@Composable
fun DevicesConnectionBridge(
    state: SyncUiState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onForget: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val companion = state.companion
    val handoff = state.handoff
    val bridgeState = deviceBridgeState(companion, handoff)
    val connected = bridgeState == DeviceBridgeState.CONNECTED || bridgeState == DeviceBridgeState.TRANSFERRING
    val green = SpacesTokens.connected
    val statusColor = when (bridgeState) {
        DeviceBridgeState.CONNECTED, DeviceBridgeState.TRANSFERRING -> green
        DeviceBridgeState.PAIRING -> SpacesTokens.tangerine
        DeviceBridgeState.ERROR -> SpacesTokens.magenta
        else -> SpacesTokens.secondary
    }
    val status = stringResource(when (bridgeState) {
        DeviceBridgeState.UNPAIRED -> R.string.devices_state_unpaired
        DeviceBridgeState.OFFLINE -> R.string.devices_state_offline
        DeviceBridgeState.PAIRING -> R.string.devices_state_pairing
        DeviceBridgeState.DISCONNECTED -> R.string.devices_state_disconnected
        DeviceBridgeState.CONNECTED -> R.string.devices_state_connected
        DeviceBridgeState.TRANSFERRING -> R.string.devices_state_transferring
        DeviceBridgeState.ERROR -> R.string.devices_state_error
    })
    val thisPhone = stringResource(R.string.devices_this_phone)
    val narrow = LocalConfiguration.current.screenWidthDp < 360 || LocalConfiguration.current.fontScale >= 1.3f
    val borderColor = if (connected) green.copy(alpha = .32f) else SpacesTokens.edge.copy(alpha = .8f)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = SpacesTokens.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
    ) {
        Column(Modifier.fillMaxWidth().padding(Spacing.space4), verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.devices_bridge_title), style = MaterialTheme.typography.titleLarge, color = SpacesTokens.primary)
                    Text(status, style = MaterialTheme.typography.labelLarge, color = statusColor)
                }
                StatusDot(statusColor, connected)
            }
            if (narrow) {
                DevicePanel(
                    iconDescription = stringResource(R.string.devices_phone_icon),
                    name = thisPhone,
                    subtitle = stringResource(R.string.renaissance_devices_android),
                    connected = companion.connected,
                    kind = DeviceKind.Phone,
                    accent = SpacesTokens.tangerine,
                )
                BridgeConnectionLine(bridgeState, handoff.progress, vertical = true)
                LaptopPanel(companion, handoff, connected)
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                    DevicePanel(
                        iconDescription = stringResource(R.string.devices_phone_icon),
                        name = thisPhone,
                        subtitle = stringResource(R.string.renaissance_devices_android),
                        connected = companion.connected,
                        kind = DeviceKind.Phone,
                        accent = SpacesTokens.tangerine,
                        modifier = Modifier.weight(1f),
                    )
                    BridgeConnectionLine(bridgeState, handoff.progress, vertical = false)
                    LaptopPanel(companion, handoff, connected = connected, modifier = Modifier.weight(1f))
                }
            }
            Text(stringResource(R.string.devices_same_network_only), style = MaterialTheme.typography.bodySmall, color = SpacesTokens.secondary)
            val trusted = companion.trusted
            val capability = when {
                trusted == null -> null
                !companion.connected -> stringResource(R.string.devices_handoff_after_connect)
                !handoff.supported -> stringResource(R.string.devices_support_unknown)
                !handoff.handoffAllowed -> stringResource(R.string.devices_handoff_not_enabled)
                else -> stringResource(R.string.devices_handoff_supported)
            }
            capability?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = SpacesTokens.secondary) }
            if (companion.error != null) Text(companion.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            if (handoff.error != null) Text(handoff.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            if (trusted != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                    if (connected) OutlinedButton(onClick = onDisconnect, modifier = Modifier.weight(1f).heightIn(min = Spacing.touch)) {
                        Text(stringResource(R.string.devices_disconnect))
                    } else Button(onClick = onConnect, enabled = companion.networkReady && !companion.busy,
                        modifier = Modifier.weight(1f).heightIn(min = Spacing.touch)) {
                        Text(stringResource(R.string.devices_connect))
                    }
                    TextButton(onClick = onForget, enabled = !companion.busy, modifier = Modifier.heightIn(min = Spacing.touch)) {
                        Text(stringResource(R.string.devices_forget))
                    }
                }
            } else if (companion.laptops.isEmpty()) {
                Text(stringResource(if (companion.networkReady) R.string.devices_search_nearby else R.string.devices_pair_after_network),
                    color = SpacesTokens.secondary)
            }
        }
    }
}

private enum class DeviceKind { Phone, Laptop }

@Composable
private fun LaptopPanel(companion: CompanionSnapshot, handoff: HandoffSnapshot, connected: Boolean, modifier: Modifier = Modifier) {
    val trusted = companion.trusted
    DevicePanel(
        iconDescription = stringResource(R.string.devices_laptop_icon),
        name = trusted?.name ?: stringResource(R.string.devices_no_trusted_laptop),
        subtitle = when {
            trusted == null -> stringResource(R.string.devices_pair_to_trust)
            handoff.supported -> stringResource(R.string.devices_handoff_supported_short)
            else -> stringResource(R.string.devices_trusted_laptop)
        },
        connected = connected,
        kind = DeviceKind.Laptop,
        accent = if (connected) SpacesTokens.connected else SpacesTokens.ultramarine,
        modifier = modifier,
    )
}

@Composable
private fun DevicePanel(
    iconDescription: String,
    name: String,
    subtitle: String,
    connected: Boolean,
    kind: DeviceKind,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    val semantic = stringResource(if (connected) R.string.devices_state_connected else R.string.devices_state_offline)
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(SpacesTokens.row)
            .semantics { contentDescription = "$iconDescription, $name, $subtitle, $semantic" }
            .padding(Spacing.space3),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.space1),
    ) {
        DeviceGlyph(kind, accent, Modifier.size(width = 48.dp, height = 44.dp))
        Text(name, style = MaterialTheme.typography.titleSmall, color = SpacesTokens.primary, maxLines = 2)
        Text(subtitle, style = MaterialTheme.typography.labelSmall, color = SpacesTokens.secondary, maxLines = 2)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatusDot(if (connected) SpacesTokens.connected else SpacesTokens.muted, connected)
            Text(semantic, style = MaterialTheme.typography.labelMedium, color = if (connected) SpacesTokens.connected else SpacesTokens.secondary)
        }
    }
}

@Composable
private fun BridgeConnectionLine(state: DeviceBridgeState, progress: HandoffProgress?, vertical: Boolean) {
    val reducedMotion = rememberReducedMotion()
    val connected = state == DeviceBridgeState.CONNECTED || state == DeviceBridgeState.TRANSFERRING
    val lineColor = when (state) {
        DeviceBridgeState.CONNECTED, DeviceBridgeState.TRANSFERRING -> SpacesTokens.connected
        DeviceBridgeState.PAIRING -> SpacesTokens.tangerine
        DeviceBridgeState.ERROR -> SpacesTokens.magenta
        else -> SpacesTokens.muted
    }
    val line = lineColor.copy(alpha = if (connected) .46f else .62f)
    val fraction = if (state == DeviceBridgeState.TRANSFERRING && !reducedMotion) deviceTransferProgress(progress) else null
    val description = stringResource(R.string.devices_local_bridge)
    Canvas(if (vertical) Modifier.fillMaxWidth().height(44.dp) else Modifier.width(44.dp).height(142.dp)
        .semantics { contentDescription = description }) {
        val start = if (vertical) Offset(size.width / 2, 3.dp.toPx()) else Offset(2.dp.toPx(), size.height / 2)
        val end = if (vertical) Offset(size.width / 2, size.height - 3.dp.toPx()) else Offset(size.width - 2.dp.toPx(), size.height / 2)
        drawLine(line, start, end, 2.dp.toPx(), cap = StrokeCap.Round,
            pathEffect = if (connected) null else PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx())))
        if (fraction != null) {
            val center = Offset(start.x + (end.x - start.x) * fraction, start.y + (end.y - start.y) * fraction)
            drawCircle(SpacesTokens.connected.copy(alpha = .18f), radius = 8.dp.toPx(), center = center)
            drawCircle(SpacesTokens.connected, radius = 3.dp.toPx(), center = center)
        }
    }
}

@Composable
private fun DeviceGlyph(kind: DeviceKind, tint: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Canvas(modifier) {
        val sw = 2.dp.toPx()
        if (kind == DeviceKind.Phone) {
            val left = size.width * .3f
            val right = size.width * .7f
            drawRoundRect(tint, Offset(left, size.height * .05f), androidx.compose.ui.geometry.Size(right - left, size.height * .88f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()), style = Stroke(sw))
            drawLine(tint, Offset(left + 5.dp.toPx(), size.height * .18f), Offset(right - 5.dp.toPx(), size.height * .18f), sw, cap = StrokeCap.Round)
            drawCircle(tint, 1.5.dp.toPx(), Offset(size.width / 2, size.height * .79f))
        } else {
            drawRoundRect(tint, Offset(size.width * .13f, size.height * .08f), androidx.compose.ui.geometry.Size(size.width * .74f, size.height * .63f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()), style = Stroke(sw))
            drawLine(tint, Offset(size.width * .07f, size.height * .83f), Offset(size.width * .93f, size.height * .83f), 3.dp.toPx(), cap = StrokeCap.Round)
            drawLine(tint, Offset(size.width * .22f, size.height * .72f), Offset(size.width * .78f, size.height * .72f), sw, cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun StatusDot(color: androidx.compose.ui.graphics.Color, connected: Boolean) {
    Canvas(Modifier.size(9.dp)) {
        if (connected) drawCircle(color.copy(alpha = .2f), size.minDimension * .5f)
        drawCircle(color, size.minDimension * if (connected) .34f else .28f)
    }
}
