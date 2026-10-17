package id.riko.hub.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.riko.hub.R
import id.riko.hub.domain.model.AppSettings
import id.riko.hub.domain.model.ThemeMode
import id.riko.hub.navigation.Destination
import id.riko.hub.ui.components.ScreenFrame
import id.riko.hub.ui.components.rikoFormFieldColors
import id.riko.hub.ui.theme.*

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onTheme: (ThemeMode) -> Unit,
    onDisplayName: (String) -> Unit,
    backupContent: @Composable () -> Unit = {},
) {
    var name by rememberSaveable(settings.displayName) { mutableStateOf(settings.displayName) }
    var nameError by remember { mutableStateOf(false) }

    ScreenFrame(Destination.Settings) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = ChefButter,
            border = BorderStroke(2.dp, ChefGround),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "PREFERENSI · KONTROL PRIBADI",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = ChefGround.copy(alpha = 0.7f),
                    )
                    Text(
                        "ATUR\nRUANGMU.",
                        fontFamily = NotoCondensedBlack,
                        fontWeight = FontWeight.Black,
                        fontSize = 32.sp,
                        lineHeight = 34.sp,
                        color = ChefGround,
                        modifier = Modifier.semantics { heading() },
                    )
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(ChefGround),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("⚙", fontSize = 20.sp)
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = ChefPanel,
            border = BorderStroke(1.dp, ChefLine),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(ChefCoral),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        name.trim().firstOrNull()?.uppercase() ?: "R",
                        fontFamily = NotoCondensedBlack,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        color = ChefGround,
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(settings.displayName, style = MaterialTheme.typography.titleMedium, color = ChefInk, fontWeight = FontWeight.Bold)
                    Text("NAMA TAMPILAN", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = ChefMuted)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; nameError = false },
                        modifier = Modifier.fillMaxWidth().testTag("setting-display-name"),
                        label = { Text(stringResource(R.string.settings_display_name), color = ChefMuted) },
                        colors = rikoFormFieldColors(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        isError = nameError,
                        supportingText = if (nameError) ({ Text(stringResource(R.string.settings_name_required), color = ChefCoral) }) else null,
                    )
                    Button(
                        onClick = {
                            val cleaned = name.trim()
                            if (cleaned.isEmpty()) nameError = true else { nameError = false; onDisplayName(cleaned) }
                        },
                        modifier = Modifier.heightIn(min = 44.dp).testTag("setting-save-name"),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ChefLime, contentColor = ChefGround),
                    ) {
                        Text(stringResource(R.string.settings_save_name), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = ChefPanel,
            border = BorderStroke(1.dp, ChefLine),
        ) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.theme).uppercase(),
                    fontFamily = NotoCondensedBlack,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = ChefInk,
                )
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
            }
        }

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = ChefPanel,
            border = BorderStroke(1.dp, ChefLine),
        ) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.local_data).uppercase(),
                    fontFamily = NotoCondensedBlack,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = ChefInk,
                )
                backupContent()
            }
        }
    }
}

@Preview
@Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsPreview() {
    RikoHubTheme { SettingsScreen(AppSettings(), {}, {}) }
}

@Composable
private fun PreferenceRadio(label: String, selected: Boolean, testTag: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp).testTag(testTag)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = ChefLime,
                unselectedColor = ChefMuted,
            )
        )
        Text(label, color = ChefInk, style = MaterialTheme.typography.bodyMedium)
    }
}
