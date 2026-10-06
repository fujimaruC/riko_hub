package id.riko.hub.domain.logic

import java.time.LocalDate

object FocusSelector {
    fun validPin(ranked: List<UrgencyInfo>, id: Long?, date: LocalDate?, today: LocalDate): Long? =
        id?.takeIf { date == today && ranked.any { item -> item.task.id == id } }

    fun select(ranked: List<UrgencyInfo>, id: Long?, date: LocalDate?, today: LocalDate): Long? =
        validPin(ranked, id, date, today) ?: ranked.firstOrNull()?.task?.id
}

enum class HomeMode(val scale: Double) { CALM(1.0), NORMAL(1.0), DENSE(.9) }
object ModeSelector {
    fun select(urgentCount: Int) = when { urgentCount == 0 -> HomeMode.CALM; urgentCount < 3 -> HomeMode.NORMAL; else -> HomeMode.DENSE }
}
