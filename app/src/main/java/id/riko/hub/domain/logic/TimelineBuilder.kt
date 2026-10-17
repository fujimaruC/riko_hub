package id.riko.hub.domain.logic

import id.riko.hub.domain.model.Event
import id.riko.hub.domain.model.Task
import id.riko.hub.domain.model.TaskStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class TimelineStopKind { EVENT, DEADLINE, COMPLETED }

data class TimelineStop(
    val key: String,
    val at: Long,
    val endAt: Long?,
    val task: Task? = null,
    val event: Event? = null,
    val kind: TimelineStopKind,
)

data class TimelineDay(val date: LocalDate, val stops: List<TimelineStop>)

fun timelineWeek(date: LocalDate): LocalDate =
    date.minusDays((date.dayOfWeek.value - 1).toLong())

fun buildTimeline(
    tasks: List<Task>,
    events: List<Event>,
    weekStart: LocalDate,
    zone: ZoneId,
): List<TimelineDay> {
    val dates = (0L..6L).map(weekStart::plusDays)
    val starts = dates.map { it.atStartOfDay(zone).toInstant().toEpochMilli() }
    val ends = dates.mapIndexed { index, _ ->
        dates.getOrNull(index + 1)?.let { it.atStartOfDay(zone).toInstant().toEpochMilli() }
            ?: dates[index].plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    }
    val stops = dates.associateWith { mutableListOf<TimelineStop>() }

    tasks.asSequence().filter { it.deletedAt == null }.forEach { task ->
        task.deadlineAt?.let { addTaskStop(task, it, TimelineStopKind.DEADLINE, zone, stops) }
        if (task.status == TaskStatus.DONE) {
            task.completedAt?.let { addTaskStop(task, it, TimelineStopKind.COMPLETED, zone, stops) }
        }
    }

    events.asSequence().filter { it.deletedAt == null && (it.endAt ?: it.startAt) >= it.startAt }.forEach { event ->
        dates.forEachIndexed { index, day ->
            val start = starts[index]
            val end = ends[index]
            val eventEnd = event.endAt ?: event.startAt
            val point = eventEnd == event.startAt
            if ((point && event.startAt >= start && event.startAt < end) ||
                (!point && event.startAt < end && eventEnd > start)
            ) {
                stops.getValue(day) += TimelineStop(
                    key = "event:${event.id}:$day",
                    at = maxOf(event.startAt, start),
                    endAt = event.endAt?.let { minOf(it, end) },
                    event = event,
                    kind = TimelineStopKind.EVENT,
                )
            }
        }
    }

    return dates.map { day ->
        TimelineDay(day, stops.getValue(day).sortedWith(
            compareBy<TimelineStop> { it.at }
                .thenBy { it.kind.ordinal }
                .thenBy { it.task?.id ?: it.event!!.id },
        ))
    }
}

private fun addTaskStop(
    task: Task,
    at: Long,
    kind: TimelineStopKind,
    zone: ZoneId,
    days: Map<LocalDate, MutableList<TimelineStop>>,
) {
    val date = Instant.ofEpochMilli(at).atZone(zone).toLocalDate()
    days[date]?.add(TimelineStop(
        key = "${kind.name.lowercase()}:${task.id}",
        at = at,
        endAt = null,
        task = task,
        kind = kind,
    ))
}
