package id.riko.hub.ui.courses

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import id.riko.hub.core.time.Clock
import id.riko.hub.data.local.RikoDatabase
import id.riko.hub.data.local.entity.toDomain
import id.riko.hub.data.repository.CourseRepository
import id.riko.hub.ui.theme.RikoHubTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull

class CoursesTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val clock = object : Clock {
        override fun now() = Instant.parse("2026-10-06T04:00:00Z").toEpochMilli()
        override fun zone() = ZoneId.of("Asia/Makassar")
    }
    private lateinit var database: RikoDatabase
    private var databaseClosed = false
    private lateinit var model: CoursesViewModel
    private val store = ViewModelStore()

    @Before fun setup() {
        rule.runOnUiThread { rule.activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
        database = Room.inMemoryDatabaseBuilder(context, RikoDatabase::class.java).allowMainThreadQueries().build()
        databaseClosed = false
        model = CoursesViewModel(CourseRepository(database, clock))
        store.put("courses", model)
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density)) {
                RikoHubTheme { Surface { CoursesRoute(model) {} } }
            }
        }
        rule.waitUntil(5_000) { !model.state.value.loading }
    }

    @After fun cleanup() {
        rule.runOnIdle { store.clear() }
        if (!databaseClosed) database.close()
    }

    private fun courses() = runBlocking { database.courses().observeAll().first().map { it.toDomain() } }
    private fun waitFor(condition: () -> Boolean) = rule.waitUntil(5_000, condition)
    private fun save() { rule.onNodeWithTag("course-save").performScrollTo().performClick() }

    @Test fun create_edit_archive_restore_and_back_from_editor_then_detail() {
        rule.onNodeWithText("Tambah mata kuliah").performClick()
        rule.onNodeWithTag("course-title").performTextInput("Fisika")
        save()
        waitFor { courses().size == 1 && model.state.value.editor == null }
        val id = courses().single().id
        assertEquals("Fisika", courses().single().title)

        rule.onNodeWithText("Edit mata kuliah").performClick()
        rule.onNodeWithTag("course-title").performScrollTo().performTextClearance()
        rule.onNodeWithTag("course-title").performTextInput("Fisika Dasar")
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        waitFor { model.state.value.editor == null }
        assertEquals(id, model.state.value.selectedId)

        rule.onNodeWithText("Edit mata kuliah").performClick()
        rule.onNodeWithTag("course-title").performScrollTo().performTextClearance()
        rule.onNodeWithTag("course-title").performTextInput("Fisika Dasar")
        save()
        waitFor { courses().single().title == "Fisika Dasar" && model.state.value.editor == null }
        rule.onNodeWithText("Arsipkan").performClick()
        waitFor { courses().single().archivedAt != null }
        assertNotNull(model.state.value.courses.first { it.id == id }.archivedAt)

        rule.onNodeWithText("Kembali ke daftar").performClick()
        rule.onNodeWithText("Lihat arsip").performClick()
        rule.onNodeWithText("Fisika Dasar").performClick()
        rule.onNodeWithText("Pulihkan").performClick()
        waitFor { courses().single().archivedAt == null }
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        waitFor { model.state.value.selectedId == null }
    }

    @Test fun failed_save_has_no_retry_action_that_can_skip_invalid_timetable_drafts() {
        rule.onNodeWithText("Tambah mata kuliah").performClick()
        rule.onNodeWithTag("course-title").performTextInput("Fisika")
        rule.onNodeWithText("Tambah jadwal").performScrollTo().performClick()
        rule.runOnUiThread {
            database.close()
            databaseClosed = true
        }
        save()
        waitFor {
            !model.state.value.busy && rule.onAllNodesWithText("Perubahan mata kuliah gagal disimpan.")
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Perubahan mata kuliah gagal disimpan.").assertExists()
        rule.onNodeWithTag("course-slot-0-start").performScrollTo().performTextClearance()
        rule.onNodeWithTag("course-slot-0-start").performTextInput("xx:xx")
        rule.onNodeWithTag("course-save").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithText("Coba lagi").assertDoesNotExist()
        assertEquals("Fisika", model.state.value.editor?.title)
    }
}
