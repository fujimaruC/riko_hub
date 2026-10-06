package id.riko.hub.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import android.widget.Toast
import id.riko.hub.R
import id.riko.hub.navigation.RikoNavHost
import id.riko.hub.ui.theme.RikoHubTheme
import id.riko.hub.ui.theme.ThemeMode
import id.riko.hub.ui.theme.observeTheme
import id.riko.hub.ui.theme.saveTheme
import kotlinx.coroutines.launch
import java.io.IOException

@Composable
fun RikoHub(onDarkThemeChanged: (Boolean) -> Unit = {}) {
    val context = LocalContext.current.applicationContext
    val storedMode by remember(context) { context.observeTheme() }.collectAsState(initial = ThemeMode.SYSTEM)
    var pendingMode by rememberSaveable { mutableStateOf<ThemeMode?>(null) }
    val scope = rememberCoroutineScope()
    val mode = pendingMode ?: storedMode
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    SideEffect { onDarkThemeChanged(dark) }
    RikoHubTheme(dark) {
        RikoNavHost(mode, onTheme = { selected ->
            pendingMode = selected
            scope.launch {
                try {
                    context.saveTheme(selected)
                } catch (error: IOException) {
                    Log.e("ThemePreference", "Cannot save theme preference", error)
                    Toast.makeText(context, R.string.theme_save_failed, Toast.LENGTH_SHORT).show()
                }
            }
        })
    }
}
