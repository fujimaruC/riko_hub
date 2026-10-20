package id.riko.hub.ui.tasks

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import id.riko.hub.core.time.Clock
import id.riko.hub.data.local.RikoDatabase
import id.riko.hub.data.mapper.toDomain
import id.riko.hub.data.repository.TaskRepository
import id.riko.hub.domain.logic.taskDeadline
import id.riko.hub.domain.model.*
import id.riko.hub.ui.theme.RikoHubTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.time.*

class TaskCrudTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val clock = object : Clock {
        override fun now() = Instant.parse("2026-10-06T04:00:00Z").toEpochMilli()
        override fun zone() = ZoneId.of("Asia/Makassar")
    }
    private lateinit var database: RikoDatabase
    private lateinit var model: TasksViewModel
    private val store = ViewModelStore()
    private val scale = mutableFloatStateOf(1f)
    private val dark = mutableStateOf(false)
    private val openTaskId = mutableStateOf<Long?>(null)
    private val taskOpenConsumed = mutableStateOf(false)

    @Before fun setup() {
        rule.runOnUiThread { rule.activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
        context.deleteDatabase("phase3-ui-check.db")
        database = Room.databaseBuilder(context, RikoDatabase::class.java, "phase3-ui-check.db").build()
        model = TasksViewModel(TaskRepository(database, clock), clock)
        store.put("tasks", model)
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, scale.floatValue)) {
                RikoHubTheme(darkTheme = dark.value) { Surface { TasksRoute(model, {}, openTaskId.value) { taskOpenConsumed.value = true } } }
            }
        }
        rule.waitUntil(5_000) { !model.state.value.loading }
    }
    @After fun cleanup() {
        rule.runOnIdle { store.clear() }
        database.close()
        context.deleteDatabase("phase3-ui-check.db")
    }
    private fun tasks() = runBlocking { database.tasks().observeAll().first() }
    private fun waitFor(condition: () -> Boolean) = rule.waitUntil(5_000, condition)
    private fun save() { rule.onNodeWithTag("task-save").performScrollTo().performClick() }

    @Test fun opening_notification_id_opens_existing_task_and_consumes_missing_id() {
        rule.onNodeWithText("Tambah").performClick()
        rule.onNodeWithTag("task-title").performTextInput("Buka dari pengingat")
        save()
        waitFor { tasks().size == 1 && model.state.value.editor == null }
        val id = tasks().single().id
        rule.runOnIdle { openTaskId.value = id }
        waitFor { model.state.value.editor?.id == id && taskOpenConsumed.value }
        rule.runOnIdle { model.dismissEditor(); openTaskId.value = null; taskOpenConsumed.value = false }
        rule.runOnIdle { openTaskId.value = id + 10_000L }
        waitFor { taskOpenConsumed.value && model.state.value.editor == null }
    }

    @Test fun editor_filter_complete_reopen_delete_undo_and_restart_preserve_data() {
        rule.onNodeWithText("Tambah").performClick()
        rule.onNodeWithTag("task-save").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag("task-title").performScrollTo().performTextInput("Tugas basis data")
        rule.onNode(hasText("Akademik") and !hasTestTag("filter-ACADEMIC")).performScrollTo().performClick()
        rule.onNodeWithText("Besok").performScrollTo().performClick()
        rule.onNodeWithText("Mingguan").performScrollTo().performClick()
        rule.onNodeWithTag("task-estimate").performScrollTo().performTextInput("45")
        rule.onNodeWithTag("task-reminder").performScrollTo().performTextInput("30")
        rule.onNodeWithTag("checklist-new").performScrollTo().performTextInput("Baca bab")
        rule.onNodeWithText("Tambah item").performScrollTo().performClick()
        rule.onNodeWithText("Tinggi").performScrollTo().performClick()
        rule.onNodeWithTag("task-notes").performScrollTo().performTextInput("Baca bab tiga")
        save()
        waitFor { tasks().size == 1 && model.state.value.editor == null }
        val id = tasks().single().id
        assertEquals("ACADEMIC", tasks().single().category)
        assertEquals("HIGH", tasks().single().priority)
        assertEquals("Baca bab tiga", tasks().single().notes)
        assertEquals(45, tasks().single().estimatedMinutes)
        assertEquals(Recurrence.WEEKLY, tasks().single().toDomain().recurrence)
        assertEquals(30, tasks().single().reminderMinutesBefore)
        assertEquals("Baca bab", tasks().single().toDomain().checklist.single().title)
        assertEquals(taskDeadline(LocalDate.of(2026, 10, 7), null, clock.zone()), tasks().single().deadlineAt)
        rule.onNodeWithTag("filter-PERSONAL").performScrollTo().performClick()
        rule.onNodeWithText("Belum ada tugas di sini.").assertExists()
        rule.onNodeWithTag("filter-ACADEMIC").performScrollTo().performClick()
        rule.onNodeWithTag("task-$id").performScrollTo().performClick()
        rule.onNodeWithTag("task-workspace-title").assertTextContains("Tugas basis data")
        rule.onNodeWithText("Baca bab tiga").assertExists()
        rule.onNodeWithTag("task-completion-action").assertExists()
        rule.onNodeWithTag("task-workspace-edit").performScrollTo().performClick()
        rule.onNodeWithTag("task-title").performTextReplacement("Tugas diperbarui")
        rule.onNodeWithText("Sedang dikerjakan").performScrollTo().performClick()
        save()
        waitFor { tasks().single().status == "IN_PROGRESS" && model.state.value.editor == null }
        rule.onNodeWithTag("check-$id").performScrollTo().performClick()
        waitFor { tasks().first { it.id == id }.status == "DONE" }
        val generated = tasks().single { it.generatedFromTaskId == id }
        assertEquals(TaskStatus.NOT_STARTED, generated.toDomain().status)
        assertNotNull(tasks().first { it.id == id }.completedAt)
        rule.onNodeWithTag("done-bucket").performScrollTo().performClick()
        rule.onNodeWithTag("check-$id").performScrollTo().performClick()
        waitFor { tasks().first { it.id == id }.status == "NOT_STARTED" }
        assertNull(tasks().first { it.id == id }.completedAt)
        assertTrue(tasks().none { it.id == generated.id })
        rule.onNodeWithTag("task-$id").performScrollTo().performClick()
        rule.onNodeWithText("Hapus tugas").performScrollTo().performClick()
        waitFor { tasks().isEmpty() }
        rule.onNodeWithText("Urungkan").performClick()
        waitFor { tasks().size == 1 }
        rule.runOnIdle { store.clear() }
        database.close()
        database = Room.databaseBuilder(context, RikoDatabase::class.java, "phase3-ui-check.db").build()
        assertEquals("Tugas diperbarui", tasks().single().title)
        assertEquals("NOT_STARTED", tasks().single().status)
        assertEquals("Baca bab tiga", tasks().single().notes)
    }

    @Test fun invalid_numeric_draft_blocks_save_and_search_matches_title_or_notes() {
        rule.onNodeWithText("Tambah").performClick()
        rule.onNodeWithTag("task-title").performTextInput("Biology lab")
        rule.onNodeWithTag("task-estimate").performScrollTo().performTextInput("0")
        rule.onNodeWithTag("task-save").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag("task-estimate").performTextReplacement("30")
        rule.onNodeWithTag("task-notes").performScrollTo().performTextInput("Read chapter")
        save()
        waitFor { tasks().size == 1 && model.state.value.editor == null }
        rule.onNodeWithTag("task-search").performScrollTo().performTextInput("CHAPTER")
        rule.onNodeWithTag("task-${tasks().single().id}").assertExists()
        rule.onNodeWithTag("task-search").performTextReplacement("missing")
        rule.onNodeWithText("Belum ada tugas di sini.").assertExists()
        rule.onNodeWithTag("task-search").performTextClearance()
        rule.onNodeWithTag("task-${tasks().single().id}").assertExists()
    }

    @Test fun checklist_progress_is_hidden_when_empty_and_updates_after_checking_item() {
        rule.onNodeWithText("Tambah").performClick()
        rule.onNodeWithTag("task-title").performTextInput("Siapkan presentasi")
        save()
        waitFor { tasks().size == 1 && model.state.value.editor == null }
        rule.onNodeWithText("0/0 selesai").assertDoesNotExist()
        val taskTag = "task-${tasks().single().id}"
        rule.onNodeWithTag(taskTag).performScrollTo().performClick()
        rule.onNodeWithTag("task-workspace-edit").performScrollTo().performClick()
        rule.onNodeWithTag("checklist-new").performScrollTo().performTextInput("Susun materi")
        rule.onNodeWithText("Tambah item").performScrollTo().performClick()
        rule.onNodeWithTag("checklist-new").performTextInput("Latihan")
        rule.onNodeWithText("Tambah item").performScrollTo().performClick()
        save()
        waitFor { tasks().single().toDomain().checklist.size == 2 && model.state.value.editor == null }
        rule.onNodeWithText("0/2 selesai").assertExists()
        rule.onNodeWithTag(taskTag).performScrollTo().performClick()
        rule.onNodeWithTag("task-workspace-edit").performScrollTo().performClick()
        val checklistItems = rule.onAllNodes(
            isToggleable() and hasParent(hasAnyDescendant(hasTestTag("checklist-title-0"))),
            useUnmergedTree = true,
        )
        checklistItems.assertCountEquals(2)
        checklistItems.onFirst().performScrollTo().performClick()
        save()
        waitFor { tasks().single().toDomain().checklist.count { it.done } == 1 && model.state.value.editor == null }
        rule.onNodeWithText("1/2 selesai").assertExists()
    }

    @Test fun large_text_both_themes_and_failed_save_keep_editor_usable() {
        rule.runOnIdle { scale.floatValue = 2f }
        rule.onNodeWithText("Tambah").performClick()
        rule.onNodeWithTag("task-title").performTextInput("Tugas dengan judul panjang untuk menguji tata letak")
        listOf(false, true).forEach { night ->
            rule.runOnIdle { dark.value = night }
            rule.onNodeWithTag("task-title").performScrollTo()
            assertNoClipping()
            rule.onNodeWithText("Sedang dikerjakan").performScrollTo()
            assertNoClipping()
            rule.onNodeWithTag("task-save").performScrollTo().assertIsEnabled()
            assertNoClipping()
        }
        // A stale editor record exercises the actual repository failure and ViewModel recovery path.
        rule.runOnIdle { model.change(model.state.value.editor!!.copy(id = 999)) }
        rule.onNodeWithTag("task-workspace-edit").performScrollTo().performClick()
        save()
        waitFor { rule.onAllNodesWithText("Gagal menyimpan. Coba lagi.").fetchSemanticsNodes().isNotEmpty() }
        assertNotNull(model.state.value.editor)
        assertTrue(tasks().isEmpty())
        rule.runOnIdle { model.change(model.state.value.editor!!.copy(id = 0)) }
        rule.onNodeWithText("Coba lagi.", substring = true).assertExists()
        rule.onNodeWithText("Coba lagi", substring = false).performClick()
        waitFor { tasks().size == 1 && model.state.value.editor == null }
        rule.onNodeWithTag("task-${tasks().single().id}").performScrollTo()
        assertNoClipping()
    }

    private fun assertNoClipping() {
        rule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult), useUnmergedTree = true)
            .fetchSemanticsNodes().forEach { node ->
                val results = mutableListOf<TextLayoutResult>()
                node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
                results.forEach { text ->
                    assertFalse("Clipped: ${text.layoutInput.text}", text.multiParagraph.didExceedMaxLines ||
                        (0 until text.lineCount).any { text.getLineRight(it) > text.size.width + 1f || text.getLineBottom(it) > text.size.height + 1f })
                }
            }
    }
}
