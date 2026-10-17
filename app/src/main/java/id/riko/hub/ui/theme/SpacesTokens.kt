package id.riko.hub.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class SpacesPalette(
    val canvas: Color, val surface: Color, val row: Color, val pressed: Color, val edge: Color,
    val primary: Color, val secondary: Color, val muted: Color, val tangerine: Color,
    val tangerineDim: Color, val jade: Color, val ultramarine: Color, val magenta: Color,
    val amber: Color, val connected: Color, val cream: Color, val sage: Color,
)

internal val DarkSpacesPalette = SpacesPalette(
    canvas = ChefCanvas, surface = ChefPanel, row = ChefRaised,
    pressed = Color(0xFF3E3F38), edge = ChefLine, primary = ChefInk,
    secondary = ChefMuted, muted = Color(0xFF898A80), tangerine = ChefCoral,
    tangerineDim = Color(0xFF9D4E20), jade = ChefSage,
    ultramarine = ChefPeriwinkle, magenta = ChefCoral,
    amber = ChefButter, connected = ChefLime, cream = ChefButter, sage = ChefSage,
)

internal val LightSpacesPalette = SpacesPalette(
    canvas = Color(0xFFF1EFE6), surface = Color(0xFFFAF8F0), row = Color(0xFFE6E5DB),
    pressed = Color(0xFFD8D8CC), edge = Color(0xFFB7B8AE), primary = ChefGround,
    secondary = Color(0xFF55574E), muted = Color(0xFF73756B), tangerine = Color(0xFFB84D13),
    tangerineDim = Color(0xFF8D3D10), jade = Color(0xFF71806E),
    ultramarine = ChefPeriwinkle, magenta = ChefCoral,
    amber = Color(0xFF82692D), connected = Color(0xFF68771F), cream = Color(0xFF82692D),
    sage = Color(0xFF71806E),
)

val LocalSpacesPalette = staticCompositionLocalOf { DarkSpacesPalette }

object SpacesTokens {
    val canvas @Composable get() = LocalSpacesPalette.current.canvas
    val surface @Composable get() = LocalSpacesPalette.current.surface
    val row @Composable get() = LocalSpacesPalette.current.row
    val pressed @Composable get() = LocalSpacesPalette.current.pressed
    val edge @Composable get() = LocalSpacesPalette.current.edge
    val tangerine @Composable get() = LocalSpacesPalette.current.tangerine
    val tangerineDim @Composable get() = LocalSpacesPalette.current.tangerineDim
    val primary @Composable get() = LocalSpacesPalette.current.primary
    val secondary @Composable get() = LocalSpacesPalette.current.secondary
    val muted @Composable get() = LocalSpacesPalette.current.muted
    val jade @Composable get() = LocalSpacesPalette.current.jade
    val ultramarine @Composable get() = LocalSpacesPalette.current.ultramarine
    val magenta @Composable get() = LocalSpacesPalette.current.magenta
    val amber @Composable get() = LocalSpacesPalette.current.amber
    val connected @Composable get() = LocalSpacesPalette.current.connected
    val cream @Composable get() = LocalSpacesPalette.current.cream
    val sage @Composable get() = LocalSpacesPalette.current.sage
}
