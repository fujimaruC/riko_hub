package id.riko.hub.domain.logic

import id.riko.hub.domain.model.*
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class TaskGroupingTest {
    private val zone = ZoneId.of("Asia/Makassar")
    private val today = LocalDate.of(2026, 10, 4) // Sunday; the Tasks bucket is a rolling seven days.
    private val now = today.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
    private fun task(days: Long, id: Long = days + 10) = Task(id = id, title = "Task", deadlineAt = taskDeadline(today.plusDays(days), null, zone))

    @Test fun buckets_cover_past_today_tomorrow_seven_days_and_undated() {
        val tasks = listOf(task(-1), task(0), task(1), task(2), task(7), task(8), Task(title = "Undated"),
            task(-2).copy(status = TaskStatus.DONE), task(0, 99).copy(deletedAt = now))
        val groups = groupTasks(tasks, null, now, zone)
        assertEquals(listOf(1, 1, 1, 2, 2, 1), TaskBucket.entries.map { groups[it]?.size ?: 0 })
        assertEquals(TaskBucket.TODAY, taskBucket(task(0).copy(deadlineAt = now), now, zone))
        assertEquals(TaskBucket.OVERDUE, taskBucket(task(0).copy(deadlineAt = now - 1), now, zone))
    }

    @Test fun filters_and_ties_are_deterministic_and_done_is_separate() {
        val tasks = listOf(task(1, 2), task(1, 1), task(0).copy(category = Category.ACADEMIC))
        assertEquals(listOf(1L, 2L), groupTasks(tasks, Category.PERSONAL, now, zone)[TaskBucket.TOMORROW]!!.map { it.id })
        assertEquals(1, groupTasks(tasks, Category.ACADEMIC, now, zone).values.flatten().size)
        assertTrue(groupTasks(tasks, Category.PROJECTS, now, zone).isEmpty())
        assertEquals(TaskBucket.DONE, taskBucket(task(-1).copy(status = TaskStatus.DONE), now, zone))
    }

    @Test fun title_and_notes_search_intersects_category_and_whitespace_clears_search() {
        val tasks = listOf(
            task(0, 1).copy(title = "Read Biology", notes = "Chapter two"),
            task(0, 2).copy(title = "Other", notes = "BIOLOGY notes", category = Category.ACADEMIC),
            task(0, 3).copy(title = "Plan", category = Category.ORGANIZATION),
        )
        assertEquals(listOf(1L), groupTasks(tasks, Category.PERSONAL, now, zone, " biology ")
            .values.flatten().map { it.id })
        assertEquals(listOf(2L), groupTasks(tasks, Category.ACADEMIC, now, zone, "biology")
            .values.flatten().map { it.id })
        assertEquals(3, groupTasks(tasks, null, now, zone, "  ").values.flatten().size)
    }

    @Test fun midnight_timezone_and_dst_use_local_dates() {
        val midnight = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        assertEquals(TaskBucket.TOMORROW, taskBucket(task(1), midnight - 1, zone))
        assertEquals(TaskBucket.TODAY, taskBucket(task(1), midnight, zone))
        assertEquals(TaskBucket.OVERDUE, taskBucket(task(0), midnight, zone))
        val dst = ZoneId.of("America/New_York")
        val spring = LocalDate.of(2026, 3, 8)
        val start = spring.atStartOfDay(dst).toInstant().toEpochMilli()
        val next = Task(title = "DST", deadlineAt = taskDeadline(spring.plusDays(1), LocalTime.MIDNIGHT, dst))
        assertEquals(TaskBucket.TOMORROW, taskBucket(next, start, dst))
        assertEquals(LocalTime.of(23, 59), Instant.ofEpochMilli(taskDeadline(spring, null, dst)).atZone(dst).toLocalTime())
        assertEquals(LocalTime.of(9, 30), Instant.ofEpochMilli(taskDeadline(today, LocalTime.of(9, 30), zone)).atZone(zone).toLocalTime())
    }
}
