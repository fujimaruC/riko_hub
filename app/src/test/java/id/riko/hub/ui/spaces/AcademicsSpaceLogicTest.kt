package id.riko.hub.ui.spaces

import id.riko.hub.domain.model.Course
import id.riko.hub.domain.model.CourseWeeklySlot
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AcademicsSpaceLogicTest {
    @Test fun weeklySlotUsesLocalDayAndHalfOpenTimeRange() {
        val zone = ZoneId.of("Asia/Jakarta")
        val course = Course(title = "Design", weeklySlots = listOf(CourseWeeklySlot(dayOfWeek = 1, startMinute = 9 * 60, endMinute = 11 * 60)))
        fun at(hour: Int, minute: Int) = LocalDate.of(2026, 10, 12).atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()

        assertTrue(course.hasCurrentSlot(at(9, 0), zone))
        assertTrue(course.hasCurrentSlot(at(10, 59), zone))
        assertFalse(course.hasCurrentSlot(at(11, 0), zone))
        assertFalse(course.hasCurrentSlot(at(8, 59), zone))
        assertFalse(course.hasCurrentSlot(at(9, 30), ZoneOffset.UTC))
    }
}
