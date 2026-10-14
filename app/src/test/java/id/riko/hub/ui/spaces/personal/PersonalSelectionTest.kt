package id.riko.hub.ui.spaces.personal

import id.riko.hub.domain.model.Category
import id.riko.hub.domain.model.Recurrence
import id.riko.hub.domain.model.Task
import id.riko.hub.domain.model.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class PersonalSelectionTest {
    private val zone = ZoneId.of("Asia/Makassar")
    private val today = Instant.parse("2026-10-13T03:00:00Z").toEpochMilli()
    private fun at(hour: Int) = Instant.parse("2026-10-13T${"%02d".format(hour)}:00:00Z").toEpochMilli()

    @Test fun todaySelectionUsesLocalDateAndDoesNotPullTomorrowSuccessorForward() {
        val due = Task(id = 1, title = "Due today", deadlineAt = at(12))
        val tomorrow = Task(id = 2, title = "Generated tomorrow", deadlineAt = at(17), generatedFromTaskId = 1)
        val doneToday = Task(id = 3, title = "Done", status = TaskStatus.DONE, completedAt = today)
        val deleted = Task(id = 4, title = "Deleted", deletedAt = today)
        val academic = Task(id = 5, title = "Academic", category = Category.ACADEMIC)

        assertEquals(listOf(due, doneToday), personalTodayTasks(listOf(due, tomorrow, doneToday, deleted, academic), today, zone))
    }

    @Test fun routinesAndRemindersUseOnlyValidItemsForToday() {
        val routine = Task(id = 1, title = "Read", deadlineAt = at(12), recurrence = Recurrence.DAILY)
        val successor = Task(id = 2, title = "Read", deadlineAt = at(17), recurrence = Recurrence.DAILY, generatedFromTaskId = 1)
        val overdue = Task(id = 4, title = "Walk", deadlineAt = at(0), recurrence = Recurrence.DAILY)
        val reminder = routine.copy(reminderMinutesBefore = 30)
        val malformed = Task(id = 3, title = "Bad", deadlineAt = at(12), reminderMinutesBefore = -1)

        assertEquals(listOf(routine, overdue), personalDailyRoutines(listOf(routine, successor, overdue), today, zone))
        assertEquals(listOf(reminder to (at(12) - 30 * 60_000L)), personalTodayReminders(listOf(reminder, malformed), today, zone))
    }

    @Test fun notesRequireRealNonblankTaskNotes() {
        val note = Task(id = 1, title = "Idea", notes = "A real note", updatedAt = 2)
        assertEquals(listOf(note), personalNotes(listOf(note, note.copy(id = 2, notes = " "))))
    }

    @Test fun aWideNoteDoesNotDropItsUnpairedNeighborOrChangeReadingOrder() {
        val notes = (1L..5L).map { id ->
            Task(id = id, title = "Note $id", notes = if (id == 2L) "x".repeat(121) else "Short note")
        }
        val rows = personalNoteRows(notes, twoColumns = true)
        assertEquals(listOf(listOf(1L), listOf(2L), listOf(3L, 4L), listOf(5L)), rows.map { row -> row.map { it.id } })
        assertEquals(notes, rows.flatten())
        assertEquals(notes.map { listOf(it) }, personalNoteRows(notes, twoColumns = false))
    }
}
