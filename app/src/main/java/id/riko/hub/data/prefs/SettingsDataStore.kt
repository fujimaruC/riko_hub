package id.riko.hub.data.prefs

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import id.riko.hub.domain.model.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.LocalDate

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsDataStore(private val store: DataStore<Preferences>) {
    constructor(context: Context) : this(context.applicationContext.settingsDataStore)
    private val theme = stringPreferencesKey("theme_mode")
    private val name = stringPreferencesKey("display_name")
    private val navigation = stringPreferencesKey("nav_style")
    private val hints = intPreferencesKey("hints_shown_count")
    private val pinId = longPreferencesKey("pinned_focus_task_id")
    private val pinDate = stringPreferencesKey("pinned_focus_date")

    data class BackupValues(val theme: String, val displayName: String, val navigation: String, val hints: Int, val pinnedFocusSyncId: String?, val pinnedFocusDate: String?)

    suspend fun backupValues(taskSyncIds: Map<Long, String>): BackupValues {
        val values = store.data.first()
        return BackupValues(values[theme] ?: "SYSTEM", values[name] ?: "Riko", values[navigation] ?: "SPATIAL_COMPASS", values[hints] ?: 0, values[pinId]?.let(taskSyncIds::get), values[pinDate])
    }

    suspend fun restoreBackup(values: BackupValues, pinIdValue: Long?) {
        store.edit {
            it[theme] = values.theme
            it[name] = values.displayName
            it[navigation] = values.navigation
            it[hints] = values.hints
            if (pinIdValue != null && values.pinnedFocusDate != null) {
                it[pinId] = pinIdValue
                it[pinDate] = values.pinnedFocusDate
            } else { it.remove(pinId); it.remove(pinDate) }
        }
    }

    val settings = store.data.catch { error ->
        if (error !is IOException) throw error
        Log.e("SettingsDataStore", "Cannot read settings", error)
        emit(emptyPreferences())
    }.map { values ->
        val date = values[pinDate]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val id = values[pinId]?.takeIf { it > 0 && date != null }
        AppSettings(
            themeMode = ThemeMode.entries.firstOrNull { it.name == values[theme] } ?: ThemeMode.SYSTEM,
            displayName = values[name]?.takeIf { it.isNotBlank() } ?: "Riko",
            navStyle = NavigationStyle.entries.firstOrNull { it.name == values[navigation] } ?: NavigationStyle.SPATIAL_COMPASS,
            hintsShownCount = (values[hints] ?: 0).coerceAtLeast(0),
            pinnedFocusTaskId = id, pinnedFocusDate = if (id == null) null else date,
        )
    }

    suspend fun setTheme(mode: ThemeMode) { store.edit { it[theme] = mode.name } }
    suspend fun setDisplayName(value: String) {
        require(value.isNotBlank()) { "Display name must not be blank" }
        store.edit { it[name] = value.trim() }
    }
    suspend fun setNavigationStyle(value: NavigationStyle) { store.edit { it[navigation] = value.name } }
    suspend fun setHintsShownCount(value: Int) {
        require(value >= 0) { "Hint count must not be negative" }
        store.edit { it[hints] = value }
    }
    suspend fun recordAppLaunch() {
        store.edit { values -> values[hints] = ((values[hints] ?: 0).coerceIn(0, 3) + 1).coerceAtMost(4) }
    }
    suspend fun clearPinnedFocusIfMatches(id: Long, date: LocalDate?) {
        store.edit {
            if (it[pinId] == id && it[pinDate] == date?.toString()) {
                it.remove(pinId)
                it.remove(pinDate)
            }
        }
    }

    suspend fun setPinnedFocus(id: Long?, date: LocalDate?) {
        require((id == null && date == null) || (id != null && id > 0 && date != null)) { "Pin requires task and date" }
        store.edit {
            if (id == null) { it.remove(pinId); it.remove(pinDate) }
            else { it[pinId] = id; it[pinDate] = requireNotNull(date).toString() }
        }
    }
}
