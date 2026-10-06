package id.riko.hub.domain.logic

import id.riko.hub.domain.model.Category
import id.riko.hub.domain.model.Task
import id.riko.hub.domain.model.TaskStatus
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

enum class TaskBucket { OVERDUE, TODAY, TOMORROW, THIS_WEEK, LATER, DONE }

fun taskBucket(task: Task, now: Long, zone: ZoneId): TaskBucket {
    if (task.status == TaskStatus.DONE) return TaskBucket.DONE
    val deadline = task.deadlineAt ?: return TaskBucket.LATER
    if (deadline < now) return TaskBucket.OVERDUE
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val date = Instant.ofEpochMilli(deadline).atZone(zone).toLocalDate()
    return when {
        date == today -> TaskBucket.TODAY
        date == today.plusDays(1) -> TaskBucket.TOMORROW
        date <= today.plusDays(7) -> TaskBucket.THIS_WEEK
        else -> TaskBucket.LATER
    }
}

fun groupTasks(tasks: List<Task>, filter: Category?, now: Long, zone: ZoneId): Map<TaskBucket, List<Task>> {
    val visible = tasks.filter { it.deletedAt == null && (filter == null || it.category == filter) }
        .sortedWith(compareBy<Task> { it.deadlineAt ?: Long.MAX_VALUE }.thenBy { it.id })
    return TaskBucket.entries.associateWith { bucket -> visible.filter { taskBucket(it, now, zone) == bucket } }
        .filterValues { it.isNotEmpty() }
}

fun taskDeadline(date: LocalDate, time: LocalTime?, zone: ZoneId): Long =
    date.atTime(time ?: LocalTime.of(23, 59)).atZone(zone).toInstant().toEpochMilli()
