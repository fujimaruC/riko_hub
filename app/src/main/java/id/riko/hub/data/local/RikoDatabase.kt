package id.riko.hub.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import id.riko.hub.data.local.dao.EventDao
import id.riko.hub.data.local.dao.TaskDao
import id.riko.hub.data.local.entity.EventEntity
import id.riko.hub.data.local.entity.TaskEntity

@Database(entities = [TaskEntity::class, EventEntity::class], version = 1, exportSchema = true)
abstract class RikoDatabase : RoomDatabase() {
    abstract fun tasks(): TaskDao
    abstract fun events(): EventDao
}
