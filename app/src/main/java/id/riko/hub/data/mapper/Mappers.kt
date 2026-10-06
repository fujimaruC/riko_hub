package id.riko.hub.data.mapper

import android.util.Log
import id.riko.hub.data.local.entity.EventEntity
import id.riko.hub.data.local.entity.TaskEntity
import id.riko.hub.domain.model.*

private inline fun <reified T : Enum<T>> safeEnum(value: String, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == value } ?: fallback.also {
        Log.w("RikoMapper", "Unknown enum value; using default")
    }

fun TaskEntity.toDomain(): Task {
    val taskStatus = safeEnum(status, TaskStatus.NOT_STARTED)
    return Task(
        id, syncId, title, notes, safeEnum(category, Category.PERSONAL),
        safeEnum(priority, Priority.MEDIUM), taskStatus, deadlineAt, hasDeadlineTime,
        createdAt, updatedAt,
        if (taskStatus == TaskStatus.DONE) completedAt ?: updatedAt else null, deletedAt,
    )
}

fun Task.toEntity() = TaskEntity(
    id, syncId, title, notes, category.name, priority.name, status.name,
    deadlineAt, hasDeadlineTime, createdAt, updatedAt, completedAt, deletedAt,
)

fun EventEntity.toDomain() = Event(
    id, syncId, title, notes, safeEnum(kind, EventKind.OTHER), safeEnum(category, Category.ACADEMIC),
    startAt, endAt, location, createdAt, updatedAt, deletedAt,
)

fun Event.toEntity() = EventEntity(
    id, syncId, title, notes, kind.name, category.name, startAt, endAt,
    location, createdAt, updatedAt, deletedAt,
)
