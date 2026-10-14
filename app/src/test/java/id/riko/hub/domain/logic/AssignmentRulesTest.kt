package id.riko.hub.domain.logic

import id.riko.hub.domain.model.AcademicAssignment
import id.riko.hub.domain.model.AcademicState
import id.riko.hub.domain.model.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AssignmentRulesTest {
    @Test fun captures_original_deadline_and_submission_then_keeps_timestamp_on_reopen() {
        val task = Task(id = 8, title = "Essay", deadlineAt = 123, hasDeadlineTime = true, createdAt = 10, updatedAt = 11)
        val submitted = newAssignment(task, null, AcademicState.SUBMITTED, 20, "  https://example.test  ")
        assertEquals(123L, submitted.originalDeadlineAt)
        assertTrue(submitted.originalHasDeadlineTime)
        assertEquals(20L, submitted.submittedAt)
        assertEquals("https://example.test", submitted.submissionReference)

        val reopened = prepareAssignment(submitted.copy(state = AcademicState.IN_PROGRESS), submitted, task, 30, null)
        assertEquals(20L, reopened.submittedAt)
        assertEquals("https://example.test", reopened.submissionReference)
        val cleared = prepareAssignment(reopened, reopened, task, 31, "   ")
        assertNull(cleared.submissionReference)
    }

    @Test fun allows_explicit_completion_reopen_and_rejects_completed_to_submitted() {
        assertTrue(AcademicState.IN_PROGRESS in allowedAcademicTransitions(AcademicState.COMPLETED))
        assertTrue(AcademicState.SUBMITTED !in allowedAcademicTransitions(AcademicState.COMPLETED))
        val previous = AcademicAssignment(id = 1, taskId = 2, state = AcademicState.COMPLETED)
        assertTrue(runCatching {
            prepareAssignment(previous.copy(state = AcademicState.SUBMITTED), previous, Task(id = 2, title = "Task"), 5, null)
        }.isFailure)
    }

    @Test fun validates_timestamp_and_reference_size() {
        val task = Task(id = 2, title = "Task")
        assertTrue(runCatching { newAssignment(task, null, AcademicState.RECEIVED, -1, null) }.isFailure)
        assertTrue(runCatching { newAssignment(task, null, AcademicState.RECEIVED, 1, "x".repeat(2001)) }.isFailure)
    }

    @Test fun keeps_a_null_original_deadline_after_the_task_gets_one() {
        val originalTask = Task(id = 3, title = "Reading", createdAt = 1, updatedAt = 1)
        val initial = newAssignment(originalTask, null, AcademicState.RECEIVED, 2, null)
        val changedTask = originalTask.copy(deadlineAt = 50, hasDeadlineTime = true, updatedAt = 3)
        val edited = prepareAssignment(initial.copy(state = AcademicState.PLANNED), initial, changedTask, 4, null)
        assertNull(edited.originalDeadlineAt)
        assertEquals(false, edited.originalHasDeadlineTime)
    }
}
