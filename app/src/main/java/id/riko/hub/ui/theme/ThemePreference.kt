package id.riko.hub.ui.theme

import android.content.Context
import id.riko.hub.RikoHubApp
import kotlinx.coroutines.flow.map

private val Context.settings get() = (applicationContext as RikoHubApp).container.settings
fun Context.observeTheme() = settings.settings.map { it.themeMode }
suspend fun Context.saveTheme(mode: ThemeMode) = settings.setTheme(mode)
