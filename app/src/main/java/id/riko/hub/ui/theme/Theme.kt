package id.riko.hub.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalRikoDarkTheme = staticCompositionLocalOf { true }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Composable
fun RikoHubTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalRikoColors provides if (darkTheme) DarkRikoColors else LightRikoColors,
        LocalRikoDarkTheme provides darkTheme,
        LocalSpacesPalette provides if (darkTheme) DarkSpacesPalette else LightSpacesPalette,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = RikoTypography,
            shapes = RikoMaterialShapes,
            content = content,
        )
    }
}
