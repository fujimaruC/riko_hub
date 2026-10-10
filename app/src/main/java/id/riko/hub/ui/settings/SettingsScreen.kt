package id.riko.hub.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import id.riko.hub.R
import id.riko.hub.domain.model.AppSettings
import id.riko.hub.domain.model.NavigationStyle
import id.riko.hub.navigation.Destination
import id.riko.hub.ui.components.ScreenFrame
import id.riko.hub.ui.theme.*

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onTheme: (ThemeMode) -> Unit,
    onDisplayName: (String) -> Unit,
    onNavigationStyle: (NavigationStyle) -> Unit,
    onNavigate: (Destination) -> Unit,
    backupContent: @Composable () -> Unit = {},
) {
    var name by rememberSaveable(settings.displayName) { mutableStateOf(settings.displayName) }
    var nameError by remember { mutableStateOf(false) }

    ScreenFrame(Destination.Settings, onNavigate) {
        Text(stringResource(R.string.theme), style = MaterialTheme.typography.titleLarge)
        Column(Modifier.selectableGroup()) {
            ThemeMode.entries.forEach { option ->
                PreferenceRadio(
                    label = stringResource(when (option) {
                        ThemeMode.SYSTEM -> R.string.theme_system
                        ThemeMode.LIGHT -> R.string.theme_light
                        ThemeMode.DARK -> R.string.theme_dark
                    }),
                    selected = option == settings.themeMode,
                    testTag = "setting-theme-${option.name}",
                    onClick = { onTheme(option) },
                )
            }
        }

        Text(stringResource(R.string.settings_display_name), style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; nameError = false },
            modifier = Modifier.fillMaxWidth().testTag("setting-display-name"),
            label = { Text(stringResource(R.string.settings_display_name)) },
            singleLine = true,
            isError = nameError,
            supportingText = if (nameError) ({ Text(stringResource(R.string.settings_name_required)) }) else null,
        )
        Button(
            onClick = {
                val cleaned = name.trim()
                if (cleaned.isEmpty()) nameError = true else { nameError = false; onDisplayName(cleaned) }
            },
            modifier = Modifier.heightIn(min = Spacing.touch).testTag("setting-save-name"),
            shape = RikoShapes.pill,
        ) { Text(stringResource(R.string.settings_save_name)) }

        Text(stringResource(R.string.settings_navigation), style = MaterialTheme.typography.titleLarge)
        Column(Modifier.selectableGroup()) {
            NavigationStyle.entries.forEach { option ->
                PreferenceRadio(
                    label = stringResource(when (option) {
                        NavigationStyle.SPATIAL_COMPASS -> R.string.settings_nav_spatial
                        NavigationStyle.COMPASS_ONLY -> R.string.settings_nav_compass
                    }),
                    selected = option == settings.navStyle,
                    testTag = "setting-navigation-${option.name}",
                    onClick = { onNavigationStyle(option) },
                )
            }
        }
        Text(stringResource(R.string.local_data), style = MaterialTheme.typography.bodyMedium)
        backupContent()
    }
}

@Preview
@Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsPreview() {
    RikoHubTheme { SettingsScreen(AppSettings(), {}, {}, {}, {}) }
}

@Composable
private fun PreferenceRadio(label: String, selected: Boolean, testTag: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = Spacing.row).testTag(testTag)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.space3),
    ) {
        RadioButton(selected = selected, onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.onSurface))
        Text(label)
    }
}

// TODO(v0.2): Add deliberate manual export/import; automatic backup remains disabled.
