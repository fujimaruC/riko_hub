package id.riko.hub

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class SpacesNavigationTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun all_four_spaces_return_to_landing_then_home() {
        openSpaces()
        listOf("AcademicsSpace", "ProjectsSpace", "PersonalSpace", "DevicesSpace").forEach { route ->
            rule.onNodeWithTag("space-portal-$route").performScrollTo().performClick()
            awaitScreen(route)
            systemBack()
            awaitScreen("Spaces")
        }
        systemBack()
        awaitScreen("Home")
        openSpaces()
        awaitScreen("Spaces")
        systemBack()
        awaitScreen("Home")
    }

    @Test fun existing_courses_and_sync_routes_remain_available_from_spaces() {
        listOf(
            Triple("AcademicsSpace", "academics-manage-courses", "Courses"),
            Triple("DevicesSpace", "devices-open-sync", "Sync"),
        ).forEach { (space, actionTag, existingRoute) ->
            openSpaces()
            rule.onNodeWithTag("space-portal-$space").performScrollTo().performClick()
            awaitScreen(space)
            rule.onNodeWithTag(actionTag).performScrollTo().performClick()
            awaitScreen(existingRoute)
            systemBack()
            awaitScreen(space)
        }
    }

    private fun openSpaces() {
        rule.onNodeWithTag("dock-Spaces").performClick()
        awaitScreen("Spaces")
    }

    private fun systemBack() = rule.runOnUiThread {
        rule.activity.onBackPressedDispatcher.onBackPressed()
    }

    private fun awaitScreen(route: String) {
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("screen-$route").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("screen-$route").assertIsDisplayed()
    }
}
