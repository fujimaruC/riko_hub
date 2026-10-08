package id.riko.hub.ui

import id.riko.hub.navigation.SettingsWriteFailure
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsWriteTrackerTest {
    @Test fun unrelated_writes_preserve_failures_and_same_preference_write_invalidates_retry() {
        val writes = SettingsWriteTracker()
        val nameGeneration = writes.start(SettingsPreference.DISPLAY_NAME)
        var retries = 0
        val nameFailure = SettingsWriteFailure(1) {
            if (writes.isCurrent(SettingsPreference.DISPLAY_NAME, nameGeneration)) retries++
        }
        writes.failed(SettingsPreference.DISPLAY_NAME, nameGeneration, nameFailure)

        val themeGeneration = writes.start(SettingsPreference.THEME)
        val themeFailure = SettingsWriteFailure(2) {}
        writes.failed(SettingsPreference.THEME, themeGeneration, themeFailure)
        assertEquals(nameFailure, writes.earliestFailure())
        writes.succeeded(SettingsPreference.THEME, themeGeneration)
        assertEquals(nameFailure, writes.earliestFailure())

        val newerNameGeneration = writes.start(SettingsPreference.DISPLAY_NAME)
        nameFailure.retry()
        assertEquals(0, retries)
        writes.failed(SettingsPreference.DISPLAY_NAME, nameGeneration, nameFailure)
        assertEquals(null, writes.earliestFailure())
        assertEquals(true, writes.isCurrent(SettingsPreference.DISPLAY_NAME, newerNameGeneration))
    }

    @Test fun dismissing_failure_promotes_next_and_stale_id_preserves_new_failure() {
        val writes = SettingsWriteTracker()
        val nameGeneration = writes.start(SettingsPreference.DISPLAY_NAME)
        val nameFailure = SettingsWriteFailure(1) {}
        writes.failed(SettingsPreference.DISPLAY_NAME, nameGeneration, nameFailure)
        val themeGeneration = writes.start(SettingsPreference.THEME)
        val themeFailure = SettingsWriteFailure(2) {}
        writes.failed(SettingsPreference.THEME, themeGeneration, themeFailure)

        writes.dismiss(nameFailure.id)
        assertEquals(themeFailure, writes.earliestFailure())

        val newerThemeGeneration = writes.start(SettingsPreference.THEME)
        val newerThemeFailure = SettingsWriteFailure(3) {}
        writes.failed(SettingsPreference.THEME, newerThemeGeneration, newerThemeFailure)
        writes.dismiss(themeFailure.id)
        assertEquals(newerThemeFailure, writes.earliestFailure())
    }
}
