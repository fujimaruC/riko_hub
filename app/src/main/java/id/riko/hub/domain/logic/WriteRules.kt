package id.riko.hub.domain.logic

import id.riko.hub.domain.model.Event
import id.riko.hub.domain.model.Task
import id.riko.hub.domain.model.TaskStatus

private fun validateText(title: String, notes: String?) {
    require(title.trim().length in 1..120) { "Title must contain 1 to 120 characters" }
    require(notes == null || notes.length <= 2000) { "Notes must contain at most 2000 characters" }
}

fun prepareTask(task: Task, previous: Task?, now: Long): Task {
    validateText(task.title, task.notes)
    require(if (previous == null) task.id == 0L else task.id == previous.id) { "Task does not exist" }
    require(previous?.deletedAt == null) { "Restore the task before editing it" }
    val timestamp = maxOf(now, previous?.createdAt ?: now, previous?.updatedAt ?: now)
    return task.copy(
        title = task.title.trim(), syncId = previous?.syncId ?: task.syncId,
        createdAt = previous?.createdAt ?: timestamp, updatedAt = timestamp,
        completedAt = if (task.status == TaskStatus.DONE) previous?.completedAt ?: timestamp else null,
        deletedAt = null,
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
