package id.riko.hub.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import id.riko.hub.data.local.entity.AcademicAssignmentEntity
import id.riko.hub.data.local.entity.AcademicDeadlineChangeEntity
import id.riko.hub.data.local.entity.AssignmentWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface AssignmentDao {
    @Transaction
    @Query("SELECT academic_assignments.* FROM academic_assignments JOIN tasks ON tasks.id = academic_assignments.taskId WHERE tasks.deletedAt IS NULL ORDER BY academic_assignments.id")
    fun observeAll(): Flow<List<AssignmentWithDetails>>

    @Transaction
    @Query("SELECT academic_assignments.* FROM academic_assignments JOIN tasks ON tasks.id = academic_assignments.taskId WHERE tasks.deletedAt IS NULL AND academic_assignments.courseId = :courseId ORDER BY academic_assignments.id")
    fun observeForCourse(courseId: Long): Flow<List<AssignmentWithDetails>>

    @Transaction
    @Query("SELECT academic_assignments.* FROM academic_assignments JOIN tasks ON tasks.id = academic_assignments.taskId WHERE tasks.id = :taskId AND tasks.deletedAt IS NULL")
    fun observeByTask(taskId: Long): Flow<AssignmentWithDetails?>

    @Query("SELECT * FROM academic_assignments WHERE taskId = :taskId")
    suspend fun findByTask(taskId: Long): AcademicAssignmentEntity?

    @Query("SELECT * FROM academic_assignments WHERE id = :id")
    suspend fun findById(id: Long): AcademicAssignmentEntity?

    @Query("SELECT * FROM academic_deadline_changes WHERE assignmentId = :assignmentId ORDER BY id")
    suspend fun deadlineChanges(assignmentId: Long): List<AcademicDeadlineChangeEntity>

    @Query("SELECT * FROM academic_assignments ORDER BY id")
    suspend fun backupSnapshot(): List<AcademicAssignmentEntity>

    @Query("SELECT * FROM academic_deadline_changes ORDER BY assignmentId, id")
    suspend fun backupDeadlineChanges(): List<AcademicDeadlineChangeEntity>

    @Insert suspend fun insert(assignment: AcademicAssignmentEntity): Long
    @Update suspend fun update(assignment: AcademicAssignmentEntity): Int
    @Insert suspend fun insertDeadlineChange(change: AcademicDeadlineChangeEntity): Long
}
