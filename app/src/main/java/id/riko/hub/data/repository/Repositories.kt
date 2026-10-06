package id.riko.hub.data.repository

import android.util.Log
import androidx.room.withTransaction
import id.riko.hub.core.time.Clock
import id.riko.hub.data.local.RikoDatabase
import id.riko.hub.data.mapper.*
import id.riko.hub.domain.logic.prepareEvent
import id.riko.hub.domain.logic.prepareTask
import id.riko.hub.domain.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.map

private suspend fun <T> RikoDatabase.write(block: suspend () -> T): T = try {
    withTransaction { block() }
} catch (error: Exception) {
    if (error !is CancellationException) Log.e("RikoRepository", "Local write failed", error)
    throw error
}

class TaskRepository(private val database: RikoDatabase, private val clock: Clock) {
    private val dao = database.tasks()
    fun observeActive() = dao.observeActive().map { rows -> rows.map { it.toDomain() } }
    fun observeAll() = dao.observeAll().map { rows -> rows.map { it.toDomain() } }
    fun observeById(id: Long) = dao.observeById(id).map { it?.toDomain() }

    suspend fun upsert(task: Task): Long = database.write {
        val previous = if (task.id == 0L) null else dao.adminFindForWrite(task.id)?.toDomain()
        val row = prepareTask(task, previous, clock.now()).toEntity()
        if (row.id == 0L) dao.insert(row) else { check(dao.update(row) == 1); row.id }
    }

    suspend fun setStatus(id: Long, status: TaskStatus) = database.write {
        val previous = requireNotNull(dao.adminFindForWrite(id)?.toDomain()) { "Task does not exist" }
        val row = prepareTask(previous.copy(status = status), previous, clock.now()).toEntity()
        check(dao.update(row) == 1)
    }

    suspend fun softDelete(id: Long) = setDeleted(id, true)
    suspend fun restore(id: Long) = setDeleted(id, false)
    private suspend fun setDeleted(id: Long, deleted: Boolean) = database.write {
        val previous = requireNotNull(dao.adminFindForWrite(id)) { "Task does not exist" }
        val timestamp = maxOf(clock.now(), previous.createdAt, previous.updatedAt)
        dao.setDeletedAt(id, if (deleted) previous.deletedAt ?: timestamp else null, timestamp)
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
