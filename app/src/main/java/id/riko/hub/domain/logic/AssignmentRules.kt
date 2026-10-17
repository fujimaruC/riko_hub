package id.riko.hub.domain.logic

import id.riko.hub.domain.model.AcademicAssignment
import id.riko.hub.domain.model.AcademicState
import id.riko.hub.domain.model.Task
import java.util.UUID

const val MAX_ASSIGNMENT_TIMESTAMP = 253402300799999L

fun allowedAcademicTransitions(state: AcademicState): Set<AcademicState> = when (state) {
    AcademicState.COMPLETED -> setOf(AcademicState.IN_PROGRESS)
    AcademicState.SUBMITTED -> setOf(AcademicState.IN_PROGRESS, AcademicState.REVIEW, AcademicState.COMPLETED)
    else -> AcademicState.values().toSet()
}

fun prepareAssignment(
    input: AcademicAssignment,
    previous: AcademicAssignment?,
    task: Task,
    now: Long,
    submissionReference: String?,
): AcademicAssignment {
    require(input.id == if (previous == null) 0L else previous.id) { "Assignment does not exist" }
    require(task.id == input.taskId && task.deletedAt == null) { "Task does not exist or is deleted" }
    require(!task.hasDeadlineTime || task.deadlineAt != null) { "Timed assignment deadline requires a deadline" }
    require(input.taskId > 0L && (input.courseId == null || input.courseId > 0L)) { "Invalid assignment link" }
    require(input.syncId.isNotBlank() && input.syncId.length <= 120) { "Invalid assignment ID" }
    require(now in 0..MAX_ASSIGNMENT_TIMESTAMP) { "Invalid timestamp" }
    require(listOfNotNull(task.deadlineAt, previous?.originalDeadlineAt, previous?.submittedAt).all { it in 0..MAX_ASSIGNMENT_TIMESTAMP }) {
        "Invalid assignment date"
    }
    require(submissionReference == null || submissionReference.length <= 2000) { "Submission reference must contain at most 2000 characters" }
    require(previous == null || input.state in allowedAcademicTransitions(previous.state) || input.state == previous.state) {
        "Invalid assignment state transition"
    }
    val timestamp = maxOf(now, task.createdAt, task.updatedAt, previous?.createdAt ?: now, previous?.updatedAt ?: now)
    require(timestamp in 0..MAX_ASSIGNMENT_TIMESTAMP) { "Invalid assignment timestamp" }
    return input.copy(
        syncId = previous?.syncId ?: input.syncId,
        taskId = task.id,
        submittedAt = if (input.state == AcademicState.SUBMITTED && previous?.state != AcademicState.SUBMITTED)
            timestamp else previous?.submittedAt,
        submissionReference = if (submissionReference == null) previous?.submissionReference
            else submissionReference.trim().takeIf(String::isNotEmpty),
        originalDeadlineAt = if (previous == null) task.deadlineAt else previous.originalDeadlineAt,
        originalHasDeadlineTime = previous?.originalHasDeadlineTime ?: task.hasDeadlineTime,
        createdAt = previous?.createdAt ?: timestamp,
        updatedAt = timestamp,
        deadlineChanges = previous?.deadlineChanges.orEmpty(),
    )
}

fun newAssignment(task: Task, courseId: Long?, state: AcademicState, now: Long, reference: String?) = prepareAssignment(
    AcademicAssignment(syncId = UUID.randomUUID().toString(), taskId = task.id, courseId = courseId, state = state),
    null, task, now, reference,
)
