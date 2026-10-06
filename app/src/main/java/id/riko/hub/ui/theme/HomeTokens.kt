package id.riko.hub.ui.theme

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object HomeTokens {
    val calmLight = Color(0xFFEAF6EF)
    val calmDark = Color(0xFF1B2A27)
    val denseLight = Color(0xFFFFF1EC)
    val denseDark = Color(0xFF2A2022)
    val outline = 1.5.dp
    val focusRing = 3.dp
    val metadataMinimum = 128.dp
    val minimumLargeTextBubble = 72.dp
    val drift = 3.dp
    val calmDrift = 2.dp
    val completionTravel = 24.dp
    val glowLight = 24.dp
    val glowDark = 20.dp
    const val denseWashAlpha = .55f
    const val completeMillis = 360
    const val denseMillis = 400
    const val recededAlpha = .35f
    const val recededScale = .6f
    const val springDamping = .75f
    const val addOvershoot = 1.05f
    fun variant(id: Long) = Math.floorMod(id, 4).toInt()
    fun phase(id: Long) = (Math.floorMod(id, 360) * 37 % 360).toFloat()
    fun period(id: Long) = 6000 + Math.floorMod(id, 5).toInt() * 1000
}

// Elliptical corner curves from the four CSS-style radius pairs in the token specification.
val HomeBlobShapes = listOf(
    listOf(.58f,.42f,.52f,.48f,.42f,.58f,.48f,.52f),
    listOf(.48f,.52f,.60f,.40f,.52f,.48f,.40f,.60f),
    listOf(.55f,.45f,.45f,.55f,.60f,.40f,.50f,.50f),
    listOf(.42f,.58f,.55f,.45f,.50f,.50f,.58f,.42f),
).map { r -> GenericShape { size, _ ->
    val w = size.width; val h = size.height
    // Adjacent CSS radii are normalized together so curves never cross.
    val scale = minOf(1f, 1f / (r[0]+r[2]), 1f / (r[4]+r[6]), 1f / (r[1]+r[7]), 1f / (r[3]+r[5]))
    val a=r.map { it * scale }; val k=.5522848f
    moveTo(a[0]*w,0f); lineTo(w-a[2]*w,0f)
    cubicTo(w-a[2]*w*(1-k),0f,w,a[3]*h*(1-k),w,a[3]*h)
    lineTo(w,h-a[5]*h); cubicTo(w,h-a[5]*h*(1-k),w-a[4]*w*(1-k),h,w-a[4]*w,h)
    lineTo(a[6]*w,h); cubicTo(a[6]*w*(1-k),h,0f,h-a[7]*h*(1-k),0f,h-a[7]*h)
    lineTo(0f,a[1]*h); cubicTo(0f,a[1]*h*(1-k),a[0]*w*(1-k),0f,a[0]*w,0f); close()
} }
