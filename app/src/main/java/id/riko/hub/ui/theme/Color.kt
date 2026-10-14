package id.riko.hub.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

internal val LightScheme = lightColorScheme(
    primary = Color(0xFFA34412), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFE4CF), onPrimaryContainer = Color(0xFF2B2630),
    secondary = Color(0xFF6DB8EE), onSecondary = Color(0xFF2B2630),
    secondaryContainer = Color(0xFFDCEFFC), onSecondaryContainer = Color(0xFF2B2630),
    tertiary = Color(0xFF6FD0AD), onTertiary = Color(0xFF2B2630),
    tertiaryContainer = Color(0xFFD8F3E8), onTertiaryContainer = Color(0xFF2B2630),
    background = Color(0xFFFBF6EC), onBackground = Color(0xFF2B2630),
    surface = Color(0xFFFFFDF8), onSurface = Color(0xFF2B2630),
    surfaceVariant = Color(0xFFF3EBDD), onSurfaceVariant = Color(0xFF6F6877),
    surfaceContainer = Color(0xFFF3EBDD), surfaceContainerHigh = Color(0xFFFFFDF8),
    outline = Color(0xFFE2D8C6), outlineVariant = Color(0xFFE2D8C6),
    error = Color(0xFF2B2630), errorContainer = Color(0xFFFFE0DA),
    onErrorContainer = Color(0xFF2B2630), scrim = Color(0xFF2B2630),
)
internal val DarkScheme = darkColorScheme(
    primary = Color(0xFFE8742A), onPrimary = Color(0xFF101213),
    primaryContainer = Color(0xFF442716), onPrimaryContainer = Color(0xFFE8E6E1),
    secondary = Color(0xFF2FA37A), onSecondary = Color(0xFF101213),
    secondaryContainer = Color(0xFF17382C), onSecondaryContainer = Color(0xFFE8E6E1),
    tertiary = Color(0xFF4A5FD0), onTertiary = Color(0xFFE8E6E1),
    tertiaryContainer = Color(0xFF22294C), onTertiaryContainer = Color(0xFFE8E6E1),
    background = Color(0xFF101213), onBackground = Color(0xFFE8E6E1),
    surface = Color(0xFF191C1D), onSurface = Color(0xFFE8E6E1),
    surfaceVariant = Color(0xFF242829), onSurfaceVariant = Color(0xFFB5AFA8),
    surfaceDim = Color(0xFF101213), surfaceBright = Color(0xFF303536),
    surfaceContainerLowest = Color(0xFF101213), surfaceContainerLow = Color(0xFF191C1D),
    surfaceContainer = Color(0xFF242829), surfaceContainerHigh = Color(0xFF303536),
    surfaceContainerHighest = Color(0xFF383D3E),
    outline = Color(0xFF555B5C), outlineVariant = Color(0xFF303536),
    error = Color(0xFFFF9F91), onError = Color(0xFF101213),
    errorContainer = Color(0xFF512020), onErrorContainer = Color(0xFFE8E6E1),
    scrim = Color.Black,
)
