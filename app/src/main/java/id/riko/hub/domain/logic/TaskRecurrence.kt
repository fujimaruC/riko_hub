package id.riko.hub.domain.logic

import id.riko.hub.domain.model.Recurrence
import id.riko.hub.domain.model.Task
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

fun nextTaskDeadline(task: Task, now: Long, zone: ZoneId): Long? {
    val deadline = task.deadlineAt ?: return null
    val base = Instant.ofEpochMilli(task.recurrenceAnchorAt ?: deadline).atZone(zone)
    val time = if (task.hasDeadlineTime) base.toLocalTime() else LocalTime.of(23, 59)
    val threshold = maxOf(deadline, now)
    val after = Instant.ofEpochMilli(threshold).atZone(zone)
    val date = when (task.recurrence) {
        Recurrence.NONE -> return null
        Recurrence.DAILY -> nextDate(base.toLocalDate(), after.toLocalDate(), 1, time, zone, threshold)
        Recurrence.WEEKLY -> nextDate(base.toLocalDate(), after.toLocalDate(), 7, time, zone, threshold)
        Recurrence.MONTHLY -> nextMonth(base.toLocalDate(), after.toLocalDate(), time, zone, threshold)
    }
    return date
}

private fun nextDate(base: LocalDate, after: LocalDate, intervalDays: Long, time: LocalTime, zone: ZoneId, threshold: Long): Long {
    val steps = maxOf(1, Math.floorDiv(ChronoUnit.DAYS.between(base, after), intervalDays))
    val candidate = base.plusDays(steps * intervalDays)
    val result = candidate.atTime(time).atZone(zone).toInstant().toEpochMilli()
    return if (result > threshold) result else base.plusDays((steps + 1) * intervalDays)
        .atTime(time).atZone(zone).toInstant().toEpochMilli()
}

private fun nextMonth(base: LocalDate, after: LocalDate, time: LocalTime, zone: ZoneId, threshold: Long): Long {
    val elapsedMonths = ChronoUnit.MONTHS.between(base.withDayOfMonth(1), after.withDayOfMonth(1))
    val steps = maxOf(1, elapsedMonths)
    val candidate = base.plusMonths(steps)
    val result = candidate.atTime(time).atZone(zone).toInstant().toEpochMilli()
    return if (result > threshold) result else base.plusMonths(steps + 1)
        .atTime(time).atZone(zone).toInstant().toEpochMilli()
}
