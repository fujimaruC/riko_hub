package id.riko.hub.ui.spaces

import id.riko.hub.domain.model.Category
import id.riko.hub.domain.model.Task
import id.riko.hub.domain.model.TaskStatus

internal enum class ProjectFilter { ACTIVE, ALL, DONE }

internal fun projectTasks(tasks: List<Task>): List<Task> = tasks.asSequence()
    .filter { it.category == Category.PROJECTS && it.deletedAt == null }
    .sortedWith(compareByDescending<Task> { it.priority.ordinal }
        .thenBy { it.deadlineAt ?: Long.MAX_VALUE }.thenBy { it.createdAt }.thenBy { it.syncId })
    .toList()

internal fun initialProjectSelection(projects: List<Task>): Task? =
    projects.firstOrNull { it.status != TaskStatus.DONE } ?: projects.firstOrNull()

internal fun visibleProjects(projects: List<Task>, filter: ProjectFilter): List<Task> = when (filter) {
    ProjectFilter.ACTIVE -> projects.filter { it.status != TaskStatus.DONE }
    ProjectFilter.ALL -> projects
    ProjectFilter.DONE -> projects.filter { it.status == TaskStatus.DONE }
}

internal fun projectDeckIndex(index: Int, count: Int): Int =
    if (count <= 0) 0 else Math.floorMod(index, count)

internal fun nextProjectDeckIndex(index: Int, direction: Int, count: Int): Int =
    if (count <= 1) projectDeckIndex(index, count) else Math.floorMod(index + direction, count)

internal fun projectMilestoneFraction(task: Task): Float? = task.checklist.takeIf { it.isNotEmpty() }
    ?.let { items -> items.count { it.done }.toFloat() / items.size }

internal fun projectIdentityIndex(syncId: String, count: Int): Int = Math.floorMod(syncId.hashCode(), count)
