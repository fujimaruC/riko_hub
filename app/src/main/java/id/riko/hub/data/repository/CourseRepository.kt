package id.riko.hub.data.repository

import androidx.room.withTransaction
import id.riko.hub.core.time.Clock
import id.riko.hub.data.local.RikoDatabase
import id.riko.hub.data.local.entity.toDomain
import id.riko.hub.data.local.entity.toEntity
import id.riko.hub.data.local.entity.toSlotEntities
import id.riko.hub.domain.model.Course
import id.riko.hub.domain.model.normalizeAndValidateCourse
import kotlinx.coroutines.flow.map

class CourseRepository(private val database: RikoDatabase, private val clock: Clock) {
    private val dao = database.courses()

    fun observeAll() = dao.observeAll().map { rows -> rows.map { it.toDomain() } }
    fun observeById(id: Long) = dao.observeById(id).map { it?.toDomain() }

    suspend fun upsert(course: Course): Long = database.withTransaction {
        val input = normalizeAndValidateCourse(course)
        val previous = if (input.id == 0L) null else requireNotNull(dao.adminFindForWrite(input.id)) {
            "Course does not exist"
        }.toDomain()
        val now = clock.now()
        val maxOrder = dao.maxSortOrder()
        if (previous == null && maxOrder == Long.MAX_VALUE) {
            dao.findAllForOrder().forEachIndexed { position, row ->
                check(dao.update(row.copy(sortOrder = position.toLong(), updatedAt = maxOf(now, row.createdAt, row.updatedAt))) == 1)
            }
        }
        val appendOrder = if (maxOrder == Long.MAX_VALUE) dao.maxSortOrder() ?: -1L else maxOrder ?: -1L
        val saved = if (previous == null) input.copy(
            id = 0,
            sortOrder = appendOrder + 1L,
            archivedAt = null,
            createdAt = now,
            updatedAt = now,
        ) else input.copy(
            syncId = previous.syncId,
            sortOrder = previous.sortOrder,
            archivedAt = previous.archivedAt,
            createdAt = previous.createdAt,
            updatedAt = maxOf(now, previous.createdAt, previous.updatedAt),
        )
        val row = saved.toEntity()
        val id = if (previous == null) dao.insert(row) else {
            check(dao.update(row) == 1)
            row.id
        }
        dao.deleteSlots(id)
        if (saved.weeklySlots.isNotEmpty()) dao.insertSlots(saved.toSlotEntities(id))
        id
    }

    suspend fun setArchived(id: Long, archived: Boolean) = database.withTransaction {
        val previous = requireNotNull(dao.adminFindForWrite(id)?.toDomain()) { "Course does not exist" }
        val now = maxOf(clock.now(), previous.createdAt, previous.updatedAt)
        val archivedAt = if (archived) previous.archivedAt ?: now else null
        check(dao.update(previous.copy(archivedAt = archivedAt, updatedAt = now).toEntity()) == 1)
    }

    suspend fun move(id: Long, direction: Int) = database.withTransaction {
        require(direction == -1 || direction == 1) { "Direction must be -1 or 1" }
        val course = requireNotNull(dao.adminFindForWrite(id)?.course) { "Course does not exist" }
        val ordered = dao.findOrderedGroup(active = course.archivedAt == null).toMutableList()
        val index = ordered.indexOfFirst { it.id == id }
        require(index >= 0) { "Course does not exist" }
        val neighborIndex = index + direction
        if (neighborIndex !in ordered.indices) return@withTransaction
        val neighbor = ordered[neighborIndex]
        if (course.sortOrder == neighbor.sortOrder) {
            val current = ordered[index]
            ordered[index] = neighbor
            ordered[neighborIndex] = current
            val now = clock.now()
            ordered.forEachIndexed { position, row ->
                check(dao.update(row.copy(sortOrder = position.toLong(), updatedAt = maxOf(now, row.createdAt, row.updatedAt))) == 1)
            }
        } else {
            val now = clock.now()
            check(dao.update(course.copy(sortOrder = neighbor.sortOrder, updatedAt = maxOf(now, course.createdAt, course.updatedAt))) == 1)
            check(dao.update(neighbor.copy(sortOrder = course.sortOrder, updatedAt = maxOf(now, neighbor.createdAt, neighbor.updatedAt))) == 1)
        }
        Unit
    }
}
