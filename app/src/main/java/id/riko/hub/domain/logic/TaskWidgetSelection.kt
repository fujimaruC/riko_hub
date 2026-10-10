package id.riko.hub.domain.logic

import id.riko.hub.core.time.Clock
import id.riko.hub.domain.model.Task
import id.riko.hub.domain.model.TaskStatus
import java.time.Instant
import java.time.temporal.ChronoUnit

fun selectWidgetTasks(tasks: List<Task>, clock: Clock): List<UrgencyInfo> =
    UrgencyCalculator.rank(tasks, clock).take(3)

fun nextWidgetRefreshAt(tasks: List<Task>, clock: Clock): Long? {
    val now = clock.now()
    val zone = clock.zone()
    val eligible = tasks.filter { it.deletedAt == null && it.status != TaskStatus.DONE }
    if (eligible.isEmpty()) return null

    val nextMidnight = Instant.ofEpochMilli(now).atZone(zone).toLocalDate().plusDays(1)
        .atStartOfDay(zone).toInstant().toEpochMilli()
    return buildList {
        if (nextMidnight > now) add(nextMidnight)
        for (deadline in eligible.mapNotNull { it.deadlineAt }) {
            if (deadline > now && deadline >= Long.MIN_VALUE + THREE_HOURS) {
                val urgencyBoundary = deadline - THREE_HOURS
                if (urgencyBoundary > now) add(urgencyBoundary)
            }
            if (deadline < Long.MAX_VALUE) {
                val overdueBoundary = deadline + 1
                if (overdueBoundary > now) add(overdueBoundary)
            }
        }
    }.minOrNull()
}

private const val THREE_HOURS = 3 * 60 * 60 * 1000L
