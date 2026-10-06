package id.riko.hub.domain.model

import java.time.LocalDate
import java.util.UUID

enum class Category { ACADEMIC, ORGANIZATION, PERSONAL, PROJECTS }
enum class Priority { LOW, MEDIUM, HIGH }
enum class TaskStatus { NOT_STARTED, IN_PROGRESS, DONE }
enum class EventKind { CLASS, EXAM, MEETING, ACTIVITY, NOTE, OTHER }
typealias ThemeMode = id.riko.hub.ui.theme.ThemeMode
enum class NavigationStyle { SPATIAL_COMPASS, COMPASS_ONLY }

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
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val displayName: String = "Riko",
    val navStyle: NavigationStyle = NavigationStyle.SPATIAL_COMPASS,
    val hintsShownCount: Int = 0,
    val pinnedFocusTaskId: Long? = null,
    val pinnedFocusDate: LocalDate? = null,
)
