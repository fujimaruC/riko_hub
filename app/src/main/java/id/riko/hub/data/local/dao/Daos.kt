package id.riko.hub.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import id.riko.hub.data.local.entity.EventEntity
import id.riko.hub.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE deletedAt IS NULL AND status != 'DONE' ORDER BY deadlineAt IS NULL, deadlineAt, id")
    fun observeActive(): Flow<List<TaskEntity>>
    @Query("SELECT * FROM tasks WHERE deletedAt IS NULL ORDER BY id")
    fun observeAll(): Flow<List<TaskEntity>>
    @Query("SELECT * FROM tasks WHERE id = :id AND deletedAt IS NULL")
    fun observeById(id: Long): Flow<TaskEntity?>
    @Query("SELECT * FROM tasks WHERE deletedAt IS NULL AND deadlineAt >= :start AND deadlineAt < :end ORDER BY deadlineAt, id")
    fun observeDeadlinesBetween(start: Long, end: Long): Flow<List<TaskEntity>>
    @Query("SELECT * FROM tasks WHERE deletedAt IS NULL AND status = 'DONE' AND completedAt >= :start AND completedAt < :end ORDER BY completedAt, id")
    fun observeCompletedBetween(start: Long, end: Long): Flow<List<TaskEntity>>
    // Internal transaction reads include deleted rows for restore; never expose them to UI.
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun adminFindForWrite(id: Long): TaskEntity?
    @Query("SELECT * FROM tasks ORDER BY id")
    suspend fun adminFindAllForReminders(): List<TaskEntity>
    @Query("SELECT * FROM tasks WHERE generatedFromTaskId = :parentId")
    suspend fun findGeneratedSuccessor(parentId: Long): TaskEntity?
    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun adminCount(): Int
    @Insert suspend fun insert(task: TaskEntity): Long
    @Update suspend fun update(task: TaskEntity): Int
    @Query("UPDATE tasks SET deletedAt = :deletedAt, updatedAt = :updatedAt, generationPristine = 0 WHERE id = :id")
    suspend fun setDeletedAt(id: Long, deletedAt: Long?, updatedAt: Long)
    @Query("UPDATE tasks SET deletedAt = :updatedAt, updatedAt = :updatedAt WHERE id = :id AND deletedAt IS NULL AND generationPristine = 1")
    suspend fun deleteGeneratedForUndo(id: Long, updatedAt: Long): Int
    @Query("UPDATE tasks SET deletedAt = NULL, updatedAt = :updatedAt WHERE id = :id AND deletedAt IS NOT NULL AND generationPristine = 1")
    suspend fun restoreGeneratedForRedo(id: Long, updatedAt: Long): Int
}

@Dao
interface EventDao {
    // Half-open range includes spanning and point events, including an event ending at start.
    @Query("SELECT * FROM events WHERE deletedAt IS NULL AND startAt < :end AND COALESCE(endAt, startAt) >= :start ORDER BY startAt, id")
    fun observeBetween(start: Long, end: Long): Flow<List<EventEntity>>
    @Query("SELECT * FROM events WHERE id = :id AND deletedAt IS NULL")
    fun observeById(id: Long): Flow<EventEntity?>
    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun adminFindForWrite(id: Long): EventEntity?
    @Query("SELECT COUNT(*) FROM events")
    suspend fun adminCount(): Int
    @Insert suspend fun insert(event: EventEntity): Long
    @Update suspend fun update(event: EventEntity): Int
    @Query("UPDATE events SET deletedAt = :deletedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setDeletedAt(id: Long, deletedAt: Long?, updatedAt: Long)
}
