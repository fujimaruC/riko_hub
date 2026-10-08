package id.riko.hub.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import id.riko.hub.core.time.Clock
import id.riko.hub.data.local.Migrations
import id.riko.hub.data.local.RikoDatabase
import id.riko.hub.data.local.entity.TaskEntity
import id.riko.hub.data.mapper.toDomain
import id.riko.hub.data.repository.TaskRepository
import id.riko.hub.domain.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class TaskExtensionDatabaseTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val zone = ZoneId.of("Asia/Makassar")
    private val clock = MutableClock(Instant.parse("2026-10-20T04:00:00Z").toEpochMilli(), zone)

    @Test fun version_1_migration_preserves_rows_and_adds_safe_defaults() {
        val helper = MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            RikoDatabase::class.java.canonicalName!!,
            FrameworkSQLiteOpenHelperFactory(),
        )
        val name = "task-extension-migration.db"
        context.deleteDatabase(name)
        val old = helper.createDatabase(name, 1)
        old.execSQL("""INSERT INTO tasks(id,syncId,title,notes,category,priority,status,deadlineAt,hasDeadlineTime,createdAt,updatedAt,completedAt,deletedAt)
            VALUES(1,'sync-1','Old task',NULL,'PERSONAL','MEDIUM','NOT_STARTED',NULL,0,10,10,NULL,NULL)""")
        old.close()
        val migrated = helper.runMigrationsAndValidate(name, 2, true, Migrations.MIGRATION_1_2)
        migrated.query("SELECT * FROM tasks WHERE id=1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Old task", cursor.getString(cursor.getColumnIndexOrThrow("title")))
            assertNull(cursor.getString(cursor.getColumnIndexOrThrow("estimatedMinutes")))
            assertEquals("NONE", cursor.getString(cursor.getColumnIndexOrThrow("recurrence")))
            assertEquals("[]", cursor.getString(cursor.getColumnIndexOrThrow("checklistJson")))
            assertNull(cursor.getString(cursor.getColumnIndexOrThrow("reminderMinutesBefore")))
            assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("generationPristine")))
            assertNull(cursor.getString(cursor.getColumnIndexOrThrow("recurrenceAnchorAt")))
        }
        migrated.close()
        context.deleteDatabase(name)
    }

    @Test fun malformed_checklist_fails_closed_instead_of_becoming_empty() {
        val row = TaskEntity(
            id = 1, syncId = "sync", title = "Task", notes = null, category = "PERSONAL",
            priority = "MEDIUM", status = "NOT_STARTED", deadlineAt = null, hasDeadlineTime = false,
            createdAt = 1, updatedAt = 1, completedAt = null, deletedAt = null,
            estimatedMinutes = null, recurrence = "NONE",
            checklistJson = "[{\"id\":\"step-1\",\"title\":\"Step\",\"done\":\"false\"}]",
            reminderMinutesBefore = null, generatedFromTaskId = null, generationPristine = false,
            recurrenceAnchorAt = null,
        )
        assertThrows(IllegalStateException::class.java) { row.toDomain() }
    }

    @Test fun completion_undo_and_redo_reuse_pristine_successor() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, RikoDatabase::class.java).build()
        try {
            val tasks = TaskRepository(database, clock)
            val id = tasks.upsert(Task(
                title = "Daily", deadlineAt = taskDeadline(LocalDate.of(2026, 10, 20), null, zone),
                recurrence = Recurrence.DAILY,
                checklist = listOf(ChecklistItem(title = "Review", done = true)),
            ))
            tasks.upsert(requireNotNull(tasks.observeById(id).first()).copy(status = TaskStatus.DONE))
            val child = requireNotNull(database.tasks().findGeneratedSuccessor(id))
            assertEquals(TaskStatus.NOT_STARTED, child.toDomain().status)
            assertEquals(LocalDate.of(2026, 10, 21), Instant.ofEpochMilli(child.deadlineAt!!).atZone(zone).toLocalDate())
            assertFalse(child.toDomain().checklist.single().done)
            assertTrue(child.generationPristine)

            tasks.setStatus(id, TaskStatus.NOT_STARTED)
            assertNotNull(database.tasks().adminFindForWrite(child.id)?.deletedAt)
            assertTrue(database.tasks().adminFindForWrite(child.id)!!.generationPristine)
            tasks.setStatus(id, TaskStatus.DONE)
            val restored = requireNotNull(database.tasks().adminFindForWrite(child.id))
            assertNull(restored.deletedAt)
            assertEquals(2, database.tasks().adminCount())
        } finally {
            database.close()
        }
    }

    @Test fun edited_successor_survives_parent_undo_and_is_not_duplicated_on_redo() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, RikoDatabase::class.java).build()
        try {
            val tasks = TaskRepository(database, clock)
            val id = tasks.upsert(Task(title = "Weekly", deadlineAt = taskDeadline(LocalDate.of(2026, 10, 20), null, zone), recurrence = Recurrence.WEEKLY))
            tasks.setStatus(id, TaskStatus.DONE)
            val child = requireNotNull(database.tasks().findGeneratedSuccessor(id))
            tasks.upsert(child.toDomain().copy(title = "Edited next week"))
            assertFalse(database.tasks().adminFindForWrite(child.id)!!.generationPristine)

            tasks.setStatus(id, TaskStatus.NOT_STARTED)
            assertNull(database.tasks().adminFindForWrite(child.id)?.deletedAt)
            tasks.setStatus(id, TaskStatus.DONE)
            assertEquals(2, database.tasks().adminCount())
            assertEquals("Edited next week", tasks.observeById(child.id).first()?.title)
        } finally {
            database.close()
        }
    }

    private class MutableClock(var time: Long, private val timeZone: ZoneId) : Clock {
        override fun now() = time
        override fun zone() = timeZone
    }
}
