package id.riko.hub.domain.logic

import kotlin.math.hypot
import kotlin.math.min

data class BubbleSlot(val x: Double, val y: Double, val maxDiameter: Double)
data class BubblePlacement(val urgency: UrgencyInfo?, val x: Double, val y: Double, val diameter: Double, val overflow: Int = 0) {
    val id get() = urgency?.task?.id ?: -1L
}

object BubbleLayoutPlanner {
    const val HEIGHT_RATIO = .95
    // Two corrected coordinates: see STATUS. Diameters and all other slots retain the specification.
    val slots = listOf(BubbleSlot(.34, .30, .54), BubbleSlot(.76, .42, .40),
        BubbleSlot(.27, .74, .38), BubbleSlot(.71, .80, .30), BubbleSlot(.86, .13, .22),
        BubbleSlot(.495, .894, .20), BubbleSlot(.09, .542, .18))

    fun plan(ranked: List<UrgencyInfo>, focus: Long?, mode: HomeMode): List<BubblePlacement> {
        val ordered = ranked.sortedBy { if (it.task.id == focus) 0 else 1 }
        val visible = if (ordered.size > 7) ordered.take(6) else ordered
        return visible.mapIndexed { index, item ->
            val slot = slots[index]
            val tierSize = if (index == 0) maxOf(.40, item.tier.diameter) else item.tier.diameter
            BubblePlacement(item, slot.x, slot.y, min(tierSize, slot.maxDiameter) * mode.scale)
        } + if (ordered.size > 7) listOf(BubblePlacement(null, slots[6].x, slots[6].y,
            min(BubbleTier.S.diameter, slots[6].maxDiameter) * mode.scale, ordered.size - 6)) else emptyList()
    }

    fun largeTextPlan(ranked: List<UrgencyInfo>, focus: Long?): List<BubblePlacement> {
        val ordered = ranked.sortedBy { if (it.task.id == focus) 0 else 1 }
        return ordered.take(3).mapIndexed { i, item ->
            BubblePlacement(item, .5, .5, listOf(.85, .80, .82)[i])
        } + if (ordered.size > 3) listOf(BubblePlacement(null, .5, .5, .4, ordered.size - 3)) else emptyList()
    }

    // The UI caps this normalized allowance at the 2/3dp motion token.
    fun driftAllowance(item: BubblePlacement, all: List<BubblePlacement>): Double {
        val r = item.diameter / 2
        val edge = minOf(item.x-r, 1-item.x-r, item.y*HEIGHT_RATIO-r, HEIGHT_RATIO-item.y*HEIGHT_RATIO-r)
        val pair = all.filter { it.id != item.id }.minOfOrNull {
            val distance = hypot(item.x-it.x, (item.y-it.y)*HEIGHT_RATIO)
            (distance - (item.diameter+it.diameter)/2 + .12*minOf(item.diameter,it.diameter)) / (2*kotlin.math.sqrt(2.0))
        } ?: edge
        return minOf(edge, pair).coerceAtLeast(0.0)
    }

    fun withinBounds(p: BubblePlacement): Boolean = p.x - p.diameter / 2 >= -1e-9 &&
        p.x + p.diameter / 2 <= 1 + 1e-9 && p.y * HEIGHT_RATIO - p.diameter / 2 >= -1e-9 &&
        p.y * HEIGHT_RATIO + p.diameter / 2 <= HEIGHT_RATIO + 1e-9

    fun overlap(a: BubblePlacement, b: BubblePlacement): Double =
        maxOf(0.0, (a.diameter + b.diameter) / 2 - hypot(a.x - b.x, (a.y - b.y) * HEIGHT_RATIO))
}
