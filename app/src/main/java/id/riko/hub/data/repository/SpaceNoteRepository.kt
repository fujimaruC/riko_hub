package id.riko.hub.data.repository

import androidx.room.withTransaction
import id.riko.hub.core.time.Clock
import id.riko.hub.data.local.RikoDatabase
import id.riko.hub.data.local.entity.SpaceNoteEntity
import id.riko.hub.domain.model.SpaceNote
import id.riko.hub.domain.model.SpaceNoteKind
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID

class SpaceNoteRepository(private val database: RikoDatabase, private val clock: Clock) {
    private val dao get() = database.spaceNotes()

    fun observeJournals() = dao.observeJournals().map { rows -> rows.map { it.toDomain() } }
    fun observeJournalHistory() = observeJournals()
    fun observeIdeas() = dao.observeIdeas().map { rows -> rows.map { it.toDomain() } }

    suspend fun saveToday(date: LocalDate, body: String): SpaceNote = database.withTransaction {
        val clean = validateBody(body)
        val previous = dao.findJournalsForDate(date.toString()).firstOrNull()
        val now = clock.now()
        if (previous == null) {
            val row = SpaceNoteEntity(syncId = UUID.randomUUID().toString(), kind = "JOURNAL", body = clean,
                journalDate = date.toString(), projectTaskSyncId = null, createdAt = now, updatedAt = now, deletedAt = null)
            dao.insert(row)
            row.toDomain()
        } else {
            previous.copy(body = clean, updatedAt = maxOf(now, previous.createdAt, previous.updatedAt)).also {
                check(dao.update(it) == 1)
            }.toDomain()
        }
    }

    suspend fun saveIdea(projectTaskSyncId: String? = null, body: String): SpaceNote = database.withTransaction {
        val clean = validateBody(body)
        require(projectTaskSyncId == null || projectTaskSyncId.isNotBlank() && projectTaskSyncId.length <= 120) { "Invalid project task ID" }
        require(projectTaskSyncId == null || database.tasks().findSyncId(projectTaskSyncId) != null) { "Project task does not exist" }
        val now = clock.now()
        SpaceNoteEntity(syncId = UUID.randomUUID().toString(), kind = "IDEA", body = clean,
            journalDate = null, projectTaskSyncId = projectTaskSyncId, createdAt = now, updatedAt = now, deletedAt = null)
            .also { dao.insert(it) }.toDomain()
    }

    suspend fun update(syncId: String, body: String): SpaceNote = database.withTransaction {
        val previous = requireNotNull(dao.findBySyncId(syncId)?.takeIf { it.deletedAt == null }) { "Note does not exist" }
        val updated = previous.copy(body = validateBody(body), updatedAt = maxOf(clock.now(), previous.createdAt, previous.updatedAt))
        check(dao.update(updated) == 1)
        updated.toDomain()
    }

    suspend fun softDelete(syncId: String): Unit = database.withTransaction {
        val previous = requireNotNull(dao.findBySyncId(syncId)?.takeIf { it.deletedAt == null }) { "Note does not exist" }
        val now = maxOf(clock.now(), previous.createdAt, previous.updatedAt)
        check(dao.update(previous.copy(deletedAt = now, updatedAt = now)) == 1)
    }

    private fun validateBody(body: String): String = body.trim().also {
        require(it.isNotEmpty() && it.length <= MAX_BODY_LENGTH) { "Note must contain 1 to $MAX_BODY_LENGTH characters" }
    }

    private fun SpaceNoteEntity.toDomain() = SpaceNote(syncId, SpaceNoteKind.valueOf(kind), body,
        journalDate?.let(LocalDate::parse), projectTaskSyncId, createdAt, updatedAt, deletedAt)

    private companion object { const val MAX_BODY_LENGTH = 10_000 }
}
