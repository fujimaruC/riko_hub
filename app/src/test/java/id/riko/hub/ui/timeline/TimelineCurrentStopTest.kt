package id.riko.hub.ui.timeline

import id.riko.hub.domain.logic.buildTimeline
import id.riko.hub.domain.logic.timelineWeek
import id.riko.hub.domain.model.Event
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class TimelineCurrentStopTest {
    @Test fun ongoing_multiday_event_marks_only_its_current_daily_segment() {
        val zone = ZoneId.of("Asia/Makassar")
        val monday = LocalDate.of(2026, 10, 12)
        fun at(day: Int, hour: Int) = monday.plusDays(day.toLong()).atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()
        val event = Event(id = 1, title = "Project session", startAt = at(0, 9), endAt = at(2, 12))
        val days = buildTimeline(emptyList(), listOf(event), timelineWeek(monday), zone)
        val activeDays = days.filter { day -> day.stops.any { it.isCurrentEvent(at(1, 10)) } }
        assertEquals(listOf(monday.plusDays(1)), activeDays.map { it.date })
        val tuesday = days[1].stops.single()
        assertFalse(tuesday.isCurrentEvent(at(2, 0)))
        assertTrue(days[2].stops.single().isCurrentEvent(at(2, 0)))
        assertFalse(days[2].stops.single().isCurrentEvent(at(2, 12)))
    }
}
