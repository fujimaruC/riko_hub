package id.riko.hub.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object RikoShapes {
    val xs = RoundedCornerShape(8.dp)
    val small = RoundedCornerShape(12.dp)
    val medium = RoundedCornerShape(16.dp)
    val wide = RoundedCornerShape(24.dp)
    val tall = RoundedCornerShape(28.dp)
    val pill = RoundedCornerShape(50)
    val asymA = RoundedCornerShape(topStart = 32.dp, topEnd = 12.dp, bottomEnd = 32.dp, bottomStart = 12.dp)
    val asymB = RoundedCornerShape(topStart = 12.dp, topEnd = 32.dp, bottomEnd = 12.dp, bottomStart = 32.dp)
    val sheet = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
}
internal val RikoMaterialShapes = Shapes(
    extraSmall = RikoShapes.xs, small = RikoShapes.small, medium = RikoShapes.medium,
    large = RikoShapes.wide, extraLarge = RikoShapes.tall,
)
