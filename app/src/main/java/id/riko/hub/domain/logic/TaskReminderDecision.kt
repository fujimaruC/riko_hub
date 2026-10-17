package id.riko.hub.domain.logic

import id.riko.hub.domain.model.Task
import id.riko.hub.domain.model.TaskStatus

data class TaskReminderSchedule(val taskId: Long, val deadlineAt: Long, val offsetMinutes: Int, val reminderAt: Long)

fun taskReminderSchedule(task: Task, now: Long): TaskReminderSchedule? {
    val schedule = taskReminderToken(task) ?: return null
    return schedule.takeIf { it.reminderAt > now }
}

fun taskReminderToken(task: Task): TaskReminderSchedule? {
    val deadline = task.deadlineAt ?: return null
    val offset = task.reminderMinutesBefore ?: return null
    if (task.id <= 0 || task.deletedAt != null || task.status == TaskStatus.DONE || offset !in 0..10080) return null
    val reminderAt = try { Math.subtractExact(deadline, offset * 60_000L) } catch (_: ArithmeticException) { return null }
    return TaskReminderSchedule(task.id, deadline, offset, reminderAt)
}

fun TaskReminderSchedule.matches(task: Task): Boolean = task.id == taskId && task.deadlineAt == deadlineAt &&
    task.reminderMinutesBefore == offsetMinutes && task.deletedAt == null && task.status != TaskStatus.DONE

enum class ReminderWorkAction { KEEP, ENQUEUE, CANCEL }

fun taskReminderWorkAction(task: Task, now: Long, existing: TaskReminderSchedule?, existingWorkActive: Boolean): ReminderWorkAction {
    val current = taskReminderToken(task) ?: return ReminderWorkAction.CANCEL
    if (existingWorkActive && existing == current) return ReminderWorkAction.KEEP
    return if (current.reminderAt > now) ReminderWorkAction.ENQUEUE else ReminderWorkAction.CANCEL
}
