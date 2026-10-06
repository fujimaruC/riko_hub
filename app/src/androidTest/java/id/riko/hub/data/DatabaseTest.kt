package id.riko.hub.data

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import id.riko.hub.core.time.Clock
import id.riko.hub.data.local.RikoDatabase
import id.riko.hub.data.mapper.toDomain
import id.riko.hub.data.repository.EventRepository
import id.riko.hub.data.repository.TaskRepository
import id.riko.hub.domain.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId

class DatabaseTest {
    private val clock = object : Clock {
        override fun now() = 1000L
        override fun zone(): ZoneId = ZoneId.of("Asia/Makassar")
    }

    @Test fun task_queries_exclude_deleted_rows_and_status_changes_keep_invariants() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, RikoDatabase::class.java).build()
        try {
            val tasks = TaskRepository(database, clock)
            val id = tasks.upsert(Task(title = "  UTS  ", deadlineAt = 1100))
            val task = requireNotNull(tasks.observeById(id).first())
            assertEquals("UTS", task.title)
            assertEquals(1, tasks.observeActive().first().size)
            tasks.upsert(task.copy(title = "UTS besok"))
            assertEquals("UTS besok", tasks.observeById(id).first()?.title)
            tasks.setStatus(id, TaskStatus.DONE)
            assertEquals(1000L, tasks.observeById(id).first()?.completedAt)
            assertTrue(tasks.observeActive().first().isEmpty())
            assertEquals(1, database.tasks().observeCompletedBetween(1000, 1001).first().size)
            tasks.softDelete(id)
            assertNull(tasks.observeById(id).first())
            assertTrue(tasks.observeAll().first().isEmpty())
            assertTrue(database.tasks().observeDeadlinesBetween(1000, 1200).first().isEmpty())
            assertTrue(database.tasks().observeCompletedBetween(1000, 1200).first().isEmpty())
            val rejected = runCatching { tasks.upsert(task) }
            assertTrue(rejected.exceptionOrNull() is IllegalArgumentException)
            tasks.restore(id)
            tasks.setStatus(id, TaskStatus.NOT_STARTED)
            assertNull(tasks.observeById(id).first()?.completedAt)
            assertEquals(1, tasks.observeActive().first().size)
            val unknown = requireNotNull(database.tasks().adminFindForWrite(id)).copy(category = "FUTURE", priority = "FUTURE", status = "FUTURE")
            assertEquals(Category.PERSONAL, unknown.toDomain().category)
            assertEquals(TaskStatus.NOT_STARTED, unknown.toDomain().status)
        } finally { database.close() }
    }

    @Test fun event_queries_include_overlaps_and_exclude_deleted_rows() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, RikoDatabase::class.java).build()
        try {
            val events = EventRepository(database, clock)
            val spanning = events.upsert(Event(title = "Kelas", startAt = 50, endAt = 250))
            val point = events.upsert(Event(title = "Catatan", startAt = 100))
            events.upsert(Event(title = "Di luar rentang", startAt = 200))
            assertEquals(listOf(spanning, point), events.observeBetween(100, 200).first().map { it.id })
            events.softDelete(spanning)
            assertNull(events.observeById(spanning).first())
            assertEquals(listOf(point), events.observeBetween(100, 200).first().map { it.id })
            events.restore(spanning)
            val event = requireNotNull(events.observeById(spanning).first())
            events.upsert(event.copy(title = "Kelas baru"))
            assertEquals("Kelas baru", events.observeById(spanning).first()?.title)
            assertEquals(2, events.observeBetween(100, 200).first().size)
        } finally { database.close() }
    }
}
