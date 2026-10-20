package id.riko.hub.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import id.riko.hub.navigation.Destination
import id.riko.hub.ui.theme.RikoHubTheme
import org.junit.Rule
import org.junit.Test

class RikoDockTest {
    @get:Rule val rule = createComposeRule()

    @Test fun fixed_five_slot_dock_follows_selected_destination() {
        var current by mutableStateOf(Destination.Home)
        rule.setContent { RikoHubTheme { RikoDock(current, false) {} } }
        rule.onNodeWithTag("dock-Home").assertIsSelected()
        rule.onNodeWithTag("dock-Timeline").assertIsNotSelected()
        rule.runOnUiThread { current = Destination.Timeline }
        rule.onNodeWithTag("dock-Home").assertIsNotSelected()
        rule.onNodeWithTag("dock-Timeline").assertIsSelected()
    }
}
