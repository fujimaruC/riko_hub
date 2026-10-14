package id.riko.hub.ui.home

import androidx.compose.foundation.shape.GenericShape

internal val TodayFocusContour = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    moveTo(0f, 32f)
    cubicTo(0f, 4f, w * .14f, 0f, w * .3f, 0f)
    lineTo(w - 28f, 0f)
    quadraticBezierTo(w, 0f, w, 28f)
    lineTo(w, h - 44f)
    cubicTo(w, h, w * .84f, h - 10f, w * .68f, h - 5f)
    cubicTo(w * .4f, h + 12f, w * .2f, h - 12f, 24f, h - 8f)
    quadraticBezierTo(0f, h - 8f, 0f, h - 32f)
    close()
}
