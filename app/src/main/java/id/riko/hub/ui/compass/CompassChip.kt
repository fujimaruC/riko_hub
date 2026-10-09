package id.riko.hub.ui.compass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.withTimeoutOrNull
import id.riko.hub.R
import id.riko.hub.navigation.*
import id.riko.hub.ui.components.destinationLabel
import id.riko.hub.ui.theme.*

@Composable
fun CompassChip(current: Destination, reducedMotion: Boolean, onNavigate: (Destination) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var holding by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(Destination.Home) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val navigate by rememberUpdatedState(onNavigate)
    val description = stringResource(R.string.open_compass, destinationLabel(current))
    val dragDistance = with(LocalDensity.current) { Spacing.compassDrag.toPx() }
    val accents = LocalRikoColors.current
    val selectedColor = when (selected) {
        Destination.Home -> accents.peach
        Destination.Tasks -> accents.academic
        Destination.Timeline -> accents.organization
        Destination.Vault -> accents.projects
        Destination.Sync -> accents.personal
        Destination.Settings -> accents.peach
    }
    val bloom by animateFloatAsState(
        targetValue = if (holding) 1f else 0f,
        animationSpec = if (reducedMotion) snap() else spring(
            dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium,
        ), label = "compass bloom",
    )
    val ballColor by animateColorAsState(
        targetValue = if (holding) selectedColor.container else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(if (reducedMotion) 0 else Motion.short), label = "compass color",
    )
    Box(contentAlignment = Alignment.Center) {
        BubbleHalo(bloom)
        FilledTonalIconButton(
            onClick = { expanded = true }, shape = RikoShapes.pill,
            modifier = Modifier.size(56.dp).testTag("compass-ball")
                .pointerInput(current, dragDistance) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        // Handle touch here; IconButton keeps its accessible click action.
                        down.consume()
                        var tapped = false
                        val held = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis.toLong()) {
                            while (true) {
                                val change = awaitPointerEvent(PointerEventPass.Initial).changes
                                    .firstOrNull { it.id == down.id } ?: return@withTimeoutOrNull false
                                if (!change.pressed) {
                                    tapped = true
                                    return@withTimeoutOrNull false
                                }
                                if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop)
                                    return@withTimeoutOrNull false
                            }
                        } == null
                        if (tapped) expanded = true
                        if (held) {
                            holding = true
                            selected = Destination.Home
                            var destination: Destination? = null
                            try {
                                while (true) {
                                    val change = awaitPointerEvent(PointerEventPass.Initial).changes
                                        .firstOrNull { it.id == down.id } ?: break
                                    val movement = change.position - down.position
                                    dragOffset = movement
                                    selected = SpatialMap.compassTarget(movement.x, movement.y, dragDistance)
                                    change.consume()
                                    if (!change.pressed) {
                                        destination = selected
                                        break
                                    }
                                }
                            } finally {
                                holding = false
                                dragOffset = Offset.Zero
                            }
                            destination?.takeIf { it != current }?.let(navigate)
                        }
                    }
                }
                .graphicsLayer {
                    translationX = dragOffset.x * 0.5f
                    translationY = dragOffset.y * 0.5f
                    scaleX = 1f + bloom * 0.08f
                    scaleY = 1f + bloom * 0.08f
                }
                .semantics { contentDescription = description },
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = ballColor,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        ) { CompassDots(current, bloom) }
    }
    if (expanded) Dialog(onDismissRequest = { expanded = false }) {
        Surface(shape = RikoShapes.tall, color = MaterialTheme.colorScheme.surface) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(Spacing.space5),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.space2),
            ) {
                Text(stringResource(R.string.choose_space), style = MaterialTheme.typography.titleLarge)
                CompassDestination(Destination.Timeline, current, onNavigate) { expanded = false }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.space1)) {
                    listOf(Destination.Sync, Destination.Home, Destination.Vault).forEach { destination ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            CompassDestination(destination, current, onNavigate) { expanded = false }
                        }
                    }
                }
                CompassDestination(Destination.Tasks, current, onNavigate) { expanded = false }
                TextButton(onClick = { expanded = false }) { Text(stringResource(R.string.close), color = MaterialTheme.colorScheme.onSurface) }
            }
        }
    }
}

@Composable
private fun BubbleHalo(bloom: Float) {
    val colors = LocalRikoColors.current
    Canvas(Modifier.size(56.dp)) {
        val origin = center
        val satellites = listOf(
            Triple(Offset(-72.dp.toPx(), -20.dp.toPx()), colors.personal.color, 9.dp.toPx()),
            Triple(Offset(-58.dp.toPx(), -64.dp.toPx()), colors.academic.color, 13.dp.toPx()),
            Triple(Offset(-16.dp.toPx(), -82.dp.toPx()), colors.organization.color, 8.dp.toPx()),
            Triple(Offset(-92.dp.toPx(), -80.dp.toPx()), colors.projects.color, 6.dp.toPx()),
        )
        satellites.forEach { (distance, color, radius) ->
            drawCircle(color.copy(alpha = 0.75f * bloom.coerceIn(0f, 1f)),
                radius = radius * bloom.coerceAtLeast(0f), center = origin + distance * bloom)
        }
    }
}

@Composable
private fun CompassDestination(
    destination: Destination, current: Destination,
    onNavigate: (Destination) -> Unit, onDismiss: () -> Unit,
) {
    TextButton(
        onClick = { onDismiss(); onNavigate(destination) },
        modifier = Modifier.heightIn(min = Spacing.touch).semantics { selected = current == destination },
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface,
            containerColor = if (current == destination) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
        contentPadding = PaddingValues(Spacing.space2),
    ) { Text(destinationLabel(destination)) }
}

@Composable
private fun CompassDots(current: Destination, bloom: Float) {
    val ink = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(Modifier.size(Spacing.compass)) {
        compassDestinations.forEach { destination ->
            val position = requireNotNull(SpatialMap.position(destination))
            val center = Offset(size.width * (0.5f + position.x * 0.3f), size.height * (0.5f + position.y * 0.3f))
            drawCircle(if (current == destination) ink else muted, Spacing.compassDot.toPx() * (1f + bloom * 0.25f), center)
        }
    }
}
