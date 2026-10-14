package id.riko.hub.ui.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.riko.hub.R
import id.riko.hub.navigation.Destination
import id.riko.hub.navigation.isSpaces
import id.riko.hub.ui.components.destinationLabel
import id.riko.hub.ui.theme.Motion
import id.riko.hub.ui.theme.SpacesTokens

private val destinations = listOf(
    Destination.Home, Destination.Timeline, Destination.Capture, Destination.Vault, Destination.Spaces,
)
private val icons = mapOf(
    Destination.Home to R.drawable.ic_dock_today,
    Destination.Timeline to R.drawable.ic_dock_plan,
    Destination.Capture to R.drawable.ic_dock_capture,
    Destination.Vault to R.drawable.ic_dock_vault,
    Destination.Spaces to R.drawable.ic_dock_spaces,
)

@Composable
fun RenaissanceDock(
    current: Destination,
    reducedMotion: Boolean,
    onNavigate: (Destination) -> Unit,
) {
    val tab = when {
        current.isSpaces || current == Destination.Courses || current == Destination.Sync -> Destination.Spaces
        current == Destination.Tasks || current == Destination.Settings -> Destination.Home
        else -> current
    }
    val selected = destinations.indexOf(tab).takeIf { it >= 0 } ?: destinations.indexOf(Destination.Home)
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val selectedPosition = if (isRtl) destinations.lastIndex - selected else selected
    val marker by animateFloatAsState(selectedPosition.toFloat(),
        animationSpec = if (reducedMotion) snap() else tween(Motion.dock, easing = Motion.standard), label = "dock selection")
    val background = if (current.isSpaces) SpacesTokens.surface else MaterialTheme.colorScheme.surface
    val accent = if (current.isSpaces) SpacesTokens.tangerine else MaterialTheme.colorScheme.primary
    val inactive = if (current.isSpaces) SpacesTokens.secondary else MaterialTheme.colorScheme.onSurfaceVariant
    val selectedIcon = if (current.isSpaces) SpacesTokens.canvas else MaterialTheme.colorScheme.onPrimary
    Surface(color = Color.Transparent, modifier = Modifier.fillMaxWidth().heightIn(min = 88.dp)) {
        Box(Modifier.fillMaxWidth()) {
            Canvas(Modifier.matchParentSize()) {
                val tabsWidth = size.width
                val segment = tabsWidth / destinations.size
                val centerX = (marker + 0.5f) * segment
                val shoulder = 24.dp.toPx()
                val corner = 12.dp.toPx()
                val half = minOf(segment * 0.46f, centerX - corner, tabsWidth - centerX - corner)
                val depth = 28.dp.toPx()
                val path = Path().apply {
                    moveTo(corner, shoulder)
                    lineTo(centerX - half, shoulder)
                    cubicTo(centerX - half * 0.58f, shoulder + depth, centerX - half * 0.35f, shoulder + depth, centerX, shoulder + depth)
                    cubicTo(centerX + half * 0.35f, shoulder + depth, centerX + half * 0.58f, shoulder + depth, centerX + half, shoulder)
                    lineTo(size.width - corner, shoulder)
                    quadraticTo(size.width, shoulder, size.width, shoulder + corner)
                    lineTo(size.width, size.height - corner)
                    quadraticTo(size.width, size.height, size.width - corner, size.height)
                    lineTo(corner, size.height)
                    quadraticTo(0f, size.height, 0f, size.height - corner)
                    lineTo(0f, shoulder + corner)
                    quadraticTo(0f, shoulder, corner, shoulder)
                    close()
                }
                drawPath(path, background)
                drawCircle(accent, minOf(20.dp.toPx(), segment * 0.28f), Offset(centerX, 24.dp.toPx()))
            }
            Row(Modifier.fillMaxWidth().testTag("dock").selectableGroup(), verticalAlignment = Alignment.CenterVertically) {
                destinations.forEach { destination ->
                    val index = destinations.indexOf(destination)
                    val position = if (isRtl) destinations.lastIndex - index else index
                    val emphasis = (1f - kotlin.math.abs(marker - position)).coerceIn(0f, 1f)
                    val isSelected = destination == destinations.getOrNull(selected)
                    val label = when (destination) {
                        Destination.Home -> stringResource(R.string.today)
                        Destination.Timeline -> stringResource(R.string.shell_plan)
                        else -> destinationLabel(destination)
                    }
                    val color = lerp(inactive, selectedIcon, emphasis)
                    Column(
                        Modifier.weight(1f).widthIn(min = 48.dp).heightIn(min = 88.dp).testTag("dock-${destination.name}").selectable(
                            selected = isSelected, role = Role.Tab,
                            onClick = { onNavigate(destination) },
                        ).semantics { contentDescription = label }.padding(bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top,
                    ) {
                        Box(Modifier.fillMaxWidth().height(64.dp)) {
                            androidx.compose.material3.Icon(painterResource(icons.getValue(destination)), null,
                                tint = color,
                                modifier = Modifier.align(Alignment.TopCenter)
                                    .offset(y = (31f - 18f * emphasis).dp).size(22.dp)
                                    .testTag("dock-icon-${destination.name}"))
                        }
                        Text(label, color = lerp(inactive, accent, emphasis),
                            style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
