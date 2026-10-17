package id.riko.hub.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "space_notes", indices = [
    Index(value = ["syncId"], unique = true),
    Index(value = ["kind", "journalDate", "deletedAt"]),
    Index(value = ["kind", "deletedAt"]),
    Index(value = ["projectTaskSyncId"]),
])
data class SpaceNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val syncId: String,
    val kind: String,
    val body: String,
    val journalDate: String?,
    val projectTaskSyncId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)
