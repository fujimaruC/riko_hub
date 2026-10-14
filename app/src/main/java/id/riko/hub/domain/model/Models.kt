package id.riko.hub.domain.model

import java.time.LocalDate
import java.util.UUID

enum class Category { ACADEMIC, ORGANIZATION, PERSONAL, PROJECTS }
enum class Priority { LOW, MEDIUM, HIGH }
enum class TaskStatus { NOT_STARTED, IN_PROGRESS, DONE }
enum class EventKind { CLASS, EXAM, MEETING, ACTIVITY, NOTE, OTHER }
enum class Recurrence { NONE, DAILY, WEEKLY, MONTHLY }

data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val done: Boolean = false,
)

typealias ThemeMode = id.riko.hub.ui.theme.ThemeMode

data class Task(
    val id: Long = 0,
    val syncId: String = UUID.randomUUID().toString(),
    val title: String,
    val notes: String? = null,
    val category: Category = Category.PERSONAL,
    val priority: Priority = Priority.MEDIUM,
    val status: TaskStatus = TaskStatus.NOT_STARTED,
    val deadlineAt: Long? = null,
    val hasDeadlineTime: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val completedAt: Long? = null,
    val deletedAt: Long? = null,
    val estimatedMinutes: Int? = null,
    val recurrence: Recurrence = Recurrence.NONE,
    val checklist: List<ChecklistItem> = emptyList(),
    val reminderMinutesBefore: Int? = null,
    val generatedFromTaskId: Long? = null,
    val generationPristine: Boolean = false,
    val recurrenceAnchorAt: Long? = null,
)

data class Event(
    val id: Long = 0,
    val syncId: String = UUID.randomUUID().toString(),
    val title: String,
    val notes: String? = null,
    val kind: EventKind = EventKind.OTHER,
    val category: Category = Category.ACADEMIC,
    val startAt: Long,
    val endAt: Long? = null,
    val location: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val deletedAt: Long? = null,
)

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val displayName: String = "Riko",
    val hintsShownCount: Int = 0,
    val pinnedFocusTaskId: Long? = null,
    val pinnedFocusDate: LocalDate? = null,
)
