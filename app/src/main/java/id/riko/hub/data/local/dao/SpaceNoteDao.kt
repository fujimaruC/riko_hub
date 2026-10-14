package id.riko.hub.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import id.riko.hub.data.local.entity.SpaceNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SpaceNoteDao {
    @Query("SELECT * FROM space_notes WHERE kind = 'JOURNAL' AND deletedAt IS NULL ORDER BY journalDate DESC, updatedAt DESC, id DESC")
    fun observeJournals(): Flow<List<SpaceNoteEntity>>

    @Query("SELECT * FROM space_notes WHERE kind = 'IDEA' AND deletedAt IS NULL ORDER BY updatedAt DESC, id DESC")
    fun observeIdeas(): Flow<List<SpaceNoteEntity>>

    @Query("SELECT * FROM space_notes WHERE kind = 'JOURNAL' AND journalDate = :date AND deletedAt IS NULL ORDER BY updatedAt DESC, id DESC")
    suspend fun findJournalsForDate(date: String): List<SpaceNoteEntity>

    @Query("SELECT * FROM space_notes WHERE syncId = :syncId")
    suspend fun findBySyncId(syncId: String): SpaceNoteEntity?

    @Query("SELECT * FROM space_notes ORDER BY id")
    suspend fun backupSnapshot(): List<SpaceNoteEntity>

    @Insert suspend fun insert(note: SpaceNoteEntity): Long
    @Update suspend fun update(note: SpaceNoteEntity): Int
}
