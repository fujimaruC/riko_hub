package id.riko.hub.ui.courses

import id.riko.hub.domain.model.Course
import id.riko.hub.domain.model.CourseWeeklySlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseTimeTest {
    @Test fun time_text_rejects_invalid_hours_and_minutes_and_allows_only_24_00() {
        assertEquals(0, parseTime("00:00"))
        assertEquals(1439, parseTime("23:59"))
        assertEquals(1440, parseTime("24:00"))
        assertNull(parseTime("12:60"))
        assertNull(parseTime("12:99"))
        assertNull(parseTime("24:01"))
        assertNull(parseTime("25:00"))
        assertNull(parseTime("9:00"))
    }

    @Test fun formatted_times_round_trip_valid_slot_boundaries() {
        listOf(0, 1, 9 * 60 + 5, 1439, 1440).forEach { minute ->
            assertEquals(minute, parseTime(formatTime(minute)))
        }
    }

    @Test fun shared_course_validation_enforces_slot_bounds_and_end_after_start() {
        val course = Course(title = "Matematika", weeklySlots = listOf(CourseWeeklySlot(1, 23 * 60, 1440)))
        assertTrue(validCourse(course))
        assertFalse(validCourse(course.copy(weeklySlots = listOf(CourseWeeklySlot(1, 1440, 1440)))))
        assertFalse(validCourse(course.copy(weeklySlots = listOf(CourseWeeklySlot(8, 60, 120)))))
        assertFalse(validCourse(course.copy(weeklySlots = List(29) { CourseWeeklySlot(1, 60, 120) })))
        assertFalse(validCourse(course.copy(weeklySlots = listOf(CourseWeeklySlot(1, 60, 60)))))
    }
}
