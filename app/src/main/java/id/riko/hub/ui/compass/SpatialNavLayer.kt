package id.riko.hub.ui.compass

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.platform.LocalDensity
import id.riko.hub.navigation.Destination
import id.riko.hub.navigation.SpatialMap
import id.riko.hub.ui.theme.Spacing

@Composable
fun SpatialNavLayer(
    from: Destination,
    onNavigate: (Destination) -> Unit,
    modifier: Modifier = Modifier,
    horizontalOnly: Boolean = false,
    content: @Composable () -> Unit,
) {
    val navigate by rememberUpdatedState(onNavigate)
    val density = LocalDensity.current
    val edge = with(density) { Spacing.edgeExclusion.toPx() }
    val threshold = with(density) { Spacing.swipeThreshold.toPx() }
    Box(modifier.pointerInput(from, edge, threshold, horizontalOnly) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            // Use the original down position, before touch slop can move outside the edge zone.
            val startsAtEdge = down.position.x < edge || down.position.x > size.width - edge
            var dragging = false
            while (true) {
                val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: break
                if (change.isConsumed) break
                val total = change.position - down.position
                if (!dragging && total.getDistance() > viewConfiguration.touchSlop) {
                    // Claim only a supported direction before a child scroll/button consumes it.
                    if (horizontalOnly && kotlin.math.abs(total.y) >= kotlin.math.abs(total.x)) break
                    if (SpatialMap.swipeTarget(from, total.x, total.y, startsAtEdge,
                            viewConfiguration.touchSlop) == null) break
                    dragging = true
                }
                if (!change.pressed) {
                    if (dragging) SpatialMap.swipeTarget(from, total.x, total.y, startsAtEdge, threshold)?.let(navigate)
                    break
                }
                // Keep edge-origin horizontal drags available to Android's Back gesture.
                if (dragging && (!startsAtEdge || kotlin.math.abs(total.y) > kotlin.math.abs(total.x))) change.consume()
            }
        }
    }) { content() }
}
