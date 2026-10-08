package id.riko.hub.data.mapper

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import id.riko.hub.data.local.entity.EventEntity
import id.riko.hub.data.local.entity.TaskEntity
import id.riko.hub.domain.model.*

private inline fun <reified T : Enum<T>> safeEnum(value: String, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == value } ?: fallback.also {
        Log.w("RikoMapper", "Unknown enum value; using default")
    }

private fun encodeChecklist(items: List<ChecklistItem>) = JSONArray().also { result ->
    items.forEach { result.put(JSONObject().put("id", it.id).put("title", it.title).put("done", it.done)) }
}.toString()

private fun decodeChecklist(json: String): List<ChecklistItem> {
    try {
        val array = JSONArray(json)
        require(array.length() <= 100) { "Checklist exceeds 100 items" }
        val ids = mutableSetOf<String>()
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            val id = item.opt("id")
            val title = item.opt("title")
            val done = item.opt("done")
            require(id is String && id.isNotBlank()) { "Checklist item ID is invalid" }
            require(ids.add(id)) { "Checklist item IDs are not unique" }
            require(title is String && title.trim().length in 1..120) { "Checklist item title is invalid" }
            require(done is Boolean) { "Checklist item state is invalid" }
            ChecklistItem(id, title, done)
        }
    } catch (error: Exception) {
        throw IllegalStateException("Stored checklist data is invalid", error)
    }
}

fun TaskEntity.toDomain(): Task {
    val taskStatus = safeEnum(status, TaskStatus.NOT_STARTED)
    return Task(
        id = id, syncId = syncId, title = title, notes = notes,
        category = safeEnum(category, Category.PERSONAL),
        priority = safeEnum(priority, Priority.MEDIUM), status = taskStatus,
        deadlineAt = deadlineAt, hasDeadlineTime = hasDeadlineTime,
        createdAt = createdAt, updatedAt = updatedAt,
        completedAt = if (taskStatus == TaskStatus.DONE) completedAt ?: updatedAt else null,
        deletedAt = deletedAt, estimatedMinutes = estimatedMinutes,
        recurrence = safeEnum(recurrence, Recurrence.NONE), checklist = decodeChecklist(checklistJson),
        reminderMinutesBefore = reminderMinutesBefore,
        generatedFromTaskId = generatedFromTaskId, generationPristine = generationPristine,
        recurrenceAnchorAt = recurrenceAnchorAt,
    )
}

fun Task.toEntity() = TaskEntity(
    id = id, syncId = syncId, title = title, notes = notes,
    category = category.name, priority = priority.name, status = status.name,
    deadlineAt = deadlineAt, hasDeadlineTime = hasDeadlineTime,
    createdAt = createdAt, updatedAt = updatedAt, completedAt = completedAt, deletedAt = deletedAt,
    estimatedMinutes = estimatedMinutes, recurrence = recurrence.name,
    checklistJson = encodeChecklist(checklist), reminderMinutesBefore = reminderMinutesBefore,
    generatedFromTaskId = generatedFromTaskId, generationPristine = generationPristine,
    recurrenceAnchorAt = recurrenceAnchorAt,
)

fun EventEntity.toDomain() = Event(
    id, syncId, title, notes, safeEnum(kind, EventKind.OTHER), safeEnum(category, Category.ACADEMIC),
    startAt, endAt, location, createdAt, updatedAt, deletedAt,
)

fun Event.toEntity() = EventEntity(
    id, syncId, title, notes, kind.name, category.name, startAt, endAt,
    location, createdAt, updatedAt, deletedAt,
)
