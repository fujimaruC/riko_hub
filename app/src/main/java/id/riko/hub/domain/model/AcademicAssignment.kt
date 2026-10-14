package id.riko.hub.domain.model

import java.util.UUID

enum class AcademicState { RECEIVED, PLANNED, IN_PROGRESS, REVIEW, SUBMITTED, COMPLETED }

data class AcademicDeadlineChange(
    val id: Long = 0,
    val syncId: String = UUID.randomUUID().toString(),
    val assignmentId: Long = 0,
    val previousDeadlineAt: Long?,
    val previousHasDeadlineTime: Boolean,
    val newDeadlineAt: Long?,
    val newHasDeadlineTime: Boolean,
    val changedAt: Long,
)

data class AcademicAssignment(
    val id: Long = 0,
    val syncId: String = UUID.randomUUID().toString(),
    val taskId: Long,
    val courseId: Long? = null,
    val state: AcademicState = AcademicState.RECEIVED,
    val submittedAt: Long? = null,
    val submissionReference: String? = null,
    val originalDeadlineAt: Long? = null,
    val originalHasDeadlineTime: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val deadlineChanges: List<AcademicDeadlineChange> = emptyList(),
)

data class AssignmentTask(val assignment: AcademicAssignment, val task: Task)
