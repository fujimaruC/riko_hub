package id.riko.hub.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class Accent(val color: Color, val container: Color, val ink: Color)

@Immutable
data class RikoColors(
    val academic: Accent, val organization: Accent, val personal: Accent,
    val projects: Accent, val urgent: Accent, val peach: Accent,
)
private fun accent(color: Color, container: Color, ink: Color = PreviewGround) = Accent(color, container, ink)

internal val LightRikoColors = RikoColors(
    academic = accent(ChefCoral, Color(0xFFFFD5CE)),
    organization = accent(ChefSage, Color(0xFFE0E6DC)),
    personal = accent(ChefButter, Color(0xFFFDF0CD)),
    projects = accent(ChefLime, Color(0xFFE9F1B8)),
    urgent = accent(ChefCoral, Color(0xFFFFD5CE)),
    peach = accent(ChefCoral, Color(0xFFFFE1CC)),
)
internal val DarkRikoColors = RikoColors(
    academic = accent(ChefCoral, Color(0xFF4A1F18)),
    organization = accent(ChefSage, Color(0xFF333A31)),
    personal = accent(ChefButter, Color(0xFF473C1E)),
    projects = accent(ChefLime, Color(0xFF38401C)),
    urgent = accent(ChefCoral, Color(0xFF4A1F18)),
    peach = accent(ChefSage, ChefRaised),
)
val LocalRikoColors = staticCompositionLocalOf { LightRikoColors }
