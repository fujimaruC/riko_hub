package id.riko.hub.domain.logic

import id.riko.hub.core.time.Clock
import id.riko.hub.domain.model.Priority
import id.riko.hub.domain.model.Task
import id.riko.hub.domain.model.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class TaskWidgetSelectionTest {
    private class FixedClock(private val time: Long, private val timeZone: ZoneId) : Clock {
        override fun now() = time
        override fun zone() = timeZone
    }

    private val now = 1_800_000_000_000L
    private val zone = ZoneId.of("Asia/Makassar")
    private val clock get() = FixedClock(now, zone)
    private fun task(id: Long, deadline: Long? = null, status: TaskStatus = TaskStatus.NOT_STARTED) =
        Task(id = id, title = "Task $id", deadlineAt = deadline, status = status)

    @Test fun selectionKeepsUrgencyOrderAndCapsAtThree() {
        for (count in 0..3) assertEquals(count, selectWidgetTasks((1L..count.toLong()).map { task(it) }, clock).size)
        val tasks = (1L..5).map { task(it, now + it * 60_000) }
        val selected = selectWidgetTasks(tasks, clock)
        assertEquals(3, selected.size)
        assertEquals(UrgencyCalculator.rank(tasks, clock).take(3), selected)

        val mixed = listOf(
            task(1).copy(priority = Priority.HIGH),
            task(2, now + 60_000).copy(priority = Priority.LOW),
            task(3, now + 24 * 60 * 60 * 1000L),
            task(4).copy(status = TaskStatus.IN_PROGRESS),
            task(5, now - 60_000).copy(priority = Priority.LOW),
        )
        assertEquals(listOf(5L, 2L, 3L), selectWidgetTasks(mixed, clock).map { it.task.id })
    }

    @Test fun selectionExcludesDoneAndDeletedAndPreservesExistingTieOrder() {
        val tied = listOf(task(4), task(2), task(3).copy(deletedAt = now), task(1, status = TaskStatus.DONE))
        assertEquals(listOf(2L, 4L), selectWidgetTasks(tied, clock).map { it.task.id })
    }

    @Test fun refreshIncludesWitaMidnightAndDstLocalMidnight() {
        val day = LocalDate.of(2026, 10, 10)
        val witaNow = day.atTime(23, 30).atZone(zone).toInstant().toEpochMilli()
        val witaMidnight = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        assertEquals(witaMidnight, nextWidgetRefreshAt(listOf(task(1)), FixedClock(witaNow, zone)))

        val newYork = ZoneId.of("America/New_York")
        val beforeDst = LocalDate.of(2026, 3, 8).atTime(0, 30).atZone(newYork).toInstant().toEpochMilli()
        val dstMidnight = LocalDate.of(2026, 3, 8).plusDays(1).atStartOfDay(newYork).toInstant().toEpochMilli()
        assertEquals(dstMidnight, nextWidgetRefreshAt(listOf(task(1)), FixedClock(beforeDst, newYork)))
    }

    @Test fun refreshesAtThreeHourAndStrictOverdueBoundaries() {
        assertEquals(now + 1_000, nextWidgetRefreshAt(listOf(task(1, now + 3 * 60 * 60 * 1000L + 1_000)), clock))
        assertEquals(now + 1, nextWidgetRefreshAt(listOf(task(1, now)), clock))
        assertEquals(nextMidnight(), nextWidgetRefreshAt(listOf(task(1, now - 1)), clock))
    }

    @Test fun irrelevantTasksDoNotScheduleRefreshAndLongOverflowIsSafe() {
        assertNull(nextWidgetRefreshAt(listOf(task(1, now + 60_000, TaskStatus.DONE), task(2).copy(deletedAt = now)), clock))
        assertEquals(nextMidnight(), nextWidgetRefreshAt(listOf(task(1, Long.MAX_VALUE)), clock))
    }

    private fun nextMidnight() = java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        .plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
}
