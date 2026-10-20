package id.riko.hub.ui.components

import id.riko.hub.domain.model.ChecklistItem
import id.riko.hub.domain.model.Recurrence
import id.riko.hub.domain.model.Task
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextualEditorValidationTest {
    @Test fun ordinary_task_keeps_shared_title_validation() {
        assertTrue(validTaskInput(Task(title = "Task"), "", ""))
        assertFalse(validTaskInput(Task(title = "  "), "", ""))
    }

    @Test fun note_requires_a_nonblank_body() {
        assertFalse(validTaskInput(Task(title = "Note"), "", "", TaskEditorPresentation.NOTE))
        assertFalse(validTaskInput(Task(title = "Note", notes = "  \n"), "", "", TaskEditorPresentation.NOTE))
        assertTrue(validTaskInput(Task(title = "Note", notes = "Body"), "", "", TaskEditorPresentation.NOTE))
    }

    @Test fun routine_requires_daily_recurrence_and_a_deadline() {
        fun valid(recurrence: Recurrence, deadlineAt: Long?) = validTaskInput(
            Task(title = "Routine", recurrence = recurrence, deadlineAt = deadlineAt), "", "", TaskEditorPresentation.ROUTINE)

        assertFalse(valid(Recurrence.DAILY, null))
        assertTrue(valid(Recurrence.DAILY, 1L))
        assertFalse(valid(Recurrence.NONE, 1L))
        assertFalse(valid(Recurrence.WEEKLY, 1L))
    }

    @Test fun project_checklist_still_rejects_duplicate_ids() {
        val duplicate = ChecklistItem(id = "same", title = "One")
        val project = Task(title = "Project", checklist = listOf(duplicate, duplicate.copy(title = "Two")))

        assertFalse(validTaskInput(project, "", "", TaskEditorPresentation.PROJECT))
    }
}
