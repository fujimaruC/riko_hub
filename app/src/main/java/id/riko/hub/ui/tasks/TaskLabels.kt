package id.riko.hub.ui.tasks

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import id.riko.hub.R
import id.riko.hub.domain.logic.TaskBucket
import id.riko.hub.domain.model.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun Category.label() = when (this) {
    Category.ACADEMIC -> R.string.academic
    Category.ORGANIZATION -> R.string.organization
    Category.PERSONAL -> R.string.personal
    Category.PROJECTS -> R.string.projects
}
fun Priority.label() = when (this) {
    Priority.LOW -> R.string.priority_low
    Priority.MEDIUM -> R.string.priority_medium
    Priority.HIGH -> R.string.priority_high
}
fun TaskStatus.label() = when (this) {
    TaskStatus.NOT_STARTED -> R.string.not_started
    TaskStatus.IN_PROGRESS -> R.string.task_in_progress
    TaskStatus.DONE -> R.string.done
}
fun TaskBucket.label() = when (this) {
    TaskBucket.OVERDUE -> R.string.overdue
    TaskBucket.TODAY -> R.string.today
    TaskBucket.TOMORROW -> R.string.tomorrow
    TaskBucket.THIS_WEEK -> R.string.this_week
    TaskBucket.LATER -> R.string.later
    TaskBucket.DONE -> R.string.done
}

val taskDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("id-ID"))
val taskTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH.mm", Locale.forLanguageTag("id-ID"))

@Composable
fun deadlineLabel(task: Task, now: Long, zone: ZoneId): String {
    val deadline = task.deadlineAt ?: return stringResource(R.string.no_deadline)
    val value = Instant.ofEpochMilli(deadline).atZone(zone)
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val date = when (value.toLocalDate()) {
        today -> stringResource(R.string.today)
        today.plusDays(1) -> stringResource(R.string.tomorrow)
        else -> value.format(taskDateFormat)
    }
    return if (task.hasDeadlineTime) stringResource(R.string.deadline_with_time, date, value.format(taskTimeFormat)) else date
}
