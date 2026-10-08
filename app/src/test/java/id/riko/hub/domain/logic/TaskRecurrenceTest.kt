package id.riko.hub.domain.logic

import id.riko.hub.domain.model.Recurrence
import id.riko.hub.domain.model.Task
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class TaskRecurrenceTest {
    @Test fun daily_and_weekly_skip_missed_dates() {
        val zone = ZoneId.of("Asia/Makassar")
        val now = LocalDate.of(2026, 10, 20).atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()
        val daily = task(Recurrence.DAILY, LocalDate.of(2026, 10, 7), zone)
        val weekly = task(Recurrence.WEEKLY, LocalDate.of(2026, 10, 6), zone)
        assertEquals(LocalDate.of(2026, 10, 21), date(nextTaskDeadline(daily, now, zone), zone))
        assertEquals(LocalDate.of(2026, 10, 27), date(nextTaskDeadline(weekly, now, zone), zone))
    }

    @Test fun monthly_keeps_original_day_when_skipping_missed_months() {
        val zone = ZoneId.of("UTC")
        val now = Instant.parse("2026-03-15T12:00:00Z").toEpochMilli()
        val monthly = task(Recurrence.MONTHLY, LocalDate.of(2026, 1, 31), zone)
        assertEquals(LocalDate.of(2026, 3, 31), date(nextTaskDeadline(monthly, now, zone), zone))
    }

    @Test fun monthly_due_january_31_completes_early_in_february_on_february_28() {
        val zone = ZoneId.of("UTC")
        val now = Instant.parse("2026-02-01T12:00:00Z").toEpochMilli()
        val monthly = task(Recurrence.MONTHLY, LocalDate.of(2026, 1, 31), zone)
        assertEquals(LocalDate.of(2026, 2, 28), date(nextTaskDeadline(monthly, now, zone), zone))
    }

    @Test fun monthly_chain_keeps_january_31_anchor_after_february_clamps() {
        val zone = ZoneId.of("UTC")
        val anchor = taskDeadline(LocalDate.of(2026, 1, 31), null, zone)
        val monthly = Task(title = "Monthly", deadlineAt = anchor, recurrence = Recurrence.MONTHLY, recurrenceAnchorAt = anchor)
        val february = requireNotNull(nextTaskDeadline(monthly, anchor, zone))
        assertEquals(LocalDate.of(2026, 2, 28), date(february, zone))
        val next = requireNotNull(nextTaskDeadline(monthly.copy(deadlineAt = february), february, zone))
        assertEquals(LocalDate.of(2026, 3, 31), date(next, zone))
    }

    @Test fun overdue_daily_task_completes_before_todays_due_time_and_keeps_today_occurrence() {
        val zone = ZoneId.of("UTC")
        val base = LocalDate.of(2026, 10, 19).atTime(17, 0).atZone(zone).toInstant().toEpochMilli()
        val now = Instant.parse("2026-10-20T10:00:00Z").toEpochMilli()
        val daily = Task(title = "Daily", deadlineAt = base, hasDeadlineTime = true, recurrence = Recurrence.DAILY)
        val next = Instant.ofEpochMilli(requireNotNull(nextTaskDeadline(daily, now, zone))).atZone(zone)
        assertEquals(LocalDate.of(2026, 10, 20), next.toLocalDate())
        assertEquals(LocalTime.of(17, 0), next.toLocalTime())
    }

    @Test fun timed_daily_deadline_uses_local_calendar_across_dst_gap() {
        val zone = ZoneId.of("America/New_York")
        val base = LocalDate.of(2026, 3, 7).atTime(2, 30).atZone(zone).toInstant().toEpochMilli()
        val now = LocalDate.of(2026, 3, 7).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val next = nextTaskDeadline(Task(title = "DST", deadlineAt = base, hasDeadlineTime = true, recurrence = Recurrence.DAILY), now, zone)
        val value = Instant.ofEpochMilli(requireNotNull(next)).atZone(zone)
        assertEquals(LocalDate.of(2026, 3, 8), value.toLocalDate())
        assertEquals(LocalTime.of(3, 30), value.toLocalTime())
    }

    @Test fun daily_chain_restores_anchor_time_after_dst_gap() {
        val zone = ZoneId.of("America/New_York")
        val anchor = LocalDate.of(2026, 3, 7).atTime(2, 30).atZone(zone).toInstant().toEpochMilli()
        val daily = Task(title = "DST", deadlineAt = anchor, hasDeadlineTime = true,
            recurrence = Recurrence.DAILY, recurrenceAnchorAt = anchor)
        val gapDay = requireNotNull(nextTaskDeadline(daily, anchor, zone))
        assertEquals(LocalTime.of(3, 30), Instant.ofEpochMilli(gapDay).atZone(zone).toLocalTime())
        val next = requireNotNull(nextTaskDeadline(daily.copy(deadlineAt = gapDay), gapDay, zone))
        val value = Instant.ofEpochMilli(next).atZone(zone)
        assertEquals(LocalDate.of(2026, 3, 9), value.toLocalDate())
        assertEquals(LocalTime.of(2, 30), value.toLocalTime())
    }

    private fun task(recurrence: Recurrence, date: LocalDate, zone: ZoneId) =
        Task(title = "Repeat", deadlineAt = taskDeadline(date, null, zone), recurrence = recurrence)

    private fun date(value: Long?, zone: ZoneId) = Instant.ofEpochMilli(requireNotNull(value)).atZone(zone).toLocalDate()
}
