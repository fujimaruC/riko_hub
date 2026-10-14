package id.riko.hub.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import id.riko.hub.navigation.Destination
import id.riko.hub.ui.theme.RikoHubTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RenaissanceDockTest {
    @get:Rule val rule = createComposeRule()

    @Test fun selection_lifts_between_tabs_and_reduced_motion_snaps() {
        var current by mutableStateOf(Destination.Home)
        var reducedMotion by mutableStateOf(false)
        rule.mainClock.autoAdvance = false
        rule.setContent {
            RikoHubTheme {
                RenaissanceDock(current, reducedMotion) {}
            }
        }
        rule.mainClock.advanceTimeByFrame()

        fun iconCenter(tag: String) = rule.onNodeWithTag(tag, useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot.center.y
        val homeStart = iconCenter("dock-icon-Home")
        val timelineStart = iconCenter("dock-icon-Timeline")

        rule.runOnUiThread { current = Destination.Timeline }
        rule.mainClock.advanceTimeBy(130)
        val homeMid = iconCenter("dock-icon-Home")
        val timelineMid = iconCenter("dock-icon-Timeline")
        assertTrue(homeMid > homeStart && homeMid < timelineStart)
        assertTrue(timelineMid < timelineStart && timelineMid > homeStart)

        rule.mainClock.advanceTimeBy(500)
        val homeEnd = iconCenter("dock-icon-Home")
        val timelineEnd = iconCenter("dock-icon-Timeline")
        assertTrue(homeEnd > homeMid)
        assertTrue(timelineEnd < timelineMid)

        rule.runOnUiThread { current = Destination.Home; reducedMotion = true }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        assertEquals(homeStart, iconCenter("dock-icon-Home"), 0.5f)
        assertEquals(timelineStart, iconCenter("dock-icon-Timeline"), 0.5f)
    }
}
