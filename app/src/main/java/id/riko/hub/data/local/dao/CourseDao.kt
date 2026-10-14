package id.riko.hub.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import id.riko.hub.data.local.entity.CourseEntity
import id.riko.hub.data.local.entity.CourseWeeklySlotEntity
import id.riko.hub.data.local.entity.CourseWithWeeklySlots
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Transaction
    @Query("SELECT * FROM courses ORDER BY archivedAt IS NOT NULL, sortOrder, id")
    fun observeAll(): Flow<List<CourseWithWeeklySlots>>

    @Transaction
    @Query("SELECT * FROM courses WHERE id = :id")
    fun observeById(id: Long): Flow<CourseWithWeeklySlots?>

    @Transaction
    @Query("SELECT * FROM courses WHERE id = :id")
    suspend fun adminFindForWrite(id: Long): CourseWithWeeklySlots?

    @Transaction
    @Query("SELECT * FROM courses ORDER BY id")
    suspend fun backupSnapshot(): List<CourseWithWeeklySlots>

    @Query("SELECT * FROM course_weekly_slots ORDER BY courseId, dayOfWeek, startMinute, id")
    suspend fun weeklySlotSnapshot(): List<CourseWeeklySlotEntity>

    @Query("SELECT MAX(sortOrder) FROM courses")
    suspend fun maxSortOrder(): Long?

    @Query("SELECT * FROM courses ORDER BY archivedAt IS NOT NULL, sortOrder, id")
    suspend fun findAllForOrder(): List<CourseEntity>

    @Query("SELECT * FROM courses WHERE (archivedAt IS NULL) = (:active) ORDER BY sortOrder, id")
    suspend fun findOrderedGroup(active: Boolean): List<CourseEntity>

    @Insert
    suspend fun insert(course: CourseEntity): Long

    @Update
    suspend fun update(course: CourseEntity): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSlots(slots: List<CourseWeeklySlotEntity>)

    @Query("DELETE FROM course_weekly_slots WHERE courseId = :courseId")
    suspend fun deleteSlots(courseId: Long)
}
