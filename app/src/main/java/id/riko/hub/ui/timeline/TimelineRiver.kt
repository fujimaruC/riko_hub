package id.riko.hub.ui.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import id.riko.hub.ui.theme.LocalRikoColors

// Shared endpoints keep the river continuous between independently measured lazy-list rows.
internal fun DrawScope.drawTimeRiver(colors: List<Color>, x: Float, bend: Float = 8.dp.toPx()) {
    val river = Path().apply {
        moveTo(x, 0f)
        cubicTo(x - bend, size.height * .22f, x + bend, size.height * .35f, x + bend, size.height * .5f)
        cubicTo(x + bend, size.height * .72f, x - bend, size.height * .85f, x, size.height)
    }
    val track = if (colors.size < 2) SolidColor(colors.firstOrNull() ?: Color.Transparent)
        else Brush.verticalGradient(colors, startY = 0f, endY = size.height)
    drawPath(river, track, style = Stroke(14.dp.toPx(), cap = StrokeCap.Round), alpha = .09f)
    drawPath(river, track, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round), alpha = .65f)
}

@Composable
internal fun TimelineRiverHorizon(color: Color, modifier: Modifier = Modifier) {
    val palette = LocalRikoColors.current
    Canvas(modifier.fillMaxWidth().height(24.dp)) {
        val river = Path().apply {
            moveTo(0f, size.height * .8f)
            cubicTo(size.width * .23f, -size.height * .4f, size.width * .32f, size.height * 1.4f,
                size.width * .57f, size.height * .45f)
            cubicTo(size.width * .75f, -size.height * .25f, size.width * .84f, size.height, size.width, size.height * .2f)
        }
        val track = Brush.linearGradient(listOf(palette.academic.color, palette.organization.color,
            palette.personal.color, palette.projects.color), Offset.Zero, Offset(size.width, size.height))
        drawPath(river, track, style = Stroke(20.dp.toPx()), alpha = .08f)
        drawPath(river, track, style = Stroke(2.dp.toPx()), alpha = .75f)
        drawCircle(color, 4.dp.toPx(), Offset(size.width * .57f, size.height * .45f))
    }
}
