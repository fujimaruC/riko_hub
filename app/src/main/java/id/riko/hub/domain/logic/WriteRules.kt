package id.riko.hub.domain.logic

import id.riko.hub.domain.model.Event
import id.riko.hub.domain.model.Task
import id.riko.hub.domain.model.TaskStatus
import id.riko.hub.domain.model.Recurrence

private fun validateText(title: String, notes: String?) {
    require(title.trim().length in 1..120) { "Title must contain 1 to 120 characters" }
    require(notes == null || notes.length <= 2000) { "Notes must contain at most 2000 characters" }
}

fun prepareTask(task: Task, previous: Task?, now: Long): Task {
    validateText(task.title, task.notes)
    require(if (previous == null) task.id == 0L else task.id == previous.id) { "Task does not exist" }
    require(previous?.deletedAt == null) { "Restore the task before editing it" }
    require(task.estimatedMinutes == null || task.estimatedMinutes in 1..1440) { "Estimate must be from 1 to 1440 minutes" }
    require(task.reminderMinutesBefore == null || task.reminderMinutesBefore in 0..10080) { "Reminder must be from 0 to 10080 minutes" }
    require(task.reminderMinutesBefore == null || task.deadlineAt != null) { "Reminder requires a deadline" }
    require(task.recurrence == Recurrence.NONE || task.deadlineAt != null) { "Recurrence requires a deadline" }
    require(previous != null || (task.generatedFromTaskId == null && !task.generationPristine && task.recurrenceAnchorAt == null)) { "Generated task fields are internal" }
    require(task.checklist.size <= 100) { "Checklist must contain at most 100 items" }
    require(task.checklist.all { it.id.isNotBlank() && it.title.trim().length in 1..120 }) { "Checklist titles must contain 1 to 120 characters" }
    require(task.checklist.map { it.id }.toSet().size == task.checklist.size) { "Checklist item IDs must be unique" }
    val timestamp = maxOf(now, previous?.createdAt ?: now, previous?.updatedAt ?: now)
    return task.copy(
        title = task.title.trim(), syncId = previous?.syncId ?: task.syncId,
        createdAt = previous?.createdAt ?: timestamp, updatedAt = timestamp,
        completedAt = if (task.status == TaskStatus.DONE) previous?.completedAt ?: timestamp else null,
        deletedAt = null,
        checklist = task.checklist.map { it.copy(title = it.title.trim()) },
        generatedFromTaskId = previous?.generatedFromTaskId,
        generationPristine = false,
        recurrenceAnchorAt = when {
            task.recurrence == Recurrence.NONE -> null
            previous != null && previous.recurrence == task.recurrence && previous.deadlineAt == task.deadlineAt &&
                previous.hasDeadlineTime == task.hasDeadlineTime -> previous.recurrenceAnchorAt ?: task.deadlineAt
            else -> task.deadlineAt
        },
    )
}

fun prepareEvent(event: Event, previous: Event?, now: Long): Event {
    validateText(event.title, event.notes)
    require(event.endAt == null || event.endAt >= event.startAt) { "Event end must not precede start" }
    require(if (previous == null) event.id == 0L else event.id == previous.id) { "Event does not exist" }
    require(previous?.deletedAt == null) { "Restore the event before editing it" }
    val timestamp = maxOf(now, previous?.createdAt ?: now, previous?.updatedAt ?: now)
    return event.copy(
        title = event.title.trim(), syncId = previous?.syncId ?: event.syncId,
        createdAt = previous?.createdAt ?: timestamp, updatedAt = timestamp, deletedAt = null,
    )
}
