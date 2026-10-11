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
import id.riko.hub.data.backup.LocalBackupService
import id.riko.hub.work.TaskReminders
import id.riko.hub.ui.widget.TaskWidgets
import id.riko.hub.data.remote.drive.DriveRepository
import id.riko.hub.data.remote.drive.GoogleDriveAuthorization
import id.riko.hub.data.remote.drive.MaterialsRepository
import id.riko.hub.data.remote.device.CompanionRepository

class AppContainer(context: Context, val clock: Clock = SystemClock) {
    private val appContext = context.applicationContext
    val database by lazy {
        Room.databaseBuilder(appContext, RikoDatabase::class.java, "riko_hub.db")
            .addMigrations(Migrations.MIGRATION_1_2).build()
    }
    val reminders by lazy { TaskReminders(appContext) }
    val widgets by lazy { TaskWidgets(appContext) }
    val tasks by lazy { TaskRepository(database, clock) { reminders.requestReconciliation(); widgets.requestUpdate() } }
    val events by lazy { EventRepository(database, clock) }
    val settings by lazy { SettingsDataStore(appContext) }
    val materials: MaterialsRepository by lazy { DriveRepository(appContext) }
    val driveAuthorization by lazy { GoogleDriveAuthorization(appContext) }
    val companion: CompanionRepository by lazy { CompanionRepository(appContext) }
    val backups by lazy { LocalBackupService(database, settings, clock) {
        try { reminders.requestReconciliationChecked() } finally { widgets.requestUpdate() }
    } }
}
