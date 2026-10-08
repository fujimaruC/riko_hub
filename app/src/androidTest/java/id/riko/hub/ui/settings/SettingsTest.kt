package id.riko.hub.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.material3.Text
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import id.riko.hub.data.prefs.SettingsDataStore
import id.riko.hub.domain.model.AppSettings
import id.riko.hub.domain.model.NavigationStyle
import id.riko.hub.navigation.Destination
import id.riko.hub.ui.compass.LocalNavigationStyle
import id.riko.hub.ui.compass.SpatialNavLayer
import id.riko.hub.ui.compass.CompassChip
import id.riko.hub.ui.home.edgeHintAlpha
import id.riko.hub.ui.theme.RikoHubTheme
import id.riko.hub.ui.theme.ThemeMode
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SettingsTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var preferences: SettingsDataStore
    private lateinit var backingStore: DataStore<Preferences>
    private lateinit var settingsScope: CoroutineScope
    private lateinit var settingsFile: File
    private lateinit var initial: AppSettings
    private val showGestures = mutableStateOf(false)
    private val destination = mutableStateOf(Destination.Home)
    private val navigationStyle = mutableStateOf(NavigationStyle.SPATIAL_COMPASS)

    @Before fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        settingsFile = File(context.cacheDir, "settings-test.preferences_pb")
        settingsFile.delete()
        settingsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        backingStore = PreferenceDataStoreFactory.create(
            scope = settingsScope,
            produceFile = { settingsFile },
        )
        preferences = SettingsDataStore(backingStore)
        initial = runBlocking { preferences.settings.first() }
    }

    @After fun cleanIsolatedPreferences() {
        if (::settingsScope.isInitialized) runBlocking { settingsScope.coroutineContext.job.cancelAndJoin() }
        if (::settingsFile.isInitialized) {
            settingsFile.parentFile?.listFiles { _, name -> name.startsWith(settingsFile.name) }?.forEach { it.delete() }
        }
    }

    @Test fun preference_values_persist_and_compass_only_disables_home_and_return_swipes() {
        var current by mutableStateOf(initial)
        rule.setContent {
            val scope = rememberCoroutineScope()
            RikoHubTheme {
                if (!showGestures.value) {
                    SettingsScreen(current,
                        onTheme = { value -> scope.launch { preferences.setTheme(value); current = preferences.settings.first() } },
                        onDisplayName = { value -> scope.launch { preferences.setDisplayName(value); current = preferences.settings.first() } },
                        onNavigationStyle = { value -> scope.launch { preferences.setNavigationStyle(value); current = preferences.settings.first() } },
                        onNavigate = {},
                    )
                } else CompositionLocalProvider(LocalNavigationStyle provides navigationStyle.value) {
                    Box(Modifier.fillMaxSize()) {
                        Column {
                            SpatialNavLayer(Destination.Home, { destination.value = it }, Modifier.fillMaxWidth().height(300.dp).testTag("home-gesture")) { Text("Home") }
                            SpatialNavLayer(Destination.Tasks, { destination.value = it }, Modifier.fillMaxWidth().height(300.dp).testTag("return-gesture")) { Text("Tasks") }
                        }
                        Box(Modifier.align(Alignment.BottomEnd)) {
                            CompassChip(Destination.Home, reducedMotion = true, onNavigate = { destination.value = it })
                        }
                    }
                }
            }
        }

        rule.onNodeWithTag("setting-display-name").performScrollTo().performTextClearance()
        rule.onNodeWithTag("setting-save-name").performScrollTo().performClick()
        rule.onNodeWithText("Nama tidak boleh kosong.").assertIsDisplayed()
        rule.onNodeWithTag("setting-display-name").performScrollTo().performTextInput("Miyu")
        rule.onNodeWithTag("setting-save-name").performScrollTo().performClick()
        rule.onNodeWithTag("setting-theme-DARK").performScrollTo().performClick()
        rule.onNodeWithTag("setting-navigation-COMPASS_ONLY").performScrollTo().performClick()
        rule.waitUntil(5_000) {
            runBlocking {
                val saved = preferences.settings.first()
                saved.displayName == "Miyu" && saved.themeMode == ThemeMode.DARK && saved.navStyle == NavigationStyle.COMPASS_ONLY
            }
        }

        // Reopen the file to prove disk persistence rather than the same store's cached state.
        runBlocking { settingsScope.coroutineContext.job.cancelAndJoin() }
        settingsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        backingStore = PreferenceDataStoreFactory.create(scope = settingsScope, produceFile = { settingsFile })
        val recreated = SettingsDataStore(backingStore)
        val saved = runBlocking { recreated.settings.first() }
        assertEquals("Miyu", saved.displayName)
        assertEquals(ThemeMode.DARK, saved.themeMode)
        assertEquals(NavigationStyle.COMPASS_ONLY, saved.navStyle)
        assertEquals(1f, edgeHintAlpha(1), 0f)
        assertEquals(1f, edgeHintAlpha(3), 0f)
        assertEquals(0.55f, edgeHintAlpha(4), 0f)
        runBlocking {
            recreated.setHintsShownCount(0)
            recreated.recordAppLaunch()
            assertEquals(1, recreated.settings.first().hintsShownCount)
            repeat(2) { recreated.recordAppLaunch() }
            assertEquals(3, recreated.settings.first().hintsShownCount)
            recreated.recordAppLaunch()
            assertEquals(4, recreated.settings.first().hintsShownCount)
            repeat(2) { recreated.recordAppLaunch() }
        }
        assertEquals(4, runBlocking { recreated.settings.first().hintsShownCount })

        rule.runOnIdle { showGestures.value = true; navigationStyle.value = NavigationStyle.SPATIAL_COMPASS }
        rule.onNodeWithTag("home-gesture").performTouchInput { swipeUp() }
        assertEquals(Destination.Tasks, destination.value)
        rule.onNodeWithTag("return-gesture").performTouchInput { swipeDown() }
        assertEquals(Destination.Home, destination.value)
        rule.runOnIdle { navigationStyle.value = NavigationStyle.COMPASS_ONLY }
        rule.onNodeWithTag("home-gesture").performTouchInput { swipeUp() }
        assertEquals(Destination.Home, destination.value)
        rule.runOnIdle { destination.value = Destination.Tasks }
        rule.onNodeWithTag("return-gesture").performTouchInput { swipeDown() }
        assertEquals(Destination.Tasks, destination.value)
        rule.runOnIdle { destination.value = Destination.Home }
        rule.onNodeWithTag("compass-ball").performClick()
        rule.onNodeWithText("Tugas").performClick()
        assertEquals(Destination.Tasks, destination.value)
    }

}
