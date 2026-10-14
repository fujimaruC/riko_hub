package id.riko.hub.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Composable
fun RikoHubTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalRikoColors provides if (darkTheme) DarkRikoColors else LightRikoColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = RikoTypography,
            shapes = RikoMaterialShapes,
            content = content,
        )
    }
}
