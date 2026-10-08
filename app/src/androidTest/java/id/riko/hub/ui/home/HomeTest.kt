package id.riko.hub.ui.home

import android.view.WindowManager
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import id.riko.hub.core.time.Clock
import id.riko.hub.data.local.RikoDatabase
import id.riko.hub.data.prefs.SettingsDataStore
import id.riko.hub.data.repository.*
import id.riko.hub.domain.logic.HomeMode
import id.riko.hub.domain.model.*
import id.riko.hub.navigation.Destination
import id.riko.hub.ui.theme.RikoHubTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.time.*
import java.io.File

class HomeTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private var now = Instant.parse("2026-10-06T06:00:00Z").toEpochMilli()
    private val clock = object : Clock {
        override fun now() = now
        override fun zone() = ZoneId.of("Asia/Makassar")
    }
    private lateinit var database: RikoDatabase
    private lateinit var tasks: TaskRepository
    private lateinit var model: HomeViewModel
    private lateinit var preferences: SettingsDataStore
    private lateinit var settingsScope: CoroutineScope
    private lateinit var settingsFile: File
    private val store = ViewModelStore()
    private val scale = mutableFloatStateOf(1f)
    private val dark = mutableStateOf(false)
    private var destination: Destination? = null

    @Before fun setup() {
        rule.runOnUiThread { rule.activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
        database = Room.inMemoryDatabaseBuilder(context, RikoDatabase::class.java).build()
        tasks = TaskRepository(database, clock)
        settingsFile = File(context.cacheDir, "home-test.preferences_pb")
        settingsFile.delete()
        settingsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        preferences = SettingsDataStore(PreferenceDataStoreFactory.create(
            scope = settingsScope, produceFile = { settingsFile },
        ))
        model = HomeViewModel(tasks, EventRepository(database, clock), preferences, clock)
        store.put("home", model)
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, scale.floatValue)) {
                RikoHubTheme(dark.value) { Surface { HomeRoute(model) { destination = it } } }
            }
        }
        waitFor { !model.state.value.loading }
    }
    @After fun cleanup() {
        rule.runOnIdle { store.clear() }
        database.close()
        runBlocking { settingsScope.coroutineContext[kotlinx.coroutines.Job]!!.cancelAndJoin() }
        settingsFile.parentFile?.listFiles { _, name -> name.startsWith(settingsFile.name) }?.forEach { it.delete() }
    }
    private fun waitFor(condition: () -> Boolean) = rule.waitUntil(10_000, condition)
    private fun all() = runBlocking { tasks.observeAll().first() }
    private fun seed(count: Int) = runBlocking {
        (1..count).map { tasks.upsert(Task(title = "Tugas $it", priority = Priority.HIGH, deadlineAt = now + 18_000_000)) }
    }

    @Test fun quick_add_long_press_completion_progress_and_undo_use_repository_flow() {
        rule.onNodeWithTag("home-add").performClick()
        rule.onNodeWithTag("task-save").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag("task-title").performScrollTo().performTextInput("Tugas baru")
        rule.onNodeWithText("Hari ini").performScrollTo().performClick()
        rule.onNodeWithTag("task-save").performScrollTo().performClick()
        waitFor { all().size == 1 && model.state.value.editor == null }
        val id = all().single().id
        rule.onNodeWithTag("bubble-$id").performScrollTo().assertHasClickAction()
        rule.onNodeWithTag("bubble-$id").performTouchInput { longClick() }
        waitFor { all().single().status == TaskStatus.DONE }
        rule.onNodeWithText("1 dari 1 selesai").assertExists()
        rule.onNodeWithText("Urungkan").performClick()
        waitFor { all().single().status == TaskStatus.NOT_STARTED }
        rule.onNodeWithTag("bubble-$id").assertExists()
    }

    @Test fun state_pin_completion_previous_status_undo_rollover_and_stop_resume() {
        val ids = seed(8)
        waitFor { model.state.value.snapshot?.ranked?.size == 8 }
        assertEquals(HomeMode.DENSE, model.state.value.snapshot!!.mode)
        assertEquals(2, model.state.value.snapshot!!.overflow)
        rule.runOnIdle { model.pin(ids.last()) }
        waitFor { model.state.value.snapshot?.focus == ids.last() }
        val selected = all().last()
        rule.runOnIdle { model.start(selected) }
        waitFor { all().last().status == TaskStatus.IN_PROGRESS }
        rule.runOnIdle { model.complete(all().last()) }
        waitFor { model.state.value.snapshot?.validPin == null && model.state.value.snapshot?.done == 1 }
        rule.runOnIdle { model.undo(selected.id, TaskStatus.IN_PROGRESS) }
        waitFor { all().last().status == TaskStatus.IN_PROGRESS }
        assertNull(all().last().completedAt)
        rule.runOnIdle { model.pin(ids.last()) }
        waitFor { model.state.value.snapshot?.validPin == ids.last() }
        rule.runOnIdle { model.setActive(false) }
        val previous = model.state.value.snapshot!!.now
        now += 86_400_000
        rule.runOnIdle { model.setActive(true) }
        waitFor { model.state.value.snapshot?.now != previous }
        assertNull(model.state.value.snapshot!!.validPin)
        assertEquals(LocalDate.of(2026,10,7), model.state.value.snapshot!!.today)
    }

    @Test fun peek_pin_and_large_text_overflow_are_operable_in_both_themes() {
        val ids = seed(8)
        waitFor { model.state.value.snapshot?.ranked?.size == 8 }
        rule.runOnIdle { scale.floatValue = 2f }
        listOf(false, true).forEach { night ->
            rule.runOnIdle { dark.value = night }
            rule.onNodeWithTag("bubble-${ids[0]}").performScrollTo().performClick()
            rule.onNodeWithText("Edit", substring = false).performScrollTo().assertIsEnabled()
            assertTextBounds()
            rule.onNodeWithText("Jadikan fokus hari ini").performScrollTo().performClick()
            waitFor { model.state.value.snapshot?.validPin == ids[0] }
            rule.onNodeWithText("Lepas fokus manual").performScrollTo().performClick()
            waitFor { model.state.value.snapshot?.validPin == null }
            rule.onNodeWithText("Tutup").performScrollTo().performClick()
        }
        rule.onNodeWithText("+5 lainnya").performScrollTo().performClick()
        assertEquals(Destination.Tasks, destination)
    }
    private fun assertTextBounds() {
        rule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult), useUnmergedTree = true)
            .fetchSemanticsNodes().forEach { node ->
                val results = mutableListOf<TextLayoutResult>()
                node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
                results.forEach { text ->
                    // Bubble title ellipsis is specified; laid-out lines must still stay inside their bounds.
                    assertTrue("Text outside bounds: ${text.layoutInput.text}",
                        (0 until text.lineCount).all { text.getLineRight(it) <= text.size.width + 1f &&
                            text.getLineBottom(it) <= text.size.height + 1f })
                }
            }
    }
}
