package id.riko.hub.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import id.riko.hub.core.time.Clock
import id.riko.hub.data.local.RikoDatabase
import id.riko.hub.data.local.Migrations
import id.riko.hub.data.repository.AssignmentRepository
import id.riko.hub.data.repository.CourseRepository
import id.riko.hub.data.repository.TaskRepository
import id.riko.hub.domain.model.AcademicState
import id.riko.hub.domain.model.Course
import id.riko.hub.domain.model.Recurrence
import id.riko.hub.domain.model.Task
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId

class AssignmentRepositoryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun tracks_deadline_edits_without_changing_original_and_preserves_identity() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, RikoDatabase::class.java).build()
        try {
            var now = 100L
            val clock = clock { now }
            val tasks = TaskRepository(db, clock)
            val assignments = AssignmentRepository(db, clock)
            val taskId = tasks.upsert(Task(title = "Essay", deadlineAt = 500, hasDeadlineTime = false))
            val assignmentId = assignments.save(taskId, null, AcademicState.PLANNED, null)
            now = 200L
            tasks.upsert(requireNotNull(tasks.observeById(taskId).first()).copy(deadlineAt = null))
            now = 300L
            tasks.upsert(requireNotNull(tasks.observeById(taskId).first()).copy(deadlineAt = 600, hasDeadlineTime = false))
            now = 400L
            tasks.upsert(requireNotNull(tasks.observeById(taskId).first()).copy(hasDeadlineTime = true))

            val assignment = requireNotNull(assignments.observeByTask(taskId).first())
            assertEquals(assignmentId, assignment.id)
            assertEquals(500L, assignment.originalDeadlineAt)
            assertFalse(assignment.originalHasDeadlineTime)
            assertEquals(3, assignment.deadlineChanges.size)
            assertEquals(500L, assignment.deadlineChanges[0].previousDeadlineAt)
            assertNull(assignment.deadlineChanges[0].newDeadlineAt)
            assertFalse(assignment.deadlineChanges[2].previousHasDeadlineTime)
            assertTrue(assignment.deadlineChanges[2].newHasDeadlineTime)
            assertNull(requireNotNull(tasks.observeById(taskId).first()).completedAt)
        } finally { db.close() }
    }

    @Test fun archived_course_is_blocked_for_new_link_but_existing_link_and_soft_delete_restore_work() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, RikoDatabase::class.java).build()
        try {
            val clock = clock { 100L }
            val courses = CourseRepository(db, clock)
            val tasks = TaskRepository(db, clock)
            val assignments = AssignmentRepository(db, clock)
            val courseId = courses.upsert(Course(title = "Math"))
            val taskId = tasks.upsert(Task(title = "Problem set"))
            assignments.save(taskId, courseId, AcademicState.RECEIVED, null)
            courses.setArchived(courseId, true)
            assignments.save(taskId, courseId, AcademicState.PLANNED, null)
            val anotherTaskId = tasks.upsert(Task(title = "Other"))
            assertTrue(runCatching { assignments.save(anotherTaskId, courseId, AcademicState.RECEIVED, null) }.isFailure)
            assertEquals(1, assignments.observeForCourse(courseId).first().size)

            tasks.softDelete(taskId)
            assertNull(assignments.observeByTask(taskId).first())
            assertTrue(assignments.observeAll().first().isEmpty())
            tasks.restore(taskId)
            assertEquals(1, assignments.observeAll().first().size)
        } finally { db.close() }
    }

    @Test fun null_original_deadline_survives_later_task_deadline_changes() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, RikoDatabase::class.java).build()
        try {
            val clock = clock { 100L }
            val tasks = TaskRepository(db, clock)
            val assignments = AssignmentRepository(db, clock)
            val taskId = tasks.upsert(Task(title = "Reading"))
            assignments.save(taskId, null, AcademicState.RECEIVED, null)
            tasks.upsert(requireNotNull(tasks.observeById(taskId).first()).copy(deadlineAt = 500))
            val assignment = requireNotNull(assignments.observeByTask(taskId).first())
            assertNull(assignment.originalDeadlineAt)
            assertEquals(1, assignment.deadlineChanges.size)
            assertNull(assignment.deadlineChanges.single().previousDeadlineAt)
        } finally { db.close() }
    }

    @Test fun invalid_existing_timed_deadline_rolls_back_enrolled_task_edit() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, RikoDatabase::class.java).build()
        try {
            val clock = clock { 100L }
            val tasks = TaskRepository(db, clock)
            val assignments = AssignmentRepository(db, clock)
            val taskId = tasks.upsert(Task(title = "Quiz", deadlineAt = 500, hasDeadlineTime = true))
            assignments.save(taskId, null, AcademicState.PLANNED, null)
            val row = requireNotNull(db.tasks().adminFindForWrite(taskId))
            db.tasks().update(row.copy(deadlineAt = null))
            assertTrue(runCatching { tasks.upsert(requireNotNull(tasks.observeById(taskId).first()).copy(title = "Edited")) }.isFailure)
            assertEquals("Quiz", db.tasks().adminFindForWrite(taskId)?.title)
            assertTrue(requireNotNull(db.tasks().adminFindForWrite(taskId)).hasDeadlineTime)
            assertTrue(requireNotNull(assignments.observeByTask(taskId).first()).deadlineChanges.isEmpty())
        } finally { db.close() }
    }

    @Test fun null_original_deadline_survives_later_task_deadline_changes() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, RikoDatabase::class.java).build()
        try {
            val clock = clock { 100L }
            val tasks = TaskRepository(db, clock)
            val assignments = AssignmentRepository(db, clock)
            val taskId = tasks.upsert(Task(title = "Reading"))
            assignments.save(taskId, null, AcademicState.RECEIVED, null)
            tasks.upsert(requireNotNull(tasks.observeById(taskId).first()).copy(deadlineAt = 500))
            val assignment = requireNotNull(assignments.observeByTask(taskId).first())
            assertNull(assignment.originalDeadlineAt)
            assertEquals(1, assignment.deadlineChanges.size)
            assertNull(assignment.deadlineChanges.single().previousDeadlineAt)
        } finally { db.close() }
    }

    @Test fun invalid_existing_timed_deadline_rolls_back_enrolled_task_edit() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, RikoDatabase::class.java).build()
        try {
            val clock = clock { 100L }
            val tasks = TaskRepository(db, clock)
            val assignments = AssignmentRepository(db, clock)
            val taskId = tasks.upsert(Task(title = "Quiz", deadlineAt = 500, hasDeadlineTime = true))
            assignments.save(taskId, null, AcademicState.PLANNED, null)
            val row = requireNotNull(db.tasks().adminFindForWrite(taskId))
            db.tasks().update(row.copy(deadlineAt = null))
            assertTrue(runCatching { tasks.upsert(requireNotNull(tasks.observeById(taskId).first()).copy(title = "Edited")) }.isFailure)
            assertEquals("Quiz", db.tasks().adminFindForWrite(taskId)?.title)
            assertTrue(requireNotNull(db.tasks().adminFindForWrite(taskId)).hasDeadlineTime)
            assertTrue(requireNotNull(assignments.observeByTask(taskId).first()).deadlineChanges.isEmpty())
        } finally { db.close() }
    }

    @Test fun task_completion_does_not_change_academic_state_and_recurrence_successor_is_unlinked() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, RikoDatabase::class.java).build()
        try {
            val clock = clock { 1_000L }
            val tasks = TaskRepository(db, clock)
            val assignments = AssignmentRepository(db, clock)
            val taskId = tasks.upsert(Task(title = "Weekly", deadlineAt = 5_000, recurrence = Recurrence.WEEKLY))
            assignments.save(taskId, null, AcademicState.REVIEW, null)
            tasks.setStatus(taskId, id.riko.hub.domain.model.TaskStatus.DONE)
            assertEquals(AcademicState.REVIEW, assignments.observeByTask(taskId).first()?.state)
            val taskRows = db.tasks().backupSnapshot()
            assertEquals(2, taskRows.size)
            assertNull(assignments.observeByTask(taskRows.single { it.id != taskId }.id).first())
        } finally { db.close() }
    }

    @Test fun recurrence_undo_and_redo_preserve_manually_worked_academic_successor() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, RikoDatabase::class.java).build()
        try {
            val clock = clock { 1_000L }
            val tasks = TaskRepository(db, clock)
            val assignments = AssignmentRepository(db, clock)
            val parentId = tasks.upsert(Task(title = "Weekly", deadlineAt = 5_000, recurrence = Recurrence.WEEKLY))
            tasks.setStatus(parentId, id.riko.hub.domain.model.TaskStatus.DONE)
            val successor = db.tasks().backupSnapshot().single { it.generatedFromTaskId == parentId }
            assignments.save(successor.id, null, AcademicState.SUBMITTED, "submitted work")

            tasks.setStatus(parentId, id.riko.hub.domain.model.TaskStatus.IN_PROGRESS)
            val kept = requireNotNull(tasks.observeById(successor.id).first())
            assertNull(kept.deletedAt)
            assertEquals(AcademicState.SUBMITTED, assignments.observeByTask(successor.id).first()?.state)

            tasks.softDelete(successor.id)
            tasks.setStatus(parentId, id.riko.hub.domain.model.TaskStatus.DONE)
            assertNotNull(db.tasks().adminFindForWrite(successor.id)?.deletedAt)
            assertNull(assignments.observeByTask(successor.id).first())
        } finally { db.close() }
    }

    @Test fun migration_3_to_4_preserves_existing_rows_and_adds_assignment_foreign_keys() {
        val helper = MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(), RikoDatabase::class.java.canonicalName!!,
            FrameworkSQLiteOpenHelperFactory(),
        )
        val name = "assignment-migration.db"
        context.deleteDatabase(name)
        val old = helper.createDatabase(name, 3)
        old.execSQL("""INSERT INTO tasks(id,syncId,title,category,priority,status,hasDeadlineTime,createdAt,updatedAt,recurrence,checklistJson,generationPristine)
            VALUES(7,'task-7','Task before migration','PERSONAL','MEDIUM','NOT_STARTED',0,10,11,'NONE','[]',0)""")
        old.execSQL("""INSERT INTO events(id,syncId,title,kind,category,startAt,createdAt,updatedAt)
            VALUES(9,'event-9','Event before migration','CLASS','ACADEMIC',100,10,11)""")
        old.execSQL("""INSERT INTO courses(id,syncId,title,sortOrder,createdAt,updatedAt)
            VALUES(11,'course-11','Course before migration',0,10,11)""")
        old.execSQL("""INSERT INTO course_weekly_slots(id,courseId,dayOfWeek,startMinute,endMinute)
            VALUES(13,11,1,480,540)""")
        old.close()
        val migrated = helper.runMigrationsAndValidate(name, 4, true, Migrations.MIGRATION_3_4)
        migrated.query("SELECT id,title FROM tasks WHERE id=7").use { assertTrue(it.moveToFirst()); assertEquals("Task before migration", it.getString(1)) }
        migrated.query("SELECT id,title FROM events WHERE id=9").use { assertTrue(it.moveToFirst()); assertEquals("Event before migration", it.getString(1)) }
        migrated.query("SELECT id,title FROM courses WHERE id=11").use { assertTrue(it.moveToFirst()); assertEquals("Course before migration", it.getString(1)) }
        migrated.query("SELECT id,courseId FROM course_weekly_slots WHERE id=13").use { assertTrue(it.moveToFirst()); assertEquals(11L, it.getLong(1)) }
        migrated.query("PRAGMA foreign_key_list(academic_assignments)").use { cursor ->
            val actions = buildList { while (cursor.moveToNext()) add(cursor.getString(cursor.getColumnIndexOrThrow("on_delete"))) }
            assertTrue("CASCADE" in actions)
            assertTrue("SET NULL" in actions)
        }
        migrated.query("PRAGMA foreign_key_list(academic_deadline_changes)").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("CASCADE", cursor.getString(cursor.getColumnIndexOrThrow("on_delete")))
        }
        migrated.close()
        context.deleteDatabase(name)
    }

    private fun clock(now: () -> Long) = object : Clock {
        override fun now() = now()
        override fun zone() = ZoneId.of("UTC")
    }
}
