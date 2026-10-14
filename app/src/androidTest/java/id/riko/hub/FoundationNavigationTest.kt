package id.riko.hub

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

    private fun back() = rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }

    private fun awaitScreen(route: String) {
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("screen-$route").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("screen-$route").assertIsDisplayed()
    }

    private fun openSpaces() {
        rule.onNodeWithTag("dock-Spaces").performClick()
        awaitScreen("Spaces")
    }

    @Test fun dock_visits_all_five_destinations() {
        listOf("Home", "Timeline", "Capture", "Vault", "Spaces").forEach { route ->
            rule.onNodeWithTag("dock-$route").performClick()
            awaitScreen(route)
        }
    }

    @Test fun secondary_destinations_remain_reachable() {
        rule.onNodeWithTag("home-all-tasks").performClick()
        awaitScreen("Tasks")
        rule.onNodeWithTag("dock-Home").performClick()
        awaitScreen("Home")

        openSpaces()
        rule.onNodeWithTag("space-portal-DevicesSpace").performScrollTo().performClick()
        awaitScreen("DevicesSpace")
        rule.onNodeWithTag("devices-open-sync").performClick()
        awaitScreen("Sync")
        back()
        awaitScreen("DevicesSpace")

        rule.onNodeWithTag("dock-Spaces").performClick()
        rule.onNodeWithTag("space-portal-AcademicsSpace").performScrollTo().performClick()
        awaitScreen("AcademicsSpace")
        rule.onNodeWithTag("academics-manage-courses").performClick()
        awaitScreen("Courses")
        back()
        awaitScreen("AcademicsSpace")
    }

    @Test fun settings_back_returns_to_home_and_theme_persists() {
        val settings = (rule.activity.application as RikoHubApp).container.settings.settings
        val original = runBlocking { settings.first().themeMode }
        try {
            rule.onNodeWithText("Pengaturan").performClick()
            rule.onNodeWithText("Gelap").performClick()
            rule.waitUntil(5_000) { runBlocking { settings.first().themeMode == ThemeMode.DARK } }
            rule.activityRule.scenario.recreate()
            rule.onNodeWithTag("setting-theme-DARK").assertIsDisplayed()
            rule.onNodeWithText("Terang").performClick()
            rule.waitUntil(5_000) { runBlocking { settings.first().themeMode == ThemeMode.LIGHT } }
            back()
            awaitScreen("Home")
        } finally {
            val label = when (original) {
                ThemeMode.SYSTEM -> "Ikuti sistem"
                ThemeMode.LIGHT -> "Terang"
                ThemeMode.DARK -> "Gelap"
            }
            if (rule.onAllNodesWithTag("screen-Home").fetchSemanticsNodes().isEmpty()) back()
            rule.onNodeWithText("Pengaturan").performClick()
            rule.onNodeWithText(label).performClick()
            back()
        }
    }
}
