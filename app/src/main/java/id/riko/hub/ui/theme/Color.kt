package id.riko.hub.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Authentic M-CHEF Palette Tokens (DESIGN.md & full-app-v1.html)
val ChefGround = Color(0xFF1A1B18)
val ChefCanvas = Color(0xFF222320)
val ChefPanel = Color(0xFF2A2B27)
val ChefRaised = Color(0xFF33342E)
val ChefLine = Color(0xFF3D3E37)
val ChefInk = Color(0xFFF8F6EC)
val ChefMuted = Color(0xFFA3A499)

// Accent tokens
val ChefPeriwinkle = Color(0xFF5463FF)
val ChefCoral = Color(0xFFFF5A43)
val ChefButter = Color(0xFFF5CB58)
val ChefSage = Color(0xFFCBD7C9)
val ChefLime = Color(0xFFD9F35B)
val ChefCyan = Color(0xFF9FE3DE)

// Compatibility aliases
internal val PreviewGround = ChefCanvas
internal val PreviewPanel = ChefPanel
internal val PreviewRaised = ChefRaised
internal val PreviewInk = ChefInk
internal val PreviewMuted = ChefMuted
internal val PreviewOrange = ChefCoral
internal val PreviewLime = ChefLime
internal val PreviewPink = ChefCoral
internal val PreviewButter = ChefButter
internal val PreviewSage = ChefSage
internal val PreviewLine = ChefLine

internal val LightScheme = lightColorScheme(
    primary = Color(0xFFB84D13), onPrimary = Color.White,
    primaryContainer = ChefCoral, onPrimaryContainer = ChefGround,
    secondary = Color(0xFF71806E), onSecondary = Color.White,
    secondaryContainer = ChefSage, onSecondaryContainer = ChefGround,
    tertiary = ChefCoral, onTertiary = Color.White,
    tertiaryContainer = ChefButter, onTertiaryContainer = ChefGround,
    background = Color(0xFFF1EFE6), onBackground = ChefGround,
    surface = Color(0xFFFAF8F0), onSurface = ChefGround,
    surfaceVariant = Color(0xFFE6E5DB), onSurfaceVariant = Color(0xFF55574E),
    surfaceContainer = Color(0xFFE6E5DB), surfaceContainerHigh = Color(0xFFFAF8F0),
    outline = Color(0xFFB7B8AE), outlineVariant = Color(0xFFD0D0C6),
    error = Color(0xFFBA1A1A), errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002), scrim = ChefGround,
)

internal val DarkScheme = darkColorScheme(
    primary = ChefLime, onPrimary = ChefGround,
    primaryContainer = ChefLime, onPrimaryContainer = ChefGround,
    secondary = ChefSage, onSecondary = ChefGround,
    secondaryContainer = Color(0xFF41473E), onSecondaryContainer = ChefInk,
    tertiary = ChefCoral, onTertiary = ChefGround,
    tertiaryContainer = ChefCoral, onTertiaryContainer = ChefGround,
    background = ChefCanvas, onBackground = ChefInk,
    surface = ChefPanel, onSurface = ChefInk,
    surfaceVariant = ChefRaised, onSurfaceVariant = ChefMuted,
    surfaceDim = ChefGround, surfaceBright = ChefRaised,
    surfaceContainerLowest = ChefGround, surfaceContainerLow = ChefPanel,
    surfaceContainer = ChefPanel, surfaceContainerHigh = ChefRaised,
    surfaceContainerHighest = Color(0xFF3E3F38),
    outline = ChefLine, outlineVariant = ChefRaised,
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    scrim = Color.Black,
)
