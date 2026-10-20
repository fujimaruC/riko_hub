package id.riko.hub.ui.spaces

import id.riko.hub.domain.model.Category
import id.riko.hub.domain.model.ChecklistItem
import id.riko.hub.domain.model.Priority
import id.riko.hub.domain.model.Task
import id.riko.hub.domain.model.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectsSelectionTest {
    @Test fun selection_and_filters_use_real_project_task_statuses() {
        val urgentDone = Task(id = 1, syncId = "done", title = "Done", category = Category.PROJECTS,
            priority = Priority.HIGH, status = TaskStatus.DONE)
        val active = Task(id = 2, syncId = "active", title = "Active", category = Category.PROJECTS,
            priority = Priority.MEDIUM, status = TaskStatus.IN_PROGRESS)
        val personal = Task(id = 3, title = "Personal", category = Category.PERSONAL, priority = Priority.HIGH)
        val deleted = Task(id = 4, title = "Deleted", category = Category.PROJECTS, deletedAt = 10)
        val projects = projectTasks(listOf(urgentDone, personal, deleted, active))

        assertEquals(listOf("done", "active"), projects.map { it.syncId })
        assertEquals("active", initialProjectSelection(projects)?.syncId)
        assertEquals(listOf("active"), visibleProjects(projects, ProjectFilter.ACTIVE).map { it.syncId })
        assertEquals(listOf("done", "active"), visibleProjects(projects, ProjectFilter.ALL).map { it.syncId })
        assertEquals(listOf("done"), visibleProjects(projects, ProjectFilter.DONE).map { it.syncId })
        assertTrue(projectIdentityIndex("active", 5) in 0..4)
    }

    @Test fun milestone_progress_is_absent_without_a_checklist() {
        val noMilestones = Task(title = "Project", category = Category.PROJECTS)
        val milestones = noMilestones.copy(checklist = listOf(ChecklistItem(title = "One", done = true), ChecklistItem(title = "Two")))

        assertNull(projectMilestoneFraction(noMilestones))
        assertEquals(.5f, projectMilestoneFraction(milestones)!!, 0f)
    }

    @Test fun project_deck_cycles_and_handles_zero_or_one_card() {
        assertEquals(0, projectDeckIndex(0, 0))
        assertEquals(0, nextProjectDeckIndex(0, 1, 0))
        assertEquals(0, nextProjectDeckIndex(0, -1, 1))
        assertEquals(1, nextProjectDeckIndex(0, 1, 3))
        assertEquals(2, nextProjectDeckIndex(0, -1, 3))
        assertEquals(0, nextProjectDeckIndex(2, 1, 3))
    }
}
