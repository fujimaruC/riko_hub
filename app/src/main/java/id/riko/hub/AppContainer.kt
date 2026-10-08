package id.riko.hub

import android.content.Context
import androidx.room.Room
import id.riko.hub.core.time.Clock
import id.riko.hub.core.time.SystemClock
import id.riko.hub.data.local.RikoDatabase
import id.riko.hub.data.local.Migrations
import id.riko.hub.data.prefs.SettingsDataStore
import id.riko.hub.data.repository.EventRepository
import id.riko.hub.data.repository.TaskRepository

class AppContainer(context: Context, val clock: Clock = SystemClock) {
    private val appContext = context.applicationContext
    val database by lazy {
        Room.databaseBuilder(appContext, RikoDatabase::class.java, "riko_hub.db")
            .addMigrations(Migrations.MIGRATION_1_2).build()
    }
    val tasks by lazy { TaskRepository(database, clock) }
    val events by lazy { EventRepository(database, clock) }
    val settings by lazy { SettingsDataStore(appContext) }
}
