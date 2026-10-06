package id.riko.hub.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class ContrastTest {
    private fun contrast(a: Color, b: Color): Float {
        val first = a.luminance()
        val second = b.luminance()
        return (maxOf(first, second) + 0.05f) / (minOf(first, second) + 0.05f)
    }
    @Test fun neutral_text_meets_body_contrast_in_both_themes() {
        listOf(LightScheme, DarkScheme).forEach { scheme ->
            listOf(scheme.background, scheme.surface, scheme.surfaceVariant).forEach { background ->
                assertTrue(contrast(scheme.onSurface, background) >= 4.5f)
                assertTrue(contrast(scheme.onSurfaceVariant, background) >= 4.5f)
            }
        }
    }
    @Test fun accent_ink_and_container_text_meet_body_contrast() {
        listOf(LightRikoColors to LightScheme, DarkRikoColors to DarkScheme).forEach { (palette, scheme) ->
            listOf(palette.academic, palette.organization, palette.personal, palette.projects, palette.urgent, palette.peach).forEach { accent ->
                assertTrue(contrast(accent.ink, accent.color) >= 4.5f)
                assertTrue(contrast(scheme.onSurface, accent.container) >= 4.5f)
            }
        }
    }
    @Test fun home_mode_washes_preserve_body_contrast() {
        listOf(LightScheme to listOf(HomeTokens.calmLight, HomeTokens.denseLight),
            DarkScheme to listOf(HomeTokens.calmDark, HomeTokens.denseDark)).forEach { (scheme, backgrounds) ->
            backgrounds.forEach { assertTrue(contrast(scheme.onSurface, it) >= 4.5f) }
        }
    }
}
