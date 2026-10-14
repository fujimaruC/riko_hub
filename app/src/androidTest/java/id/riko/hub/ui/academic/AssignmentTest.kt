package id.riko.hub.ui.academic

import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import id.riko.hub.core.time.Clock
import id.riko.hub.data.local.RikoDatabase
import id.riko.hub.data.repository.AssignmentRepository
import id.riko.hub.data.repository.CourseRepository
import id.riko.hub.data.repository.TaskRepository
import id.riko.hub.domain.model.*
import id.riko.hub.ui.theme.RikoHubTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class AssignmentTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val clock = object : Clock {
        override fun now() = Instant.parse("2026-10-06T04:00:00Z").toEpochMilli()
        override fun zone() = ZoneId.of("Asia/Makassar")
    }
    private lateinit var database: RikoDatabase
    private lateinit var tasks: TaskRepository
    private lateinit var courses: CourseRepository
    private lateinit var model: AssignmentViewModel
    private lateinit var task: Task
    private val store = ViewModelStore()

    @Before fun setup() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(context, RikoDatabase::class.java).allowMainThreadQueries().build()
        tasks = TaskRepository(database, clock)
        courses = CourseRepository(database, clock)
        val taskId = tasks.upsert(Task(title = "Laporan biologi", status = TaskStatus.NOT_STARTED))
        task = tasks.observeById(taskId).first()!!
        courses.upsert(Course(title = "Biologi"))
        model = AssignmentViewModel(task, AssignmentRepository(database, clock), courses, tasks)
        store.put("assignment", model)
        rule.setContent { RikoHubTheme { Surface { AssignmentEditorContent(task, model) {} } } }
    }

    @After fun cleanup() {
        rule.runOnIdle { store.clear() }
        database.close()
    }

    @Test fun submission_and_reopen_keep_task_status_reference_and_course() = runBlocking {
        rule.waitUntil(5_000) { model.state.value.task != null && !model.state.value.loading }
        rule.onNodeWithText("Tanpa mata kuliah").performClick()
        rule.onNodeWithText("Biologi").performClick()
        rule.onNodeWithTag("assignment-reference").performTextInput("https://kampus.example/tugas/1")
        rule.onNodeWithText("Dikumpulkan").performClick()
        rule.onNodeWithTag("assignment-save").performScrollTo().performClick()
        rule.waitUntil(5_000) { model.state.value.assignment?.state == AcademicState.SUBMITTED && !model.state.value.busy }
        val submitted = model.state.value.assignment!!
        assertNotNull(submitted.submittedAt)
        assertEquals("https://kampus.example/tugas/1", submitted.submissionReference)
        assertEquals(courses.observeAll().first().single().id, submitted.courseId)
        assertEquals(TaskStatus.NOT_STARTED, tasks.observeById(task.id).first()?.status)

        rule.onNodeWithTag("assignment-reopen").performClick()
        rule.onNodeWithTag("assignment-save").performScrollTo().performClick()
        rule.waitUntil(5_000) { model.state.value.assignment?.state == AcademicState.IN_PROGRESS && !model.state.value.busy }
        assertEquals(submitted.submittedAt, model.state.value.assignment?.submittedAt)
        assertEquals(submitted.submissionReference, model.state.value.assignment?.submissionReference)
        assertEquals(TaskStatus.NOT_STARTED, tasks.observeById(task.id).first()?.status)
    }
}
