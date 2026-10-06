package id.riko.hub.domain.logic

import id.riko.hub.core.time.Clock
import id.riko.hub.domain.model.*
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class BubbleTier(val diameter: Double) { XL(.54), L(.40), M(.30), S(.20) }
data class UrgencyInfo(val task: Task, val score: Int, val tier: BubbleTier, val isOverdue: Boolean) {
    val isUrgent get() = score >= 45
}

object UrgencyCalculator {
    fun calculate(task: Task, now: Long, zone: ZoneId): UrgencyInfo {
        require(task.status != TaskStatus.DONE)
        val deadline = task.deadlineAt
        val days = deadline?.let { ChronoUnit.DAYS.between(
            Instant.ofEpochMilli(now).atZone(zone).toLocalDate(),
            Instant.ofEpochMilli(it).atZone(zone).toLocalDate()) }
        val time = when {
            deadline == null -> 3
            deadline < now -> 60
            deadline - now <= 3 * 60 * 60 * 1000L -> 55
            days == 0L -> 45
            days == 1L -> 30
            days!! <= 3 -> 18
            days <= 7 -> 8
            else -> 2
        }
        val score = time + when (task.priority) { Priority.HIGH -> 25; Priority.MEDIUM -> 12; Priority.LOW -> 0 } +
            if (task.status == TaskStatus.IN_PROGRESS) 8 else 0
        val tier = when { score >= 70 -> BubbleTier.XL; score >= 45 -> BubbleTier.L; score >= 25 -> BubbleTier.M; else -> BubbleTier.S }
        return UrgencyInfo(task, score, tier, deadline != null && deadline < now)
    }

    fun rank(tasks: List<Task>, clock: Clock): List<UrgencyInfo> {
        val now = clock.now()
        val zone = clock.zone()
        return tasks.filter { it.deletedAt == null && it.status != TaskStatus.DONE }
            .map { calculate(it, now, zone) }.sortedWith(order)
    }

    val order = compareByDescending<UrgencyInfo> { it.score }
        .thenBy { it.task.deadlineAt ?: Long.MAX_VALUE }
        .thenByDescending { it.task.priority.ordinal }
        .thenBy { it.task.createdAt }.thenBy { it.task.id }
}
