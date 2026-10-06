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
private fun accent(color: Long, container: Long, ink: Long = 0xFF2B2630) =
    Accent(Color(color), Color(container), Color(ink))

internal val LightRikoColors = RikoColors(
    academic = accent(0xFFA793EA, 0xFFE9E3FB),
    organization = accent(0xFF6DB8EE, 0xFFDCEFFC),
    personal = accent(0xFF6FD0AD, 0xFFD8F3E8),
    projects = accent(0xFFF4C04F, 0xFFFFF0C9),
    urgent = accent(0xFFFF7F6E, 0xFFFFE0DA),
    peach = accent(0xFFFFB48A, 0xFFFFE7D8),
)
internal val DarkRikoColors = RikoColors(
    academic = accent(0xFFB9A8F2, 0xFF3A3360, 0xFF1B1822),
    organization = accent(0xFF82C6F4, 0xFF1F3A52, 0xFF14202B),
    personal = accent(0xFF82DDBC, 0xFF1D4136, 0xFF12251F),
    projects = accent(0xFFF7CD68, 0xFF4A3E1B, 0xFF2A2208),
    urgent = accent(0xFFFF907F, 0xFF5A2C28, 0xFF2B1210),
    peach = accent(0xFFFFC19B, 0xFF503528, 0xFF2B1A10),
)
val LocalRikoColors = staticCompositionLocalOf { LightRikoColors }
