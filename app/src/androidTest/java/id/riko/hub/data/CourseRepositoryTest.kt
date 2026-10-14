package id.riko.hub.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import id.riko.hub.core.time.Clock
import id.riko.hub.data.local.Migrations
import id.riko.hub.data.local.RikoDatabase
import id.riko.hub.data.repository.CourseRepository
import id.riko.hub.domain.model.Course
import id.riko.hub.domain.model.CourseWeeklySlot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId

class CourseRepositoryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val clock = object : Clock {
        override fun now() = 1_000L
        override fun zone() = ZoneId.of("Asia/Makassar")
    }

    @Test fun create_edit_archive_and_move_keep_slots_and_stable_identity() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, RikoDatabase::class.java).build()
        try {
            val repo = CourseRepository(db, clock)
            val slot = CourseWeeklySlot(1, 480, 540, " Room 1 ")
            val first = repo.upsert(Course(title = "  Biology  ", weeklySlots = listOf(slot)))
            val second = repo.upsert(Course(title = "Chemistry"))
            val created = requireNotNull(repo.observeById(first).first())
            assertEquals("Biology", created.title)
            assertEquals("Room 1", created.weeklySlots.single().location)
            assertEquals(listOf(first, second), repo.observeAll().first().map { it.id })

            repo.move(second, -1)
            assertEquals(listOf(second, first), repo.observeAll().first().map { it.id })
            val edited = requireNotNull(repo.observeById(first).first())
            repo.upsert(edited.copy(title = "Biology II", weeklySlots = emptyList(), syncId = "changed"))
            val saved = requireNotNull(repo.observeById(first).first())
            assertEquals(created.syncId, saved.syncId)
            assertEquals(created.createdAt, saved.createdAt)
            assertEquals("Biology II", saved.title)
            assertTrue(saved.weeklySlots.isEmpty())

            repo.setArchived(first, true)
            assertNotNull(repo.observeById(first).first()?.archivedAt)
            assertEquals(listOf(second, first), repo.observeAll().first().map { it.id })
            repo.setArchived(first, false)
            assertNull(repo.observeById(first).first()?.archivedAt)
        } finally { db.close() }
    }

    @Test fun validation_failure_rolls_back_course_edit_and_slot_replacement() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, RikoDatabase::class.java).build()
        try {
            val repo = CourseRepository(db, clock)
            val id = repo.upsert(Course(title = "Physics", weeklySlots = listOf(CourseWeeklySlot(2, 600, 660))))
            val invalid = requireNotNull(repo.observeById(id).first()).copy(
                title = "Changed", weeklySlots = listOf(CourseWeeklySlot(2, 660, 600)),
            )
            assertTrue(runCatching { repo.upsert(invalid) }.isFailure)
            val persisted = requireNotNull(repo.observeById(id).first())
            assertEquals("Physics", persisted.title)
            assertEquals(600, persisted.weeklySlots.single().startMinute)
            assertEquals(1, db.courses().backupSnapshot().size)
        } finally { db.close() }
    }

    @Test fun migration_2_to_3_preserves_task_and_event_rows() {
        val helper = MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            RikoDatabase::class.java.canonicalName!!,
            FrameworkSQLiteOpenHelperFactory(),
        )
        val name = "course-migration.db"
        context.deleteDatabase(name)
        val old = helper.createDatabase(name, 2)
        old.execSQL("""INSERT INTO tasks(id,syncId,title,category,priority,status,hasDeadlineTime,createdAt,updatedAt,recurrence,checklistJson,generationPristine)
            VALUES(7,'task-7','Task before migration','PERSONAL','MEDIUM','NOT_STARTED',0,10,11,'NONE','[]',0)""")
        old.execSQL("""INSERT INTO events(id,syncId,title,kind,category,startAt,createdAt,updatedAt)
            VALUES(9,'event-9','Event before migration','CLASS','ACADEMIC',100,10,11)""")
        old.close()
        val migrated = helper.runMigrationsAndValidate(name, 3, true, Migrations.MIGRATION_2_3)
        migrated.query("SELECT id,title FROM tasks WHERE id=7").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Task before migration", cursor.getString(1))
        }
        migrated.query("SELECT id,title FROM events WHERE id=9").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Event before migration", cursor.getString(1))
        }
        migrated.query("PRAGMA foreign_key_list(course_weekly_slots)").use { cursor -> assertTrue(cursor.moveToFirst()) }
        migrated.close()
        context.deleteDatabase(name)
    }
}
