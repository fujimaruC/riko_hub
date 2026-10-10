package id.riko.hub.data

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import id.riko.hub.core.time.Clock
import id.riko.hub.data.local.RikoDatabase
import id.riko.hub.data.repository.TaskRepository
import id.riko.hub.domain.logic.taskDeadline
import id.riko.hub.domain.model.Recurrence
import id.riko.hub.domain.model.Task
import id.riko.hub.domain.model.TaskStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class TaskWidgetMutationHookTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val zone = ZoneId.of("Asia/Makassar")
    private val clock = object : Clock {
        override fun now() = Instant.parse("2026-10-20T04:00:00Z").toEpochMilli()
        override fun zone() = this@TaskWidgetMutationHookTest.zone
    }

    @Test fun committed_mutations_notify_once_and_failed_mutations_do_not_notify() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, RikoDatabase::class.java).build()
        try {
            var notifications = 0
            val tasks = TaskRepository(database, clock) { notifications++ }

            val id = tasks.upsert(Task(title = "Daily", deadlineAt = taskDeadline(LocalDate.of(2026, 10, 20), null, zone), recurrence = Recurrence.DAILY))
            assertEquals(1, notifications)
            tasks.upsert(requireNotNull(tasks.observeById(id).first()).copy(title = "Edited daily"))
            assertEquals(2, notifications)

            tasks.setStatus(id, TaskStatus.DONE)
            assertEquals(3, notifications)
            val successor = requireNotNull(database.tasks().findGeneratedSuccessor(id))
            tasks.setStatus(id, TaskStatus.NOT_STARTED)
            assertNotNull(database.tasks().adminFindForWrite(successor.id)?.deletedAt)
            assertEquals(4, notifications)

            tasks.softDelete(id)
            tasks.restore(id)
            assertEquals(6, notifications)

            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { tasks.upsert(Task(title = "Invalid recurring task", recurrence = Recurrence.DAILY)) }
            }
            assertEquals(6, notifications)
        } finally {
            database.close()
        }
    }

    @Test fun callback_failure_does_not_undo_committed_mutation() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, RikoDatabase::class.java).build()
        try {
            val tasks = TaskRepository(database, clock) { error("callback failed") }
            val id = tasks.upsert(Task(title = "Saved despite callback failure"))
            assertEquals("Saved despite callback failure", tasks.observeById(id).first()?.title)
        } finally {
            database.close()
        }
    }
}
