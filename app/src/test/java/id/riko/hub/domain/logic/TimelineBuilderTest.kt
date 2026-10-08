package id.riko.hub.domain.logic

import id.riko.hub.domain.model.Event
import id.riko.hub.domain.model.Task
import id.riko.hub.domain.model.TaskStatus
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineBuilderTest {
    private val zone = ZoneId.of("Asia/Makassar")
    private val monday = LocalDate.of(2026, 10, 5)

    private fun at(date: LocalDate, hour: Int = 0, minute: Int = 0, zone: ZoneId = this.zone) =
        date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()

    private fun task(
        id: Long,
        deadline: Long? = null,
        status: TaskStatus = TaskStatus.NOT_STARTED,
        completed: Long? = null,
        deleted: Long? = null,
    ) = Task(id = id, title = "Task $id", deadlineAt = deadline, status = status, completedAt = completed, deletedAt = deleted)

    private fun event(id: Long, start: Long, end: Long? = null, deleted: Long? = null) =
        Event(id = id, title = "Event $id", startAt = start, endAt = end, deletedAt = deleted)

    @Test fun week_starts_monday_and_build_keeps_all_seven_days() {
        val sunday = monday.plusDays(6)
        assertEquals(monday, timelineWeek(sunday))
        assertEquals(monday, timelineWeek(monday))

        val days = buildTimeline(emptyList(), emptyList(), timelineWeek(sunday), zone)
        assertEquals((0L..6L).map(monday::plusDays), days.map { it.date })
        assertTrue(days.all { it.stops.isEmpty() })
    }

    @Test fun same_time_stops_sort_by_kind_then_entity_id_and_keep_deadline_and_completion() {
        val time = at(monday, 9)
        val events = listOf(event(8, time), event(3, time))
        val tasks = listOf(
            task(9, deadline = time),
            task(4, deadline = time, status = TaskStatus.DONE, completed = time),
            task(2, deadline = time),
        )

        val stops = buildTimeline(tasks, events, monday, zone).first().stops
        assertEquals(
            listOf(
                "event:3:$monday", "event:8:$monday",
                "deadline:2", "deadline:4", "deadline:9", "completed:4",
            ),
            stops.map { it.key },
        )
        assertEquals(time, stops.first().at)
        assertEquals(
            stops,
            buildTimeline(tasks.reversed(), events.reversed(), monday, zone).first().stops,
        )
        assertEquals(2, stops.count { it.task?.id == 4L })
    }

    @Test fun deleted_tasks_and_events_are_excluded() {
        val time = at(monday, 11)
        val stops = buildTimeline(
            tasks = listOf(task(1, deadline = time), task(2, deadline = time, deleted = time)),
            events = listOf(event(3, time), event(4, time, deleted = time)),
            weekStart = monday,
            zone = zone,
        ).first().stops

        assertEquals(listOf("event:3:$monday", "deadline:1"), stops.map { it.key })
    }

    @Test fun week_and_day_ranges_are_half_open_and_point_events_appear_once() {
        val nextMonday = monday.plusDays(7)
        val mondayStart = at(monday)
        val sundayLastMillisecond = at(nextMonday) - 1
        val days = buildTimeline(
            tasks = listOf(
                task(1, deadline = mondayStart),
                task(2, deadline = at(nextMonday)),
                task(3, status = TaskStatus.DONE, completed = sundayLastMillisecond),
            ),
            events = listOf(
                event(1, mondayStart),
                event(2, mondayStart - 60_000, mondayStart),
                event(3, at(nextMonday)),
                event(4, sundayLastMillisecond, sundayLastMillisecond),
            ),
            weekStart = monday,
            zone = zone,
        )

        assertEquals(listOf("event:1:$monday", "deadline:1"), days.first().stops.map { it.key })
        assertEquals(listOf("event:4:${nextMonday.minusDays(1)}", "completed:3"), days.last().stops.map { it.key })
        assertEquals(1, days.sumOf { day -> day.stops.count { it.key.startsWith("event:4:") } })
        assertFalse(days.any { day -> day.stops.any { it.key == "deadline:2" || it.key == "event:3:$nextMonday" } })
    }

    @Test fun multi_day_events_clip_to_each_overlapping_day() {
        val tuesday = monday.plusDays(1)
        val wednesday = monday.plusDays(2)
        val days = buildTimeline(
            tasks = emptyList(),
            events = listOf(
                event(1, at(monday.minusDays(1), 22), at(wednesday, 2)),
                event(2, at(monday, 12), at(tuesday)),
                event(3, at(tuesday, 9), at(tuesday, 9)),
            ),
            weekStart = monday,
            zone = zone,
        )

        val mondayStops = days[0].stops
        assertEquals(listOf("event:1:$monday", "event:2:$monday"), mondayStops.map { it.key })
        assertEquals(at(monday), mondayStops[0].at)
        assertEquals(at(tuesday), mondayStops[0].endAt)
        assertEquals(at(monday, 12), mondayStops[1].at)
        assertEquals(at(tuesday), mondayStops[1].endAt)

        val tuesdayStops = days[1].stops
        assertEquals(listOf("event:1:$tuesday", "event:3:$tuesday"), tuesdayStops.map { it.key })
        assertEquals(at(tuesday), tuesdayStops[0].at)
        assertEquals(at(wednesday), tuesdayStops[0].endAt)
        assertEquals(at(tuesday, 9), tuesdayStops[1].at)
        assertEquals(at(tuesday, 9), tuesdayStops[1].endAt)
    }

    @Test fun day_clipping_uses_dst_aware_local_midnights() {
        val dstZone = ZoneId.of("America/New_York")
        val dstWeek = LocalDate.of(2026, 3, 2)
        val sunday = dstWeek.plusDays(6)
        val nextMondayStart = at(sunday.plusDays(1), zone = dstZone)
        val event = event(1, at(sunday.minusDays(1), 23, 0, dstZone), at(sunday.plusDays(1), 1, 0, dstZone))

        val days = buildTimeline(emptyList(), listOf(event), dstWeek, dstZone)
        val stop = days.last().stops.single()
        assertEquals(at(sunday, zone = dstZone), stop.at)
        assertEquals(nextMondayStart, stop.endAt)
        assertEquals(23 * 60 * 60 * 1000L, nextMondayStart - at(sunday, zone = dstZone))
    }
}
