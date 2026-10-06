package id.riko.hub.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "tasks", indices = [
    Index(value = ["deadlineAt"], name = "index_tasks_deadline"),
    Index(value = ["status", "deletedAt"], name = "index_tasks_status_deleted"),
    Index(value = ["syncId"], unique = true, name = "index_tasks_syncId"),
])
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val syncId: String,
    val title: String,
    val notes: String?,
    val category: String,
    val priority: String,
    val status: String,
    val deadlineAt: Long?,
    val hasDeadlineTime: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long?,
    val deletedAt: Long?,
)

@Entity(tableName = "events", indices = [
    Index(value = ["startAt"], name = "index_events_start"),
    Index(value = ["syncId"], unique = true, name = "index_events_syncId"),
])
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val syncId: String,
    val title: String,
    val notes: String?,
    val kind: String,
    val category: String,
    val startAt: Long,
    val endAt: Long?,
    val location: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)
