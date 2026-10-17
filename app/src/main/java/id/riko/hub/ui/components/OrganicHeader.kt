package id.riko.hub.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import id.riko.hub.ui.theme.RikoShapes
import id.riko.hub.ui.theme.Spacing
import id.riko.hub.ui.theme.rememberReducedMotion

/** Decorative contours stay behind the native content and its hit targets. */
@Composable
fun OrganicHeader(
    modifier: Modifier = Modifier,
    animated: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val reduced = rememberReducedMotion()
    var visible by remember { mutableStateOf(false) }
    val phase = if (animated && visible && !reduced && lifecycle == Lifecycle.State.RESUMED) {
        val transition = rememberInfiniteTransition(label = "organic header")
        transition.animateFloat(0f, 1f,
            infiniteRepeatable(tween(9_000, easing = LinearEasing), RepeatMode.Reverse), label = "lava contour")
    } else rememberUpdatedState(.5f)
    val orange = MaterialTheme.colorScheme.primary
    val foreground = MaterialTheme.colorScheme.onPrimary
    val contourColor = lerp(orange, foreground, .12f)
    Column(modifier.fillMaxWidth().onGloballyPositioned {
        val bounds = it.boundsInWindow()
        visible = bounds.width > 0f && bounds.height > 0f
    }.clip(RikoShapes.asymA).drawWithCache {
        val glow = Brush.radialGradient(listOf(contourColor.copy(alpha = .52f), Color.Transparent),
            Offset(size.width * .84f, size.height * .12f), size.width.coerceAtLeast(1f) * .76f)
        val contour = Path()
        onDrawBehind {
            drawRect(orange)
            drawRect(glow)
            val shift = phase.value * size.height * .12f
            contour.reset()
            contour.moveTo(0f, size.height * .83f + shift)
            contour.cubicTo(size.width * .20f, size.height * .48f + shift,
                size.width * .48f, size.height * 1.12f - shift, size.width * .65f, size.height * .73f)
            contour.cubicTo(size.width * .80f, size.height * .40f - shift,
                size.width * .94f, size.height * .68f + shift, size.width, size.height * .54f)
            contour.lineTo(size.width, size.height)
            contour.lineTo(0f, size.height)
            contour.close()
            drawPath(contour, contourColor.copy(alpha = .62f))
        }
    }.padding(Spacing.space5), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CompositionLocalProvider(LocalContentColor provides foreground, content = { content() })
    }
}
