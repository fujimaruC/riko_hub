package id.riko.hub.data.repository

import androidx.room.withTransaction
import id.riko.hub.core.time.Clock
import id.riko.hub.data.local.RikoDatabase
import id.riko.hub.data.local.entity.toDomain
import id.riko.hub.data.local.entity.toEntity
import id.riko.hub.data.mapper.toDomain
import id.riko.hub.data.mapper.toEntity
import id.riko.hub.domain.logic.newAssignment
import id.riko.hub.domain.logic.prepareAssignment
import id.riko.hub.domain.model.AcademicState
import kotlinx.coroutines.flow.map

class AssignmentRepository(private val database: RikoDatabase, private val clock: Clock) {
    private val assignments = database.assignments()
    private val tasks = database.tasks()
    private val courses = database.courses()

    fun observeByTask(taskId: Long) = assignments.observeByTask(taskId).map { row -> row?.let { it.assignment.toDomain(it.changes) } }
    fun observeForCourse(courseId: Long) = assignments.observeForCourse(courseId).map { rows -> rows.map { it.toDomain() } }
    fun observeAll() = assignments.observeAll().map { rows -> rows.map { it.toDomain() } }

    suspend fun save(taskId: Long, courseId: Long?, state: AcademicState, submissionReference: String?): Long = database.withTransaction {
        val task = requireNotNull(tasks.adminFindForWrite(taskId)?.toDomain()) { "Task does not exist" }
        require(task.deletedAt == null) { "Restore the task before creating or editing its assignment" }
        val current = assignments.findByTask(taskId)?.let { entity ->
            entity.toDomain(assignments.deadlineChanges(entity.id))
        }
        if (courseId != null) {
            val course = requireNotNull(courses.adminFindForWrite(courseId)) { "Course does not exist" }.course
            require(course.archivedAt == null || current?.courseId == courseId) { "Archived courses cannot be newly linked" }
        }
        val saved = if (current == null) {
            newAssignment(task, courseId, state, clock.now(), submissionReference)
        } else {
            prepareAssignment(current.copy(courseId = courseId, state = state), current, task, clock.now(), submissionReference)
        }
        val entity = saved.toEntity()
        if (current == null) assignments.insert(entity) else {
            check(assignments.update(entity) == 1)
            current.id
        }
    }
}
