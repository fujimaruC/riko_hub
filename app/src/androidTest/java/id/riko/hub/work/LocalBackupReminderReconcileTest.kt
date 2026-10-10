package id.riko.hub.work

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkInfo
import androidx.work.WorkManager
import id.riko.hub.core.time.Clock
import id.riko.hub.data.backup.LocalBackupService
import id.riko.hub.data.local.RikoDatabase
import id.riko.hub.data.local.entity.TaskEntity
import id.riko.hub.data.prefs.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.time.ZoneId

class LocalBackupReminderReconcileTest {
    private class Fixture {
        val scopes = mutableListOf<CoroutineScope>()
        val files = mutableListOf<File>()
        fun settings(name: String): SettingsDataStore {
            val file = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir.resolve("$name-${System.nanoTime()}.preferences_pb")
            files += file
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            scopes += scope
            val store: DataStore<Preferences> = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
            return SettingsDataStore(store)
        }
        suspend fun close() {
            scopes.forEach { it.cancel() }
            scopes.mapNotNull { it.coroutineContext[Job] }.joinAll()
            files.forEach(File::delete)
        }
    }

    @Test fun importSchedulesOnlyEligibleTasksAndLeavesNoDuplicateFixtureWork() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = Room.inMemoryDatabaseBuilder(context, RikoDatabase::class.java).build()
        val target = Room.inMemoryDatabaseBuilder(context, RikoDatabase::class.java).build()
        val work = WorkManager.getInstance(context)
        val f = Fixture()
        val now = System.currentTimeMillis()
        val clock = object : Clock { override fun now() = now; override fun zone() = ZoneId.of("UTC") }
        val fixtureId = 8_000_000_000_000_000L + System.nanoTime()
        val dummyName = TaskReminders.reminderWorkName(fixtureId)
        try {
            assertTrue(work.getWorkInfosForUniqueWork(dummyName).get().isEmpty())
            target.tasks().insert(task(fixtureId, "dummy", now))
            source.tasks().insert(task(0, "eligible", now).copy(deadlineAt = now + 2 * DAY, reminderMinutesBefore = 0))
            source.tasks().insert(task(0, "completed", now).copy(status = "DONE", completedAt = now, deadlineAt = now + 2 * DAY, reminderMinutesBefore = 0))
            source.tasks().insert(task(0, "deleted", now).copy(deletedAt = now, deadlineAt = now + 2 * DAY, reminderMinutesBefore = 0))
            val sourcePrefs = f.settings("backup-reminder-source")
            val bytes = LocalBackupService(source, sourcePrefs, clock) {}.export()
            val service = LocalBackupService(target, f.settings("backup-reminder-target"), clock) {
                reconcileTaskReminders(target, clock, work) { }
            }
            val preview = service.preview(ByteArrayInputStream(bytes))
            val result = service.importBackup(preview, false)
            assertEquals(3, result.insertedTasks)
            val rows = target.tasks().backupSnapshot().filter { it.syncId != "dummy" }.associate { it.syncId to it.id }
            val eligible = rows.getValue("eligible")
            val firstWork = activeWork(work, eligible)
            assertEquals(WorkInfo.State.ENQUEUED, firstWork.state)
            assertTrue(activeWorkItems(work, rows.getValue("completed")).isEmpty())
            assertTrue(activeWorkItems(work, rows.getValue("deleted")).isEmpty())
            val repeated = service.importBackup(service.preview(ByteArrayInputStream(bytes)), false)
            assertEquals(0, repeated.insertedTasks)
            assertEquals(0, repeated.insertedEvents)
            val afterRepeat = activeWork(work, eligible)
            assertEquals(firstWork.id, afterRepeat.id)
            assertEquals(1, activeWorkItems(work, eligible).size)
        } finally {
            (target.tasks().backupSnapshot().map { it.id } + fixtureId).forEach { work.cancelUniqueWork(TaskReminders.reminderWorkName(it)).result.get() }
            source.close(); target.close(); f.close()
        }
    }

    private fun task(id: Long, sync: String, now: Long) = TaskEntity(
        id = id, syncId = sync, title = sync, notes = null, category = "PERSONAL", priority = "MEDIUM",
        status = "NOT_STARTED", deadlineAt = null, hasDeadlineTime = false, createdAt = now, updatedAt = now,
        completedAt = null, deletedAt = null, estimatedMinutes = null, recurrence = "NONE", checklistJson = "[]",
        reminderMinutesBefore = null, generatedFromTaskId = null, generationPristine = false, recurrenceAnchorAt = null,
    )

    private fun activeWork(work: WorkManager, id: Long) = activeWorkItems(work, id).single()
    private fun activeWorkItems(work: WorkManager, id: Long) = work.getWorkInfosForUniqueWork(TaskReminders.reminderWorkName(id)).get()
        .filter { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.BLOCKED }

    companion object { private const val DAY = 86_400_000L }
}
