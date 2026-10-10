package id.riko.hub.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import id.riko.hub.R
import id.riko.hub.domain.logic.*
import id.riko.hub.domain.model.*
import id.riko.hub.ui.tasks.*
import id.riko.hub.ui.theme.*
import kotlin.math.*

@Composable
fun TaskBubble(item: BubblePlacement, size: Dp, focus: Boolean, pinned: Boolean, mode: HomeMode,
               now: Long, zone: java.time.ZoneId, reduced: Boolean, resumed: Boolean, completing: Boolean,
               driftLimit: Dp, onPeek: () -> Unit, onComplete: () -> Unit, onPin: () -> Unit, modifier: Modifier = Modifier) {
    val info = requireNotNull(item.urgency)
    val task = info.task
    val colors = LocalRikoColors.current
    val accent = when (task.category) {
        Category.ACADEMIC -> colors.academic; Category.ORGANIZATION -> colors.organization
        Category.PERSONAL -> colors.personal; Category.PROJECTS -> colors.projects
    }
    val shape = HomeBlobShapes[HomeTokens.variant(task.id)]
    val deadline = if (info.isOverdue) stringResource(R.string.home_overdue) else deadlineLabel(task, now, zone)
    val description = stringResource(R.string.home_bubble_description, task.title,
        stringResource(task.priority.label()), deadline, stringResource(task.status.label()))
    val completeLabel = stringResource(R.string.home_complete)
    val pinLabel = stringResource(if (pinned) R.string.home_unpin else R.string.home_pin)
    val focusDescription = if (focus) stringResource(if (pinned) R.string.home_focus_pinned else R.string.home_focus) else ""
    val density = LocalDensity.current
    val phase = remember(task.id) { Animatable(HomeTokens.phase(task.id)) }
    LaunchedEffect(task.id, reduced, resumed) {
        phase.snapTo(HomeTokens.phase(task.id))
        if (!reduced && resumed) while (true) {
            withFrameNanos { } // Yield even if animator scale changes to zero before this effect is cancelled.
            phase.animateTo(phase.value + 360f, tween(HomeTokens.period(task.id), easing = LinearEasing))
        }
    }
    val appearance = remember { Animatable(if (reduced) 1f else HomeTokens.recededScale) }
    LaunchedEffect(completing, reduced, resumed) {
        if (reduced || !resumed) appearance.snapTo(if (completing) HomeTokens.recededScale else 1f)
        else if (completing) appearance.animateTo(HomeTokens.recededScale, tween(HomeTokens.completeMillis))
        else { appearance.animateTo(HomeTokens.addOvershoot, tween(Motion.short)); appearance.animateTo(1f, tween(Motion.short)) }
    }
    val alpha by animateFloatAsState(if (completing) HomeTokens.recededAlpha else 1f,
        if (!resumed) snap() else tween(if (reduced) Motion.fast else HomeTokens.completeMillis), label = "completion opacity")
    val down by animateDpAsState(if (completing && !reduced && resumed) HomeTokens.completionTravel else 0.dp,
        if (!resumed) snap() else tween(if (reduced) Motion.fast else HomeTokens.completeMillis), label = "completion travel")
    val amplitude = minOf(driftLimit, if (mode == HomeMode.CALM) HomeTokens.calmDrift else HomeTokens.drift)
    Box(modifier.size(size).graphicsLayer {
        val angle = phase.value * PI.toFloat() / 180f
        val motion = if (!reduced && resumed && !completing) with(density) { amplitude.toPx() } else 0f
        translationX = sin(angle) * motion
        translationY = cos(angle) * motion + with(density) { down.toPx() }
        if (!reduced) { scaleX = appearance.value; scaleY = appearance.value }
        this.alpha = alpha
    }.clip(shape).background(if (focus) accent.color else accent.container)
        .border(if (focus) HomeTokens.focusRing else HomeTokens.outline,
            if (info.isOverdue || (focus && info.isUrgent)) colors.urgent.color
            else if (focus) MaterialTheme.colorScheme.onSurface.copy(alpha = .2f) else accent.color, shape)
        .pointerInput(task.id, focus, completing, onPeek, onComplete, onPin) {
            if (!completing) detectTapGestures(onTap = { point ->
                val distance = hypot(point.x - this.size.width / 2f, point.y - this.size.height / 2f)
                if (focus && distance > this.size.width * .40f) onPin() else onPeek()
            }, onLongPress = { onComplete() })
        }.semantics(mergeDescendants = true) {
            contentDescription = description; role = Role.Button
            if (focus) stateDescription = focusDescription
            if (completing) hideFromAccessibility() else {
                onClick { onPeek(); true }
                onLongClick(completeLabel) { onComplete(); true }
                customActions = listOf(CustomAccessibilityAction(pinLabel) { onPin(); true },
                    CustomAccessibilityAction(completeLabel) { onComplete(); true })
            }
        }.testTag("bubble-${task.id}"), contentAlignment = Alignment.Center) {
        val ink = if (focus) accent.ink else MaterialTheme.colorScheme.onSurface
        Column(Modifier.padding(Spacing.space3).clearAndSetSemantics {},
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.space1)) {
            if (focus) Text(stringResource(if (pinned) R.string.home_focus_pinned else R.string.home_focus),
                color = ink, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (info.tier == BubbleTier.S && !focus && density.fontScale >= 2f) {
                Canvas(Modifier.size(Spacing.space6)) { drawCircle(ink) }
            } else Text(if (info.tier == BubbleTier.S && !focus) task.title.substringBefore(' ').let { if (it.length > 8) stringResource(R.string.home_short_title, it.take(8)) else it } else task.title,
                color = ink, textAlign = TextAlign.Center,
                maxLines = if (focus && size >= HomeTokens.focusTitleLargeMinimum && density.fontScale < 1.3f) 3 else if (size < Spacing.returnHandle) 1 else 2,
                overflow = TextOverflow.Ellipsis,
                style = when {
                    focus && size >= HomeTokens.focusTitleLargeMinimum && density.fontScale < 1.3f -> MaterialTheme.typography.titleLarge
                    focus -> MaterialTheme.typography.titleMedium
                    info.tier in listOf(BubbleTier.XL, BubbleTier.L) -> MaterialTheme.typography.titleMedium
                    else -> MaterialTheme.typography.titleSmall
                })
            if (info.tier in listOf(BubbleTier.XL, BubbleTier.L) && size >= HomeTokens.metadataMinimum)
                Text(deadline, color = ink, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)

        }
            if (task.priority == Priority.HIGH && info.tier in listOf(BubbleTier.XL, BubbleTier.L)) {
                Canvas(Modifier.align(Alignment.TopEnd).padding(top = Spacing.space2, end = Spacing.space3).size(Spacing.space3)) {
                    drawLine(colors.urgent.color, androidx.compose.ui.geometry.Offset.Zero,
                        androidx.compose.ui.geometry.Offset(0f, this.size.height), strokeWidth = HomeTokens.outline.toPx())
                    drawPath(androidx.compose.ui.graphics.Path().apply {
                        moveTo(0f, 0f); lineTo(this@Canvas.size.width, this@Canvas.size.height / 3)
                        lineTo(0f, this@Canvas.size.height * .6f); close()
                    }, colors.urgent.color)
                }
            }
            if (task.status == TaskStatus.IN_PROGRESS) Canvas(Modifier.align(Alignment.BottomCenter).padding(bottom = Spacing.space1).size(Spacing.space2)) {
                drawArc(ink, 90f, 180f, useCenter = true)
            }
    }
}
