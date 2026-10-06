package id.riko.hub.navigation

import kotlin.math.abs

object SpatialMap {
    data class Position(val x: Int, val y: Int)
    fun position(destination: Destination): Position? = when (destination) {
        Destination.Home -> Position(0, 0)
        Destination.Tasks -> Position(0, 1)
        Destination.Timeline -> Position(0, -1)
        Destination.Vault -> Position(1, 0)
        Destination.Sync -> Position(-1, 0)
        Destination.Settings -> null
    }

    /** The held compass selects a neighboring space with a short drag; its center returns Home. */
    fun compassTarget(dx: Float, dy: Float, deadZone: Float): Destination = when {
        abs(dx) < deadZone && abs(dy) < deadZone -> Destination.Home
        abs(dx) > abs(dy) -> if (dx > 0) Destination.Vault else Destination.Sync
        else -> if (dy > 0) Destination.Tasks else Destination.Timeline
    }

    /** Distances are pixels; the caller converts the token thresholds using density. */
    fun swipeTarget(
        from: Destination, dx: Float, dy: Float, startsAtEdge: Boolean, threshold: Float,
    ): Destination? {
        val horizontal = abs(dx) >= threshold && abs(dx) > abs(dy) * 1.2f
        val vertical = abs(dy) >= threshold && abs(dy) > abs(dx) * 1.2f
        if (horizontal && startsAtEdge) return null
        val pulled = when {
            horizontal && dx < 0 -> Destination.Vault
            horizontal -> Destination.Sync
            vertical && dy < 0 -> Destination.Tasks
            vertical -> Destination.Timeline
            else -> return null
        }
        if (from == Destination.Home) return pulled
        return when {
            from == Destination.Tasks && pulled == Destination.Timeline -> Destination.Home
            from == Destination.Timeline && pulled == Destination.Tasks -> Destination.Home
            from == Destination.Vault && pulled == Destination.Sync -> Destination.Home
            from == Destination.Sync && pulled == Destination.Vault -> Destination.Home
            else -> null
        }
    }
}
