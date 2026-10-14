package id.riko.hub.data.repository

import android.util.Log
import androidx.room.withTransaction
import id.riko.hub.core.time.Clock
import id.riko.hub.data.local.RikoDatabase
import id.riko.hub.data.local.entity.AcademicDeadlineChangeEntity
import id.riko.hub.data.mapper.*
import id.riko.hub.domain.logic.prepareEvent
import id.riko.hub.domain.logic.prepareTask
import id.riko.hub.domain.logic.nextTaskDeadline
import id.riko.hub.domain.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.map
import java.util.UUID

private suspend fun <T> RikoDatabase.write(block: suspend () -> T): T = try {
    withTransaction { block() }
} catch (error: Exception) {
    if (error !is CancellationException) Log.e("RikoRepository", "Local write failed", error)
    throw error
}

class TaskRepository(private val database: RikoDatabase, private val clock: Clock, private val onMutation: () -> Unit = {}) {
    private val dao = database.tasks()
    fun observeActive() = dao.observeActive().map { rows -> rows.map { it.toDomain() } }
    fun observeAll() = dao.observeAll().map { rows -> rows.map { it.toDomain() } }
    fun observeById(id: Long) = dao.observeById(id).map { it?.toDomain() }

    suspend fun upsert(task: Task): Long = database.write { save(task) }.also { requestReconciliation() }

    private suspend fun save(task: Task): Long {
        val previous = if (task.id == 0L) null else dao.adminFindForWrite(task.id)?.toDomain()
        val row = prepareTask(task, previous, clock.now()).toEntity()
        val id = if (row.id == 0L) dao.insert(row) else {
            check(dao.update(row) == 1)
            row.id
        }
        val saved = row.toDomain().copy(id = id)
        if (previous != null && (previous.deadlineAt != saved.deadlineAt || previous.hasDeadlineTime != saved.hasDeadlineTime)) {
            database.assignments().findByTask(id)?.let { assignment ->
                val changedAt = maxOf(clock.now(), saved.updatedAt, assignment.createdAt, assignment.updatedAt)
                require(changedAt in 0..253402300799999L) { "Invalid assignment timestamp" }
                require((previous.deadlineAt == null || previous.deadlineAt in 0..253402300799999L) &&
                    (saved.deadlineAt == null || saved.deadlineAt in 0..253402300799999L) &&
                    (!previous.hasDeadlineTime || previous.deadlineAt != null) &&
                    (!saved.hasDeadlineTime || saved.deadlineAt != null)) {
                    "Invalid assignment date"
                }
                database.assignments().insertDeadlineChange(AcademicDeadlineChangeEntity(
                    syncId = UUID.randomUUID().toString(), assignmentId = assignment.id,
                    previousDeadlineAt = previous.deadlineAt, previousHasDeadlineTime = previous.hasDeadlineTime,
                    newDeadlineAt = saved.deadlineAt, newHasDeadlineTime = saved.hasDeadlineTime, changedAt = changedAt,
                ))
                check(database.assignments().update(assignment.copy(updatedAt = changedAt)) == 1)
            }
        }
        if (previous?.status == TaskStatus.DONE && saved.status != TaskStatus.DONE) {
            dao.findGeneratedSuccessor(id)?.let { successor ->
                dao.deleteGeneratedForUndo(successor.id, maxOf(clock.now(), successor.createdAt, successor.updatedAt))
            }
        }
        if (saved.status == TaskStatus.DONE) ensureSuccessor(saved)
        return id
    }

    private suspend fun ensureSuccessor(task: Task) {
        val deadline = nextTaskDeadline(task, clock.now(), clock.zone()) ?: return
        val existing = dao.findGeneratedSuccessor(task.id)
        if (existing != null) {
            dao.restoreGeneratedForRedo(existing.id, maxOf(clock.now(), existing.createdAt, existing.updatedAt))
            return
        }
        val now = maxOf(clock.now(), task.createdAt, task.updatedAt)
        dao.insert(task.copy(
            id = 0,
            syncId = UUID.randomUUID().toString(),
            status = TaskStatus.NOT_STARTED,
            deadlineAt = deadline,
            createdAt = now,
            updatedAt = now,
            completedAt = null,
            deletedAt = null,
            checklist = task.checklist.map { it.copy(done = false) },
            generatedFromTaskId = task.id,
            generationPristine = true,
        ).toEntity())
    }

    suspend fun setStatus(id: Long, status: TaskStatus) = database.write {
        val previous = requireNotNull(dao.adminFindForWrite(id)?.toDomain()) { "Task does not exist" }
        save(previous.copy(status = status))
        Unit
    }.also { requestReconciliation() }

    suspend fun softDelete(id: Long) = setDeleted(id, true)
    suspend fun restore(id: Long) = setDeleted(id, false)
    private suspend fun setDeleted(id: Long, deleted: Boolean) = database.write {
        val previous = requireNotNull(dao.adminFindForWrite(id)) { "Task does not exist" }
        val timestamp = maxOf(clock.now(), previous.createdAt, previous.updatedAt)
        dao.setDeletedAt(id, if (deleted) previous.deletedAt ?: timestamp else null, timestamp)
    }.also { requestReconciliation() }

    private fun requestReconciliation() {
        try { onMutation() }
        catch (error: Exception) {
            if (error !is CancellationException) Log.e("RikoRepository", "Reminder reconciliation request failed", error)
        }
    }
}

class EventRepository(private val database: RikoDatabase, private val clock: Clock) {
    private val dao = database.events()
    fun observeBetween(start: Long, end: Long) = dao.observeBetween(start, end).map { rows -> rows.map { it.toDomain() } }
    fun observeById(id: Long) = dao.observeById(id).map { it?.toDomain() }

    suspend fun upsert(event: Event): Long = database.write {
        val previous = if (event.id == 0L) null else dao.adminFindForWrite(event.id)?.toDomain()
        val row = prepareEvent(event, previous, clock.now()).toEntity()
        if (row.id == 0L) dao.insert(row) else { check(dao.update(row) == 1); row.id }
    }

    suspend fun softDelete(id: Long) = setDeleted(id, true)
    suspend fun restore(id: Long) = setDeleted(id, false)
    private suspend fun setDeleted(id: Long, deleted: Boolean) = database.write {
        val previous = requireNotNull(dao.adminFindForWrite(id)) { "Event does not exist" }
        val timestamp = maxOf(clock.now(), previous.createdAt, previous.updatedAt)
        dao.setDeletedAt(id, if (deleted) previous.deletedAt ?: timestamp else null, timestamp)
    }
}
