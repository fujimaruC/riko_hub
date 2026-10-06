package id.riko.hub.navigation

import org.junit.Assert.*
import org.junit.Test

class SpatialMapTest {
    private fun swipe(from: Destination = Destination.Home, x: Float = 0f, y: Float = 0f, edge: Boolean = false) =
        SpatialMap.swipeTarget(from, x, y, edge, 64f)

    @Test fun home_swipes_pull_each_neighbor_into_view() {
        assertEquals(Destination.Tasks, swipe(y = -100f))
        assertEquals(Destination.Timeline, swipe(y = 100f))
        assertEquals(Destination.Vault, swipe(x = -100f))
        assertEquals(Destination.Sync, swipe(x = 100f))
    }
    @Test fun horizontal_edge_gestures_stay_with_android() {
        assertNull(swipe(x = 100f, edge = true))
        assertNull(swipe(x = -100f, edge = true))
        assertEquals(Destination.Tasks, swipe(y = -100f, edge = true))
    }
    @Test fun taps_short_drags_and_ambiguous_diagonals_do_not_navigate() {
        assertNull(swipe())
        assertNull(swipe(x = 63f))
        assertNull(swipe(x = 100f, y = 100f))
        assertEquals(Destination.Vault, swipe(x = -64f))
    }
    @Test fun only_the_opposite_return_gesture_reaches_home() {
        assertEquals(Destination.Home, swipe(Destination.Tasks, y = 100f))
        assertEquals(Destination.Home, swipe(Destination.Timeline, y = -100f))
        assertEquals(Destination.Home, swipe(Destination.Vault, x = 100f))
        assertEquals(Destination.Home, swipe(Destination.Sync, x = -100f))
        assertNull(swipe(Destination.Tasks, y = -100f))
        assertNull(swipe(Destination.Settings, x = 100f))
    }
    @Test fun held_compass_selects_with_a_short_drag() {
        assertEquals(Destination.Home, SpatialMap.compassTarget(0f, 0f, 28f))
        assertEquals(Destination.Vault, SpatialMap.compassTarget(29f, 0f, 28f))
        assertEquals(Destination.Sync, SpatialMap.compassTarget(-29f, 0f, 28f))
        assertEquals(Destination.Timeline, SpatialMap.compassTarget(0f, -29f, 28f))
        assertEquals(Destination.Tasks, SpatialMap.compassTarget(0f, 29f, 28f))
    }
    @Test fun settings_is_outside_the_spatial_map() {
        assertNull(SpatialMap.position(Destination.Settings))
        assertEquals(SpatialMap.Position(0, 0), SpatialMap.position(Destination.Home))
        assertEquals(SpatialMap.Position(0, 1), SpatialMap.position(Destination.Tasks))
        assertEquals(SpatialMap.Position(0, -1), SpatialMap.position(Destination.Timeline))
        assertEquals(SpatialMap.Position(1, 0), SpatialMap.position(Destination.Vault))
        assertEquals(SpatialMap.Position(-1, 0), SpatialMap.position(Destination.Sync))
    }
}
