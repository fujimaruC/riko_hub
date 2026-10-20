package id.riko.hub.domain.logic

import id.riko.hub.core.time.Clock
import id.riko.hub.domain.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class HomeEngineTest {
    private val zone = ZoneId.of("Asia/Makassar")
    private val today = LocalDate.of(2026, 10, 6)
    private var now = today.atTime(14, 0).atZone(zone).toInstant().toEpochMilli()
    private val clock = object : Clock { override fun now() = now; override fun zone() = zone }
    private fun task(id: Long = 1, days: Long? = null, priority: Priority = Priority.LOW) = Task(
        id = id, title = "Task $id", priority = priority,
        deadlineAt = days?.let { taskDeadline(today.plusDays(it), null, zone) })
    private fun score(t: Task) = UrgencyCalculator.calculate(t, now, zone)
    private fun snapshot(tasks: List<Task>, settings: AppSettings = AppSettings(), events: List<Event> = emptyList()) =
        homeSnapshot(tasks, events, settings, clock)

    @Test fun all_six_worked_examples_match_scores_and_tiers() {
        val examples = listOf(
            Triple(task(days = 0, priority = Priority.HIGH).copy(title = "Persiapan UTS"), 70, BubbleTier.XL),
            Triple(task(days = 0, priority = Priority.MEDIUM).copy(title = "Tugas Individu"), 57, BubbleTier.L),
            Triple(task(days = 1, priority = Priority.MEDIUM).copy(title = "Evaluasi Proker"), 42, BubbleTier.M),
            Triple(task().copy(title = "Minum air"), 3, BubbleTier.S),
            Triple(task(days = -1, priority = Priority.MEDIUM).copy(status = TaskStatus.IN_PROGRESS), 80, BubbleTier.XL),
            Triple(task(days = 5), 8, BubbleTier.S))
        examples.forEach { (t, expected, tier) -> assertEquals(t.title, expected, score(t).score); assertEquals(tier, score(t).tier) }
    }

    @Test fun every_time_priority_status_and_tier_boundary() {
        listOf(-1L to 60, 0L to 45, 1L to 30, 2L to 18, 3L to 18, 4L to 8, 7L to 8, 8L to 2)
            .forEach { (days, points) -> assertEquals(points, score(task(days = days)).score) }
        listOf(0L, 1L, 10_800_000L).forEach { offset -> assertEquals(55, score(task().copy(deadlineAt = now + offset)).score) }
        assertEquals(45, score(task().copy(deadlineAt = now + 10_800_001)).score)
        assertEquals(60, score(task().copy(deadlineAt = now - 1)).score)
        assertTrue(score(task(days = -1)).isOverdue)
        assertFalse(score(task().copy(deadlineAt = now)).isOverdue)
        Priority.entries.zip(listOf(0, 12, 25)).forEach { (p, points) ->
            assertEquals(3 + points, score(task(priority = p)).score)
            assertEquals(11 + points, score(task(priority = p).copy(status = TaskStatus.IN_PROGRESS)).score)
        }
        assertEquals(BubbleTier.S, score(task(days = 4, priority = Priority.MEDIUM)).tier)
        assertEquals(BubbleTier.M, score(task(days = 2, priority = Priority.MEDIUM)).tier)
        assertEquals(BubbleTier.L, score(task(days = 0)).tier)
        assertEquals(BubbleTier.XL, score(task(days = 0, priority = Priority.HIGH)).tier)
        // Across midnight the next-three-hours rule takes precedence over "tomorrow".
        now = today.atTime(23, 0).atZone(zone).toInstant().toEpochMilli()
        assertEquals(55, score(task().copy(deadlineAt = now + 7_200_000)).score)
    }

    @Test fun ordering_is_score_deadline_priority_created_then_id_independent_of_input_order() {
        val items = listOf(task(9, 0), task(3).copy(createdAt = 5), task(2).copy(createdAt = 5),
            task(4).copy(createdAt = 2), task(7, 8), task(6, 9), task(5, 0, Priority.HIGH))
        val expected = listOf(5L, 9L, 4L, 2L, 3L, 7L, 6L)
        assertEquals(expected, UrgencyCalculator.rank(items.reversed(), clock).map { it.task.id })
        // Equal scores/deadlines with synthetic urgency isolates every tie-break key.
        val ranked = listOf(task(1, priority = Priority.HIGH), task(2, priority = Priority.LOW))
        assertEquals(1L, UrgencyCalculator.rank(ranked, clock).first().task.id)
        assertEquals(UrgencyCalculator.rank(items, clock), UrgencyCalculator.rank(items.reversed(), clock))
    }

    @Test fun pin_overrides_rank_expires_and_clears_when_completed_or_deleted() {
        val tasks = listOf(task(1, 0, Priority.HIGH), task(2))
        val pinned = AppSettings(pinnedFocusTaskId = 2, pinnedFocusDate = today)
        val result = snapshot(tasks, pinned)
        assertEquals(2L, result.focus)
        assertEquals(2L, result.bubbles.first().id)
        assertEquals(.40, result.bubbles.first().diameter, .00001)
        assertEquals(1L, snapshot(tasks, pinned.copy(pinnedFocusDate = today.minusDays(1))).focus)
        assertNull(snapshot(listOf(task(2).copy(status = TaskStatus.DONE, completedAt = now)), pinned).focus)
        assertNull(snapshot(listOf(task(2).copy(deletedAt = now)), pinned).validPin)
        assertEquals(1L, snapshot(listOf(tasks[0], tasks[1].copy(status = TaskStatus.DONE, completedAt = now)), pinned).focus)
    }

    @Test fun counts_zero_one_seven_eight_twenty_and_overflow() {
        for (count in listOf(0, 1, 7, 8, 20)) {
            val result = snapshot((1..count).map { task(it.toLong()) })
            assertEquals(minOf(count, 7), result.bubbles.size)
            assertEquals(if (count > 7) count - 6 else 0, result.overflow)
            assertEquals(if (count == 0) null else 1L, result.focus)
            if (count > 7) { assertNull(result.bubbles.last().urgency); assertEquals(.18, result.bubbles.last().diameter, .00001) }
        }
    }

    @Test fun modes_at_zero_one_two_three_six_urgent_tasks() {
        listOf(0, 1, 2, 3, 6).zip(listOf(HomeMode.CALM, HomeMode.NORMAL, HomeMode.NORMAL, HomeMode.DENSE, HomeMode.DENSE))
            .forEach { (count, mode) ->
                val result = snapshot((1..count).map { task(it.toLong(), 0, Priority.HIGH) })
                assertEquals(mode, result.mode)
                result.bubbles.firstOrNull()?.let { assertEquals(.54 * mode.scale, it.diameter, .00001) }
            }
    }

    @Test fun maximum_template_and_every_mode_are_in_bounds_and_overlap_at_most_twelve_percent() {
        val maximum = BubbleLayoutPlanner.slots.map { BubblePlacement(null, it.x, it.y, it.maxDiameter) }
        fun verify(items: List<BubblePlacement>) {
            items.forEachIndexed { i, a ->
                assertTrue("bounds $i", BubbleLayoutPlanner.withinBounds(a))
                items.take(i).forEach { b -> assertTrue("overlap $i with $b",
                    BubbleLayoutPlanner.overlap(a, b) <= .12 * minOf(a.diameter, b.diameter) + 1e-9) }
            }
        }
        verify(maximum)
        val ranked = UrgencyCalculator.rank((1..20).map { task(it.toLong(), 0, Priority.HIGH) }, clock)
        HomeMode.entries.forEach { mode ->
            val a = BubbleLayoutPlanner.plan(ranked, 3L, mode)
            assertEquals(a, BubbleLayoutPlanner.plan(ranked, 3L, mode)); verify(a)
        }
    }

    @Test fun midnight_rollover_updates_pin_done_row_progress_and_tick_delay() {
        val tasks = listOf(task(1, 1), task(2).copy(status = TaskStatus.DONE, completedAt = now))
        val pin = AppSettings(pinnedFocusTaskId = 1, pinnedFocusDate = today)
        now = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        assertEquals(1, nextHomeTickDelay(clock))
        assertEquals(1, snapshot(tasks, pin).doneToday.size)
        now++
        val next = snapshot(tasks, pin)
        assertNull(next.validPin); assertTrue(next.doneToday.isEmpty())
        assertEquals(45, next.ranked.single().score)
        assertEquals(60_000, nextHomeTickDelay(clock))
    }

    @Test fun progress_is_a_union_and_next_obligation_ignores_done_deleted_and_date_only() {
        val tasks = listOf(task(1, 0).copy(status = TaskStatus.DONE, completedAt = now),
            task(2).copy(status = TaskStatus.IN_PROGRESS), task(3, 0),
            task(4).copy(deadlineAt = now + 3_600_000, hasDeadlineTime = true),
            task(5, 0).copy(deletedAt = now))
        val event = Event(id = 1, title = "Kelas", startAt = now + 1_800_000, location = "Lab Komputer")
        val result = snapshot(tasks, events = listOf(event))
        assertEquals(4, result.total); assertEquals(1, result.done)
        assertEquals("Kelas", result.next?.title)
        assertEquals("Lab Komputer", result.next?.location)
        assertEquals("Task 4", snapshot(tasks).next?.title)
        assertNull(snapshot(tasks).next?.location)
        assertNull(snapshot(listOf(task(1, 0))).next)
        assertNull(snapshot(emptyList(), events = listOf(event.copy(startAt = now - 1))).next)
        assertNull(snapshot(emptyList(), events = listOf(event.copy(startAt = now + 86_400_001))).next)
    }
    @Test fun equal_score_ties_cover_every_comparator_key() {
        val a = score(task(1)).copy(score = 40)
        val inputs = listOf(a.copy(task = task(5).copy(createdAt = 1)),
            a.copy(task = task(4).copy(createdAt = 1)), a.copy(task = task(3, priority = Priority.HIGH)),
            a.copy(task = task(2, 1)), a.copy(task = task(1, 0)))
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), inputs.sortedWith(UrgencyCalculator.order).map { it.task.id })
    }

    @Test fun large_text_keeps_three_tasks_plus_overflow_and_focus_first() {
        for (count in listOf(0, 1, 3, 4, 7, 20)) {
            val ranked = UrgencyCalculator.rank((1..count).map { task(it.toLong()) }, clock)
            val result = BubbleLayoutPlanner.largeTextPlan(ranked, count.toLong())
            assertEquals(minOf(count, 3), result.count { it.urgency != null })
            assertEquals((count - 3).coerceAtLeast(0), result.sumOf { it.overflow })
            result.firstOrNull()?.let { assertEquals(count.toLong(), it.id) }
            assertTrue(result.all { it.diameter * 280 >= 72 })
        }
    }

    @Test fun drift_extremes_preserve_bounds_and_overlap() {
        val bubbles = snapshot((1..7).map { task(it.toLong(), 0, Priority.HIGH) }).bubbles
        bubbles.forEachIndexed { i, a ->
            val driftA = minOf(3.0 / 320, BubbleLayoutPlanner.driftAllowance(a, bubbles))
            for (dx in listOf(-driftA, driftA)) for (dy in listOf(-driftA, driftA)) {
                val moved = a.copy(x = a.x + dx, y = a.y + dy / .95)
                assertTrue(BubbleLayoutPlanner.withinBounds(moved))
                bubbles.take(i).forEach { b ->
                    val driftB = minOf(3.0 / 320, BubbleLayoutPlanner.driftAllowance(b, bubbles))
                    for (bx in listOf(-driftB, driftB)) for (by in listOf(-driftB, driftB)) {
                        val other = b.copy(x = b.x + bx, y = b.y + by / .95)
                        assertTrue(BubbleLayoutPlanner.overlap(moved, other) <= .12 * minOf(a.diameter,b.diameter) + 1e-9)
                    }
                }
            }
        }
    }

    @Test fun timezone_and_dst_ticks_use_injected_zone() {
        val dst = ZoneId.of("America/New_York")
        now = LocalDate.of(2026,3,8).atStartOfDay(dst).toInstant().toEpochMilli()
        val dstClock = object : Clock { override fun now() = now; override fun zone() = dst }
        assertEquals(60_000L, nextHomeTickDelay(dstClock))
        val midnight = LocalDate.of(2026,3,9).atStartOfDay(dst).toInstant().toEpochMilli()
        now = midnight - 500
        assertEquals(500L, nextHomeTickDelay(dstClock))
        assertEquals(LocalDate.of(2026,3,8), homeSnapshot(emptyList(), emptyList(), AppSettings(), dstClock).today)
        now = midnight
        assertEquals(LocalDate.of(2026,3,9), homeSnapshot(emptyList(), emptyList(), AppSettings(), dstClock).today)
    }
    @Test fun tier_diameters_are_capped_by_slot_and_scaled_after_focus_promotion() {
        val ranked = BubbleTier.entries.mapIndexed { index, tier -> score(task(index.toLong()+1)).copy(tier = tier) }
        HomeMode.entries.forEach { mode ->
            val result = BubbleLayoutPlanner.plan(ranked, 1L, mode)
            assertEquals(listOf(.54, .40, .30, .20).map { it * mode.scale }, result.map { it.diameter })
        }
        val low = listOf(score(task()))
        assertEquals(.40, BubbleLayoutPlanner.plan(low, 1L, HomeMode.CALM).single().diameter, 1e-9)
        val done = task().copy(status = TaskStatus.DONE, completedAt = now)
        assertTrue(UrgencyCalculator.rank(listOf(done, task(2).copy(deletedAt = now)), clock).isEmpty())
        assertEquals(HomeMode.CALM, snapshot(listOf(done)).mode)
    }
}
