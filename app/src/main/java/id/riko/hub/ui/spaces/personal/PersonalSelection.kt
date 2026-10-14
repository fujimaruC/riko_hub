package id.riko.hub.ui.spaces.personal

import id.riko.hub.domain.logic.taskReminderSchedule
import id.riko.hub.domain.model.Category
import id.riko.hub.domain.model.Recurrence
import id.riko.hub.domain.model.Task
import id.riko.hub.domain.model.TaskStatus
import java.time.Instant
import java.time.ZoneId

internal fun personalTasks(tasks: List<Task>): List<Task> = tasks.filter {
    it.category == Category.PERSONAL && it.deletedAt == null
}

internal fun personalTodayTasks(tasks: List<Task>, now: Long, zone: ZoneId): List<Task> {
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    return personalTasks(tasks).filter { task ->
        val completedToday = task.completedAt?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() == today } == true
        completedToday || (task.status != TaskStatus.DONE && (task.deadlineAt == null ||
            Instant.ofEpochMilli(task.deadlineAt).atZone(zone).toLocalDate() == today))
    }.sortedWith(compareBy<Task>({ it.status == TaskStatus.DONE }, { it.deadlineAt ?: Long.MAX_VALUE }, { it.title.lowercase() }))
}

internal fun personalDailyRoutines(tasks: List<Task>, now: Long, zone: ZoneId): List<Task> {
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    return personalTasks(tasks).filter { task ->
        task.recurrence == Recurrence.DAILY && (task.completedAt?.let {
            Instant.ofEpochMilli(it).atZone(zone).toLocalDate() == today
        } == true || task.status != TaskStatus.DONE && task.deadlineAt?.let {
            !Instant.ofEpochMilli(it).atZone(zone).toLocalDate().isAfter(today)
        } == true)
    }
}

internal fun personalTodayReminders(tasks: List<Task>, now: Long, zone: ZoneId): List<Pair<Task, Long>> {
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    return personalTasks(tasks).mapNotNull { task ->
        taskReminderSchedule(task, now)?.takeIf {
            Instant.ofEpochMilli(it.reminderAt).atZone(zone).toLocalDate() == today
        }?.let { task to it.reminderAt }
    }.sortedBy { it.second }
}

internal fun personalNotes(tasks: List<Task>): List<Task> = personalTasks(tasks)
    .filter { !it.notes.isNullOrBlank() }
    .sortedByDescending { it.updatedAt }

internal fun personalNoteRows(notes: List<Task>, twoColumns: Boolean): List<List<Task>> {
    if (!twoColumns) return notes.map { listOf(it) }
    val rows = mutableListOf<List<Task>>()
    val pair = mutableListOf<Task>()
    notes.forEach { task ->
        if ((task.notes?.length ?: 0) > 120) {
            if (pair.isNotEmpty()) rows += pair.toList().also { pair.clear() }
            rows += listOf(task)
        } else {
            pair += task
            if (pair.size == 2) rows += pair.toList().also { pair.clear() }
        }
    }
    if (pair.isNotEmpty()) rows += pair.toList()
    return rows
}
