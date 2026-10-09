package id.riko.hub.domain.logic

import id.riko.hub.domain.model.Recurrence
import id.riko.hub.domain.model.Task
import id.riko.hub.domain.model.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class TaskReminderDecisionTest {
    private val now = 1_000_000L
    private fun task(id: Long = 1, deadline: Long = now + 60_000, offset: Int? = 0) =
        Task(id = id, title = "Tugas", deadlineAt = deadline, reminderMinutesBefore = offset)

    @Test fun zeroOffsetAndDateOnlyStoredDeadlineScheduleDirectly() {
        val dateOnlyDeadline = taskDeadline(LocalDate.of(2026, 10, 20), null, ZoneId.of("Asia/Makassar"))
        assertEquals(dateOnlyDeadline - 30 * 60_000L,
            task(deadline = dateOnlyDeadline, offset = 30).let { taskReminderSchedule(it, 0)?.reminderAt })
        assertEquals(now + 60_000, task().let { taskReminderSchedule(it, now)?.reminderAt })
    }

    @Test fun pastInvalidAndOverflowingTimesAreSkipped() {
        assertNull(task(deadline = now).let { taskReminderSchedule(it, now) })
        assertNull(task(offset = null).let { taskReminderSchedule(it, now) })
        assertNull(task(offset = -1).let { taskReminderSchedule(it, now) })
        assertNull(task(offset = 10081).let { taskReminderSchedule(it, now) })
        assertNull(task(deadline = Long.MIN_VALUE, offset = 10080).let { taskReminderSchedule(it, now) })
    }

    @Test fun deliveryTokenRejectsEditsCompletionAndDeletion() {
        val token = requireNotNull(taskReminderToken(task()))
        assertTrue(token.matches(task()))
        assertEquals(false, token.matches(task().copy(deadlineAt = now + 120_000)))
        assertEquals(false, token.matches(task().copy(reminderMinutesBefore = 1)))
        assertEquals(false, token.matches(task().copy(status = TaskStatus.DONE)))
        assertEquals(false, token.matches(task().copy(deletedAt = now)))
        val sameFireTimeEdit = task(deadline = now + 120_000, offset = 1)
        assertEquals(token.reminderAt, taskReminderToken(sameFireTimeEdit)?.reminderAt)
        assertEquals(false, token.matches(sameFireTimeEdit))
    }

    @Test fun recurringParentCompletionAndGeneratedChildHaveIndependentSchedules() {
        val parent = task().copy(recurrence = Recurrence.DAILY)
        val child = task(id = 2, deadline = now + 86_460_000).copy(generatedFromTaskId = parent.id)
        assertNull(taskReminderToken(parent.copy(status = TaskStatus.DONE)))
        assertEquals(parent.id, taskReminderToken(parent)?.taskId)
        assertEquals(child.id, taskReminderToken(child)?.taskId)
        assertEquals(false, taskReminderToken(parent) == taskReminderToken(child))
    }

    @Test fun reconciliationKeepsMatchingLateWorkAndSkipsNewPastWork() {
        val late = task(deadline = now + 30_000)
        val token = requireNotNull(taskReminderToken(late))
        assertEquals(ReminderWorkAction.KEEP, taskReminderWorkAction(late, now + 60_000, token, true))
        assertEquals(ReminderWorkAction.CANCEL, taskReminderWorkAction(late, now + 60_000, null, false))
        assertEquals(ReminderWorkAction.ENQUEUE, taskReminderWorkAction(late, now, null, false))
        assertEquals(ReminderWorkAction.ENQUEUE, taskReminderWorkAction(late.copy(deadlineAt = now + 90_000), now, token, true))
    }
}
