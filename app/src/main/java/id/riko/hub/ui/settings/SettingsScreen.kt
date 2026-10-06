package id.riko.hub.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import id.riko.hub.R
import id.riko.hub.navigation.Destination
import id.riko.hub.ui.components.ScreenFrame
import id.riko.hub.ui.theme.*

@Composable
fun SettingsScreen(mode: ThemeMode, onTheme: (ThemeMode) -> Unit, onNavigate: (Destination) -> Unit) {
    ScreenFrame(Destination.Settings, onNavigate) {
        Text(stringResource(R.string.theme), style = MaterialTheme.typography.titleLarge)
        Column(Modifier.selectableGroup()) {
            ThemeMode.entries.forEach { option ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = Spacing.row)
                        .selectable(selected = option == mode, role = Role.RadioButton, onClick = { onTheme(option) }),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.space3),
                ) {
                    RadioButton(selected = option == mode, onClick = null,
                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.onSurface))
                    Text(stringResource(when (option) {
                        ThemeMode.SYSTEM -> R.string.theme_system
                        ThemeMode.LIGHT -> R.string.theme_light
                        ThemeMode.DARK -> R.string.theme_dark
                    }))
                }
            }
        }
        Text(stringResource(R.string.theme_temporary), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(R.string.local_data), style = MaterialTheme.typography.bodyMedium)
    }
}
// TODO(v0.1): Add display name/navigation preferences in Phase 6.
// TODO(v0.2): Add deliberate manual export/import; automatic backup remains disabled.
