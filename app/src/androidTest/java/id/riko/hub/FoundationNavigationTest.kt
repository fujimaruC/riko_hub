package id.riko.hub

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import id.riko.hub.RikoHubApp
import id.riko.hub.ui.theme.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class FoundationNavigationTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun backHome() {
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        assertScreen("Home")
    }

    private fun assertScreen(route: String) {
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("screen-$route").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("screen-$route").assertIsDisplayed()
    }

    private fun awaitTheme(mode: ThemeMode, label: String) {
        rule.waitUntil(5_000) {
            runBlocking { (rule.activity.application as RikoHubApp).container.settings.settings.first().themeMode == mode } &&
                rule.onAllNodes(hasText(label) and isSelected()).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test fun compass_visits_all_neighbors_and_back_returns_home() {
        listOf("Tugas" to "Tasks", "Timeline" to "Timeline", "Vault" to "Vault", "Sync" to "Sync").forEach { (label, route) ->
            rule.onNodeWithTag("compass-ball").performClick()
            rule.onNodeWithText(label).performClick()
            assertScreen(route)
            backHome()
        }
    }

    @Test fun four_home_swipes_reach_the_expected_destinations() {
        val swipes: List<Pair<String, TouchInjectionScope.() -> Unit>> = listOf(
            "Tasks" to { swipeUp() }, "Timeline" to { swipeDown() },
            "Vault" to { swipeLeft(startX = width * 0.75f, endX = width * 0.25f) },
            "Sync" to { swipeRight(startX = width * 0.25f, endX = width * 0.75f) },
        )
        swipes.forEach { (route, gesture) ->
            rule.onNodeWithTag("home-field").performTouchInput(gesture)
            assertScreen(route)
            backHome()
        }
    }

    @Test fun return_handle_accepts_each_opposite_swipe_and_tap() {
        val returns: List<Pair<String, TouchInjectionScope.() -> Unit>> = listOf(
            "Tugas" to { swipeDown(startY = height * 0.05f, endY = height * 0.95f) },
            "Timeline" to { swipeUp(startY = height * 0.95f, endY = height * 0.05f) },
            "Vault" to { swipeRight() }, "Sync" to { swipeLeft() },
        )
        returns.forEach { (label, gesture) ->
            rule.onNodeWithTag("compass-ball").performClick()
            rule.onNodeWithText(label).performClick()
            rule.onNodeWithTag("return-handle").performTouchInput(gesture)
            assertScreen("Home")
            rule.onNodeWithTag("compass-ball").performClick()
            rule.onNodeWithText(label).performClick()
            rule.onNodeWithText("Kembali ke Hari Ini").performClick()
            assertScreen("Home")
        }
    }

    @Test fun held_compass_drag_navigates_with_a_short_move() {
        rule.onNodeWithTag("compass-ball").performTouchInput {
            down(center)
            advanceEventTime(650)
            moveBy(Offset(80f, 0f))
            up()
        }
        rule.onNodeWithTag("screen-Vault").assertIsDisplayed()
        backHome()
    }

    @Test fun horizontal_drag_from_screen_edge_does_not_navigate() {
        rule.onNodeWithTag("home-field").performTouchInput {
            swipe(Offset(1f, centerY), Offset(width * 0.75f, centerY))
        }
        rule.onNodeWithTag("screen-Home").assertIsDisplayed()
    }

    @Test fun theme_selection_survives_activity_recreation_and_settings_back_returns_home() {
        val settings = (rule.activity.application as RikoHubApp).container.settings.settings
        val original = runBlocking { settings.first().themeMode }
        try {
            rule.onNodeWithText("Pengaturan").performClick()
            rule.onNodeWithText("Gelap").performClick()
            awaitTheme(ThemeMode.DARK, "Gelap")
            rule.activityRule.scenario.recreate()
            awaitTheme(ThemeMode.DARK, "Gelap")
            rule.onNodeWithText("Terang").performClick()
            awaitTheme(ThemeMode.LIGHT, "Terang")
            backHome()
        } finally {
            rule.waitForIdle()
            if (rule.onAllNodesWithTag("setting-theme-DARK").fetchSemanticsNodes().isEmpty()) {
                if (rule.onAllNodesWithTag("screen-Home").fetchSemanticsNodes().isEmpty())
                    rule.onNodeWithText("Kembali ke Hari Ini").performClick()
                rule.onNodeWithText("Pengaturan").performClick()
            }
            val label = when (original) {
                ThemeMode.SYSTEM -> "Ikuti sistem"
                ThemeMode.LIGHT -> "Terang"
                ThemeMode.DARK -> "Gelap"
            }
            rule.onNodeWithText(label).performClick()
            awaitTheme(original, label)
            backHome()
        }
    }
}
