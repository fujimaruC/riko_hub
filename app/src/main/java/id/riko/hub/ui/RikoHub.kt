package id.riko.hub.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import id.riko.hub.RikoHubApp
import id.riko.hub.navigation.RikoNavHost
import id.riko.hub.navigation.SettingsWriteFailure
import id.riko.hub.domain.model.AppSettings
import id.riko.hub.ui.theme.RikoHubTheme
import id.riko.hub.ui.theme.ThemeMode
import kotlinx.coroutines.launch
import java.io.IOException

internal enum class SettingsPreference { THEME, DISPLAY_NAME, NAVIGATION_STYLE }

internal class SettingsWriteTracker {
    private val generations = mutableMapOf<SettingsPreference, Int>()
    private val failures = mutableStateMapOf<SettingsPreference, SettingsWriteFailure>()

    fun start(preference: SettingsPreference): Int {
        val generation = (generations[preference] ?: 0) + 1
        generations[preference] = generation
        failures.remove(preference)
        return generation
    }

    fun isCurrent(preference: SettingsPreference, generation: Int) = generations[preference] == generation
    fun failed(preference: SettingsPreference, generation: Int, failure: SettingsWriteFailure) {
        if (isCurrent(preference, generation)) failures[preference] = failure
    }
    fun succeeded(preference: SettingsPreference, generation: Int) {
        if (isCurrent(preference, generation)) failures.remove(preference)
    }
    fun dismiss(id: Int) {
        failures.entries.firstOrNull { it.value.id == id }?.let { failures.remove(it.key) }
    }
    fun earliestFailure() = failures.values.minByOrNull { it.id }
}

@Composable
fun RikoHub(openTaskId: Long? = null, onTaskOpenConsumed: () -> Unit = {}, onDarkThemeChanged: (Boolean) -> Unit = {}) {
    val context = LocalContext.current.applicationContext
    val settingsStore = (context as RikoHubApp).container.settings
    val settings by remember(settingsStore) { settingsStore.settings }.collectAsState(initial = AppSettings())
    val scope = rememberCoroutineScope()
    var failureId by remember { mutableIntStateOf(0) }
    val writes = remember { SettingsWriteTracker() }
    val persist: (SettingsPreference, suspend () -> Unit) -> Unit = { preference, write ->
        val generation = writes.start(preference)
        fun runAttempt() {
            scope.launch {
                try {
                    write()
                    writes.succeeded(preference, generation)
                }
                catch (error: IOException) {
                    Log.e("Settings", "Cannot save preference", error)
                    writes.failed(preference, generation, SettingsWriteFailure(++failureId) {
                        if (writes.isCurrent(preference, generation)) runAttempt()
                    })
                }
            }
        }
        runAttempt()
    }
    val mode = settings.themeMode
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    SideEffect { onDarkThemeChanged(dark) }
    RikoHubTheme(dark) {
        RikoNavHost(settings, onTheme = { selected -> persist(SettingsPreference.THEME) { settingsStore.setTheme(selected) } },
            onDisplayName = { value -> persist(SettingsPreference.DISPLAY_NAME) { settingsStore.setDisplayName(value) } },
            onNavigationStyle = { value -> persist(SettingsPreference.NAVIGATION_STYLE) { settingsStore.setNavigationStyle(value) } },
            settingsWriteFailure = writes.earliestFailure(),
            onSettingsFailureDismissed = writes::dismiss,
            backupContent = { id.riko.hub.ui.settings.BackupSettingsSection((context as RikoHubApp).container.backups) },
            openTaskId = openTaskId, onTaskOpenConsumed = onTaskOpenConsumed)
    }
}
