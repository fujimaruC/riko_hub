package id.riko.hub.ui.timeline

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import id.riko.hub.core.time.Clock
import id.riko.hub.data.local.RikoDatabase
import id.riko.hub.data.repository.EventRepository
import id.riko.hub.data.repository.TaskRepository
import id.riko.hub.domain.model.Event
import id.riko.hub.domain.model.Task
import id.riko.hub.ui.theme.RikoHubTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class TimelineTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val now = Instant.parse("2026-10-06T06:00:00Z").toEpochMilli()
    private val zone = ZoneId.of("Asia/Makassar")
    private val week = LocalDate.of(2026, 10, 5)
    private val clock = object : Clock {
        override fun now() = now
        override fun zone() = zone
    }
    private lateinit var database: RikoDatabase
    private lateinit var tasks: TaskRepository
    private lateinit var events: EventRepository
    private lateinit var model: TimelineViewModel
    private val store = ViewModelStore()
    private val fontScale = mutableFloatStateOf(1f)

    @Before fun setup() {
        rule.runOnUiThread { rule.activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
        database = Room.inMemoryDatabaseBuilder(context, RikoDatabase::class.java).build()
        tasks = TaskRepository(database, clock)
        events = EventRepository(database, clock)
        runBlocking { tasks.upsert(Task(title = "Existing task", deadlineAt = now, hasDeadlineTime = true)) }
        model = TimelineViewModel(tasks, events, clock)
        store.put("timeline", model)
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale.floatValue)) {
                RikoHubTheme { Surface { TimelineRoute(model) } }
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("screen-Timeline").assertExists()
        waitFor { !model.state.value.loading }
    }

    @After fun cleanup() {
        rule.runOnIdle { store.clear() }
        database.close()
    }

    @Test fun event_crud_undo_task_stop_week_navigation_and_stale_retry_keep_other_event() {
        val taskId = runBlocking { tasks.observeAll().first().single().id }
        waitFor { model.state.value.days.any { day -> day.stops.any { it.task?.id == taskId } } }

        rule.onNodeWithTag("timeline-next").performClick()
        waitFor { model.state.value.weekStart == week.plusWeeks(1) && !model.state.value.loading }
        rule.onNodeWithText("Minggu ini", substring = false).performClick()
        waitFor { model.state.value.weekStart == week }

        rule.onNodeWithTag("timeline-stop-deadline:$taskId").performScrollTo().performClick()
        waitFor { model.state.value.taskEditor?.id == taskId }
        rule.onNodeWithTag("task-workspace-title").assertTextContains("Existing task")
        rule.onNodeWithText("Tutup", substring = false).performScrollTo().performClick()
        waitFor { model.state.value.taskEditor == null }

        rule.onNodeWithTag("timeline-add").performClick()
        waitFor { model.state.value.eventEditor != null }
        rule.onNodeWithTag("event-save").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag("event-title").performScrollTo().performTextInput("Rapat tim")
        rule.onNodeWithTag("event-save").performScrollTo().assertIsEnabled().performClick()
        waitFor { model.state.value.eventEditor == null && eventsInWeek().size == 1 }
        val created = eventsInWeek().single()

        waitFor { model.state.value.days.any { day -> day.stops.any { it.event?.id == created.id } } }
        val stopTag = eventStopTag(created.id)
        rule.onNodeWithTag(stopTag).performScrollTo().performClick()
        waitFor { model.state.value.eventEditor?.id == created.id }
        rule.onNodeWithTag("event-title").performTextReplacement("Rapat diperbarui")
        rule.onNodeWithTag("event-save").performScrollTo().performClick()
        waitFor { model.state.value.eventEditor == null && eventsInWeek().singleOrNull()?.title == "Rapat diperbarui" }

        rule.onNodeWithTag(stopTag).performScrollTo().performClick()
        rule.onNodeWithText("Hapus acara").performScrollTo().performClick()
        waitFor { model.state.value.eventEditor == null && eventsInWeek().isEmpty() }
        rule.onNodeWithText("Urungkan").performClick()
        waitFor { eventsInWeek().singleOrNull()?.id == created.id &&
            model.state.value.days.any { day -> day.stops.any { it.event?.id == created.id } } }

        // A failed delete must keep its original target when another event is opened before retry.
        rule.runOnUiThread { model.changeEvent(Event(id = 999, title = "Missing", startAt = now)) }
        rule.onNodeWithText("Hapus acara").performScrollTo().performClick()
        waitFor { rule.onAllNodesWithText("Gagal menyimpan. Coba lagi.").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Tutup", substring = false).performScrollTo().performClick()
        waitFor { model.state.value.eventEditor == null }
        rule.onNodeWithTag(stopTag).performScrollTo().performClick()
        waitFor { model.state.value.eventEditor?.id == created.id }
        rule.onNodeWithText("Coba lagi", substring = false).performClick()
        waitFor { !model.state.value.busy &&
            rule.onAllNodesWithText("Gagal menyimpan. Coba lagi.").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(created.id, model.state.value.eventEditor?.id)
        assertTrue(eventsInWeek().any { it.id == created.id })
    }

    @Test fun event_end_validation_disables_save_at_200_percent_scale() {
        rule.runOnIdle { fontScale.floatValue = 2f }
        rule.onNodeWithTag("timeline-add").performClick()
        rule.onNodeWithTag("event-title").performScrollTo().performTextInput("Acara baru")
        rule.runOnUiThread {
            val event = requireNotNull(model.state.value.eventEditor)
            model.changeEvent(event.copy(endAt = event.startAt - 1))
        }
        rule.onNodeWithTag("event-save").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithText("Waktu selesai harus sama dengan atau setelah waktu mulai.").assertExists()

        rule.runOnUiThread {
            val event = requireNotNull(model.state.value.eventEditor)
            model.changeEvent(event.copy(endAt = event.startAt + 3_600_000))
        }
        rule.onNodeWithTag("event-save").performScrollTo().assertIsEnabled()
        rule.onNodeWithText("Tutup", substring = false).performScrollTo().performClick()
    }

    @Test fun day_mode_navigates_across_week_boundaries_and_returns_to_week() {
        rule.onNodeWithTag("timeline-mode-day").performClick()
        waitFor { model.state.value.dayMode && model.state.value.days.size == 1 && model.state.value.selectedDay == LocalDate.of(2026, 10, 6) }
        repeat(6) { rule.onNodeWithTag("timeline-next").performClick() }
        waitFor { model.state.value.selectedDay == LocalDate.of(2026, 10, 12) && model.state.value.weekStart == week.plusWeeks(1) }
        rule.onNodeWithTag("timeline-previous").performClick()
        waitFor { model.state.value.selectedDay == LocalDate.of(2026, 10, 11) }
        rule.onNodeWithTag("timeline-mode-week").performClick()
        waitFor { !model.state.value.dayMode && model.state.value.days.size == 7 && model.state.value.weekStart == week }
    }

    @Test fun day_strip_selects_absolute_date_after_paging_to_next_week() {
        rule.onNodeWithTag("timeline-next").performClick()
        waitFor { model.state.value.weekStart == week.plusWeeks(1) && !model.state.value.loading }
        rule.onNodeWithTag("timeline-day-2026-10-14").performClick()
        waitFor { model.state.value.dayMode && model.state.value.selectedDay == LocalDate.of(2026, 10, 14) }
    }

    private fun waitFor(condition: () -> Boolean) = rule.waitUntil(10_000, condition)

    private fun eventsInWeek(weekStart: LocalDate = week) = runBlocking {
        events.observeBetween(
            weekStart.atStartOfDay(zone).toInstant().toEpochMilli(),
            weekStart.plusWeeks(1).atStartOfDay(zone).toInstant().toEpochMilli(),
        ).first()
    }

    private fun eventStopTag(id: Long): String {
        val key = model.state.value.days.flatMap { it.stops }.single { it.event?.id == id }.key
        return "timeline-stop-$key"
    }
}
