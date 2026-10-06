package id.riko.hub.ui.sync

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import id.riko.hub.R
import id.riko.hub.navigation.Destination
import id.riko.hub.ui.components.ScreenFrame
import id.riko.hub.ui.theme.*

@Composable
fun SyncScreen(onNavigate: (Destination) -> Unit) {
    ScreenFrame(Destination.Sync, onNavigate) {
        DeviceMap()
        Surface(shape = RikoShapes.asymB, color = MaterialTheme.colorScheme.surfaceVariant) {
            Text(stringResource(R.string.sync_placeholder), Modifier.padding(Spacing.space5), style = MaterialTheme.typography.bodyLarge)
        }
        listOf(R.string.send_files, R.string.clipboard, R.string.transfer_history).forEach {
            Column {
                Text(stringResource(it), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.coming_soon), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DeviceMap() {
    val line = MaterialTheme.colorScheme.outline
    Box(Modifier.fillMaxWidth()) {
        Canvas(Modifier.matchParentSize()) {
            val top = Offset(size.width / 2, size.height * 0.25f)
            val left = Offset(size.width * 0.22f, size.height * 0.78f)
            val right = Offset(size.width * 0.78f, size.height * 0.78f)
            val dash = PathEffect.dashPathEffect(floatArrayOf(Spacing.space2.toPx(), Spacing.space2.toPx()))
            listOf(top to left, left to right, right to top).forEach { (a, b) ->
                drawLine(line, a, b, Spacing.pathStroke.toPx(), pathEffect = dash)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.space8)) {
            DeviceNode(R.string.drive, R.string.not_ready, Modifier.fillMaxWidth(0.6f))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space6)) {
                DeviceNode(R.string.phone, R.string.this_device, Modifier.weight(1f))
                DeviceNode(R.string.laptop, R.string.not_ready, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DeviceNode(label: Int, status: Int, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
        Surface(Modifier.size(Spacing.row), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
            Box(contentAlignment = Alignment.Center) {
                Icon(painterResource(when (label) {
                    R.string.drive -> R.drawable.ic_cloud
                    R.string.phone -> R.drawable.ic_smartphone
                    else -> R.drawable.ic_laptop
                }), contentDescription = null, modifier = Modifier.size(Spacing.space6))
            }
        }
        Text(stringResource(label), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(status), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
// TODO(v0.4): Add laptop pairing and real device presence.
// TODO(v0.5): Add transfer, clipboard and sync history after pairing exists.
