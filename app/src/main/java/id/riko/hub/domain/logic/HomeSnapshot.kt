package id.riko.hub.domain.logic

import id.riko.hub.core.time.Clock
import id.riko.hub.domain.model.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class NextObligation(val title: String, val at: Long, val event: Boolean, val location: String? = null)
data class HomeSnapshot(
    val now: Long, val zone: ZoneId, val today: LocalDate, val ranked: List<UrgencyInfo>,
    val focus: Long?, val validPin: Long?, val mode: HomeMode, val bubbles: List<BubblePlacement>,
    val doneToday: List<Task>, val done: Int, val total: Int, val next: NextObligation?,
) {
    val urgentCount get() = ranked.count { it.isUrgent }
    val overflow get() = bubbles.sumOf { it.overflow }
}

fun homeSnapshot(tasks: List<Task>, events: List<Event>, settings: AppSettings, clock: Clock): HomeSnapshot {
    val now = clock.now()
    val zone = clock.zone()
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val fixed = object : Clock { override fun now() = now; override fun zone() = zone }
    fun isToday(value: Long?) = value != null && Instant.ofEpochMilli(value).atZone(zone).toLocalDate() == today
    val visible = tasks.filter { it.deletedAt == null }
    val ranked = UrgencyCalculator.rank(visible, fixed)
    val doneToday = visible.filter { it.status == TaskStatus.DONE && isToday(it.completedAt) }
        .sortedWith(compareBy<Task> { it.completedAt }.thenBy { it.id })
    // A union, not a sum: a task due and completed today counts once.
    val progress = visible.filter { isToday(it.deadlineAt) || (it.status == TaskStatus.DONE && isToday(it.completedAt)) ||
        (it.status == TaskStatus.IN_PROGRESS && it.deadlineAt == null) }
    val pin = FocusSelector.validPin(ranked, settings.pinnedFocusTaskId, settings.pinnedFocusDate, today)
    val focus = FocusSelector.select(ranked, pin, today, today)
    val mode = ModeSelector.select(ranked.count { it.isUrgent })
    val end = now + 24 * 60 * 60 * 1000L
    val next = (events.filter { it.deletedAt == null && it.startAt in now..end }
        .map { NextObligation(it.title, it.startAt, true, it.location) } + ranked.mapNotNull { item ->
        item.task.deadlineAt?.takeIf { item.task.hasDeadlineTime && it in now..end }
            ?.let { NextObligation(item.task.title, it, false) }
    }).sortedWith(compareBy<NextObligation> { it.at }.thenBy { it.title }.thenBy { it.event }).firstOrNull()
    return HomeSnapshot(now, zone, today, ranked, focus, pin, mode, BubbleLayoutPlanner.plan(ranked, focus, mode),
        doneToday, progress.count { it.status == TaskStatus.DONE }, progress.size, next)
}

fun nextHomeTickDelay(clock: Clock): Long {
    val now = clock.now()
    val midnight = Instant.ofEpochMilli(now).atZone(clock.zone()).toLocalDate().plusDays(1)
        .atStartOfDay(clock.zone()).toInstant().toEpochMilli()
    return minOf(60_000L, midnight - now).coerceAtLeast(1)
}
