package id.riko.hub.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import id.riko.hub.data.mapper.toDomain
import id.riko.hub.domain.model.AcademicAssignment
import id.riko.hub.domain.model.AcademicDeadlineChange
import id.riko.hub.domain.model.AcademicState
import id.riko.hub.domain.model.AssignmentTask

@Entity(tableName = "academic_assignments", foreignKeys = [
    ForeignKey(entity = TaskEntity::class, parentColumns = ["id"], childColumns = ["taskId"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = CourseEntity::class, parentColumns = ["id"], childColumns = ["courseId"], onDelete = ForeignKey.SET_NULL),
], indices = [
    Index(value = ["syncId"], unique = true, name = "index_academic_assignments_syncId"),
    Index(value = ["taskId"], unique = true, name = "index_academic_assignments_taskId"),
    Index(value = ["courseId"], name = "index_academic_assignments_courseId"),
])
data class AcademicAssignmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val syncId: String,
    val taskId: Long,
    val courseId: Long?,
    val state: String,
    val submittedAt: Long?,
    val submissionReference: String?,
    val originalDeadlineAt: Long?,
    val originalHasDeadlineTime: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "academic_deadline_changes", foreignKeys = [ForeignKey(
    entity = AcademicAssignmentEntity::class, parentColumns = ["id"], childColumns = ["assignmentId"], onDelete = ForeignKey.CASCADE,
)], indices = [
    Index(value = ["syncId"], unique = true, name = "index_academic_deadline_changes_syncId"),
    Index(value = ["assignmentId"], name = "index_academic_deadline_changes_assignmentId"),
])
data class AcademicDeadlineChangeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val syncId: String,
    val assignmentId: Long,
    val previousDeadlineAt: Long?,
    val previousHasDeadlineTime: Boolean,
    val newDeadlineAt: Long?,
    val newHasDeadlineTime: Boolean,
    val changedAt: Long,
)

data class AssignmentWithDetails(
    @Embedded val assignment: AcademicAssignmentEntity,
    @Relation(parentColumn = "id", entityColumn = "assignmentId") val changes: List<AcademicDeadlineChangeEntity>,
    @Relation(parentColumn = "taskId", entityColumn = "id") val task: TaskEntity,
)

fun AcademicAssignmentEntity.toDomain(changes: List<AcademicDeadlineChangeEntity> = emptyList()) = AcademicAssignment(
    id, syncId, taskId, courseId, AcademicState.valueOf(state), submittedAt, submissionReference,
    originalDeadlineAt, originalHasDeadlineTime, createdAt, updatedAt, changes.sortedBy { it.id }.map { it.toDomain() },
)

fun AcademicDeadlineChangeEntity.toDomain() = AcademicDeadlineChange(
    id, syncId, assignmentId, previousDeadlineAt, previousHasDeadlineTime,
    newDeadlineAt, newHasDeadlineTime, changedAt,
)

fun AcademicAssignment.toEntity() = AcademicAssignmentEntity(
    id, syncId, taskId, courseId, state.name, submittedAt, submissionReference,
    originalDeadlineAt, originalHasDeadlineTime, createdAt, updatedAt,
)

fun AcademicDeadlineChange.toEntity() = AcademicDeadlineChangeEntity(
    id, syncId, assignmentId, previousDeadlineAt, previousHasDeadlineTime,
    newDeadlineAt, newHasDeadlineTime, changedAt,
)

fun AssignmentWithDetails.toDomain() = AssignmentTask(assignment.toDomain(changes), task.toDomain())
