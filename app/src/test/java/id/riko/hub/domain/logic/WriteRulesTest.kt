package id.riko.hub.domain.logic

import id.riko.hub.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class WriteRulesTest {
    @Test fun completion_and_reopening_keep_timestamps_consistent() {
        val initial = prepareTask(Task(title = "  UTS  ", completedAt = 99), null, 100).copy(id = 1)
        assertEquals("UTS", initial.title)
        assertNull(initial.completedAt)
        val done = prepareTask(initial.copy(status = TaskStatus.DONE), initial, 200)
        assertEquals(200L, done.completedAt)
        val edited = prepareTask(done.copy(title = "UTS besok", createdAt = 999, syncId = "changed"), done, 300)
        assertEquals(200L, edited.completedAt)
        assertEquals(initial.createdAt, edited.createdAt)
        assertEquals(initial.syncId, edited.syncId)
        val reopened = prepareTask(edited.copy(status = TaskStatus.IN_PROGRESS), edited, 50)
        assertNull(reopened.completedAt)
        assertEquals(300L, reopened.updatedAt)
    }

    @Test fun task_validation_rejects_invalid_input_and_deleted_edits() {
        listOf("", "  ", "x".repeat(121)).forEach { title ->
            assertThrows(IllegalArgumentException::class.java) { prepareTask(Task(title = title), null, 100) }
        }
        assertThrows(IllegalArgumentException::class.java) { prepareTask(Task(title = "OK", notes = "x".repeat(2001)), null, 100) }
        assertThrows(IllegalArgumentException::class.java) { prepareTask(Task(id = 99, title = "Missing"), null, 100) }
        val deleted = Task(id = 1, title = "Deleted", deletedAt = 80)
        assertThrows(IllegalArgumentException::class.java) { prepareTask(deleted, deleted, 100) }
        assertEquals(120, prepareTask(Task(title = "x".repeat(120), notes = "x".repeat(2000)), null, 100).title.length)
    }

    @Test fun events_validate_time_order_and_preserve_identity() {
        val initial = prepareEvent(Event(title = "  Kelas  ", startAt = 100, endAt = 100), null, 200).copy(id = 1)
        val edited = prepareEvent(initial.copy(title = "Kelas baru", createdAt = 999), initial, 50)
        assertEquals(200L, edited.createdAt)
        assertEquals(200L, edited.updatedAt)
        assertEquals(initial.syncId, edited.syncId)
        assertThrows(IllegalArgumentException::class.java) { prepareEvent(initial.copy(endAt = 99), initial, 300) }
        assertThrows(IllegalArgumentException::class.java) { prepareEvent(initial.copy(title = " "), initial, 300) }
        assertThrows(IllegalArgumentException::class.java) { prepareEvent(initial.copy(notes = "x".repeat(2001)), initial, 300) }
        assertThrows(IllegalArgumentException::class.java) { prepareEvent(initial.copy(deletedAt = 250), initial.copy(deletedAt = 250), 300) }
    }
}
