package id.riko.hub.domain.model

import java.time.LocalDate

enum class SpaceNoteKind { JOURNAL, IDEA }

data class SpaceNote(
    val syncId: String,
    val kind: SpaceNoteKind,
    val body: String,
    val journalDate: LocalDate?,
    val projectTaskSyncId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)
