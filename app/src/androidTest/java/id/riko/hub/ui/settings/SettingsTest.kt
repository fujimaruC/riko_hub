package id.riko.hub.ui.settings

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import id.riko.hub.data.prefs.SettingsDataStore
import id.riko.hub.domain.model.AppSettings
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

    @Test fun preference_values_persist() {
        var current by mutableStateOf(initial)
        rule.setContent {
            val scope = rememberCoroutineScope()
            RikoHubTheme {
                SettingsScreen(current,
                    onTheme = { value -> scope.launch { preferences.setTheme(value); current = preferences.settings.first() } },
                    onDisplayName = { value -> scope.launch { preferences.setDisplayName(value); current = preferences.settings.first() } },
                )
            }
        }

        rule.onNodeWithTag("setting-display-name").performScrollTo().performTextClearance()
        rule.onNodeWithTag("setting-save-name").performScrollTo().performClick()
        rule.onNodeWithText("Nama tidak boleh kosong.").assertIsDisplayed()
        rule.onNodeWithTag("setting-display-name").performScrollTo().performTextInput("Miyu")
        rule.onNodeWithTag("setting-save-name").performScrollTo().performClick()
        rule.onNodeWithTag("setting-theme-DARK").performScrollTo().performClick()
        rule.waitUntil(5_000) {
            runBlocking {
                val saved = preferences.settings.first()
                saved.displayName == "Miyu" && saved.themeMode == ThemeMode.DARK
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

    }

    @Test fun new_preferences_default_dark_and_explicit_theme_modes_remain_available() = runBlocking {
        assertEquals(ThemeMode.DARK, initial.themeMode)
        ThemeMode.entries.forEach { mode ->
            preferences.setTheme(mode)
            assertEquals(mode, preferences.settings.first().themeMode)
        }
    }

}
