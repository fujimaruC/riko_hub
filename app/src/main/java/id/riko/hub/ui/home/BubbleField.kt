package id.riko.hub.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import id.riko.hub.R
import id.riko.hub.domain.logic.*
import id.riko.hub.domain.model.Task
import id.riko.hub.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.sqrt

@Composable
fun BubbleField(snapshot: HomeSnapshot, reduced: Boolean, resumed: Boolean,
                onPeek: (Long) -> Unit, onComplete: (Task) -> Unit, onPin: (Long) -> Unit, onOverflow: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().semantics { isTraversalGroup = true }) {
        val largeText = LocalDensity.current.fontScale >= 1.3f || maxWidth < Spacing.field + Spacing.returnHandle
        val planned = if (largeText) BubbleLayoutPlanner.largeTextPlan(snapshot.ranked, snapshot.focus) else snapshot.bubbles
        val target = if (!largeText && planned.size == 1 && planned.single().urgency != null)
            listOf(planned.single().copy(x = .5, y = .5, diameter = HomeTokens.singleTargetDiameter)) else planned
        var shown by remember { mutableStateOf(target) }
        val completedIds = snapshot.doneToday.map { it.id }.toSet()
        LaunchedEffect(target, completedIds, reduced, resumed) {
            val receding = shown.filter { it.id in completedIds && target.none { next -> next.id == it.id } }
            shown = target + receding
            if (receding.isNotEmpty()) delay(if (reduced || !resumed) Motion.fast.toLong() else HomeTokens.completeMillis.toLong())
            shown = target
        }
        val width = maxWidth
        Column {
            if (largeText) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.space4)) {
                    shown.forEachIndexed { index, item -> key(item.id) {
                        val size = if (item.overflow > 0) maxOf(HomeTokens.minimumLargeTextBubble, width * .4f) else width * item.diameter.toFloat()
                        Box(Modifier.fillMaxWidth(), contentAlignment = if (index % 2 == 0) Alignment.CenterStart else Alignment.CenterEnd) {
                            FieldItem(item, size, snapshot, reduced, resumed, item.id in completedIds, 0.dp,
                                onPeek, onComplete, onPin, onOverflow)
                        }
                    } }
                }
            } else if (shown.isNotEmpty()) Box(Modifier.fillMaxWidth().height(width * BubbleLayoutPlanner.HEIGHT_RATIO.toFloat())) {
                shown.forEachIndexed { index, item -> key(item.id) {
                    val position = DpOffset(width * (item.x - item.diameter / 2).toFloat(),
                        width * (item.y * BubbleLayoutPlanner.HEIGHT_RATIO - item.diameter / 2).toFloat())
                    val offset by animateOffsetAsState(androidx.compose.ui.geometry.Offset(position.x.value, position.y.value),
                        if (reduced || !resumed) snap() else spring(dampingRatio = HomeTokens.springDamping), label = "bubble reflow")
                    val size by animateDpAsState(width * item.diameter.toFloat(),
                        if (reduced || !resumed) snap() else tween(HomeTokens.denseMillis), label = "bubble size")
                    FieldItem(item, size, snapshot, reduced, resumed, item.id in completedIds,
                        width * BubbleLayoutPlanner.driftAllowance(item, target).toFloat(), onPeek, onComplete, onPin, onOverflow,
                        Modifier.offset { IntOffset(offset.x.dp.roundToPx(), offset.y.dp.roundToPx()) }
                            .semantics { traversalIndex = index.toFloat() })
                } }
            }
            RecededDots(snapshot.doneToday.size)
        }
    }
}

@Composable
private fun FieldItem(item: BubblePlacement, size: Dp, snapshot: HomeSnapshot, reduced: Boolean, resumed: Boolean,
                      completing: Boolean, drift: Dp, onPeek: (Long) -> Unit, onComplete: (Task) -> Unit,
                      onPin: (Long) -> Unit, onOverflow: () -> Unit, modifier: Modifier = Modifier) {
    if (item.urgency == null) Surface(onClick = onOverflow, shape = HomeBlobShapes[3],
        color = MaterialTheme.colorScheme.surfaceVariant, modifier = modifier.size(size).sizeIn(minWidth = Spacing.touch, minHeight = Spacing.touch)) {
        Box(contentAlignment = Alignment.Center) { Text(stringResource(R.string.home_overflow, item.overflow),
            style = MaterialTheme.typography.labelMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
    } else TaskBubble(item, size, item.id == snapshot.focus, item.id == snapshot.validPin, snapshot.mode,
        snapshot.now, snapshot.zone, reduced, resumed, completing, drift,
        { onPeek(item.id) }, { onComplete(item.urgency.task) }, { onPin(item.id) }, modifier)
}

@Composable
private fun RecededDots(count: Int) {
    if (count == 0) return
    val color = LocalRikoColors.current.personal.color
    val description = stringResource(R.string.home_receded, count)
    Row(Modifier.fillMaxWidth().padding(top = Spacing.space3).semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(Spacing.space2, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
        repeat(minOf(count, 5)) { Canvas(Modifier.size(Spacing.space3)) { drawCircle(color.copy(alpha = HomeTokens.recededAlpha)) } }
        if (count > 5) Text(stringResource(R.string.home_receded_more, count-5), style = MaterialTheme.typography.labelMedium)
    }
}
