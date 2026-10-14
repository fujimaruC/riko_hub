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
    academic = accent(0xFF4A5FD0, 0xFF22294C, 0xFFE8E6E1),
    organization = accent(0xFF2FA37A, 0xFF17382C, 0xFF101213),
    personal = accent(0xFFB8458F, 0xFF3A2030, 0xFFE8E6E1),
    projects = accent(0xFFD4A030, 0xFF3B3019, 0xFF0F0E0D),
    urgent = accent(0xFFFF907F, 0xFF5A2C28, 0xFF2B1210),
    peach = accent(0xFFB5AFA8, 0xFF303536, 0xFF101213),
)
val LocalRikoColors = staticCompositionLocalOf { LightRikoColors }
