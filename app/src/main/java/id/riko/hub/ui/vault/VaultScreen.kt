package id.riko.hub.ui.vault

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import id.riko.hub.R
import id.riko.hub.navigation.Destination
import id.riko.hub.ui.components.*
import id.riko.hub.ui.theme.*

@Composable
fun VaultScreen(onNavigate: (Destination) -> Unit, onComingSoon: () -> Unit) {
    ScreenFrame(Destination.Vault, onNavigate) {
        Surface(shape = RikoShapes.asymA, color = MaterialTheme.colorScheme.surfaceVariant) {
            Row(Modifier.fillMaxWidth().padding(Spacing.space5), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.space4)) {
                Icon(painterResource(R.drawable.ic_cloud), contentDescription = null, modifier = Modifier.size(Spacing.space8),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.vault_placeholder), style = MaterialTheme.typography.bodyLarge)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space4)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.space4)) {
                Folder(R.string.folder_academic, onComingSoon)
                Folder(R.string.personal, onComingSoon)
            }
            Column(Modifier.weight(1f).padding(top = Spacing.space8), verticalArrangement = Arrangement.spacedBy(Spacing.space4)) {
                Folder(R.string.folder_projects, onComingSoon)
                Folder(R.string.folder_inbox, onComingSoon)
            }
        }
    }
}

@Composable
private fun Folder(label: Int, onClick: () -> Unit) {
    Column {
        Surface(Modifier.width(Spacing.space12).height(Spacing.space3), shape = RikoShapes.small, color = MaterialTheme.colorScheme.outline) {}
        Surface(onClick = onClick, shape = RikoShapes.tall, color = MaterialTheme.colorScheme.surfaceVariant) {
            Column(Modifier.fillMaxWidth().heightIn(min = Spacing.illustration).padding(Spacing.space4), verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                Text(stringResource(label), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.not_connected), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
// TODO(v0.3): Connect Vault to Google Drive only when that version is authorized.
