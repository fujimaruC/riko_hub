package id.riko.hub.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import id.riko.hub.data.local.dao.EventDao
import id.riko.hub.data.local.dao.CourseDao
import id.riko.hub.data.local.dao.TaskDao
import id.riko.hub.data.local.dao.AssignmentDao
import id.riko.hub.data.local.dao.SpaceNoteDao
import id.riko.hub.data.local.entity.AcademicAssignmentEntity
import id.riko.hub.data.local.entity.AcademicDeadlineChangeEntity
import id.riko.hub.data.local.entity.CourseEntity
import id.riko.hub.data.local.entity.CourseWeeklySlotEntity
import id.riko.hub.data.local.entity.EventEntity
import id.riko.hub.data.local.entity.TaskEntity
import id.riko.hub.data.local.entity.SpaceNoteEntity

@Database(entities = [TaskEntity::class, EventEntity::class, CourseEntity::class, CourseWeeklySlotEntity::class, AcademicAssignmentEntity::class, AcademicDeadlineChangeEntity::class, SpaceNoteEntity::class], version = 5, exportSchema = true)
abstract class RikoDatabase : RoomDatabase() {
    abstract fun tasks(): TaskDao
    abstract fun events(): EventDao
    abstract fun courses(): CourseDao
    abstract fun assignments(): AssignmentDao
    abstract fun spaceNotes(): SpaceNoteDao
}
