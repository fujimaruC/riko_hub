package id.riko.hub.data.backup

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import id.riko.hub.core.time.Clock
import id.riko.hub.data.local.RikoDatabase
import id.riko.hub.data.local.entity.EventEntity
import id.riko.hub.data.local.entity.TaskEntity
import id.riko.hub.data.prefs.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.time.ZoneId
import java.io.File

class LocalBackupServiceTest {
    private val clock = object : Clock { override fun now() = 1000L; override fun zone() = ZoneId.of("UTC") }

    private fun store(context: android.content.Context, name: String, scopes: MutableList<CoroutineScope>, files: MutableList<File>): DataStore<Preferences> {
        val file = context.cacheDir.resolve("$name-${System.nanoTime()}.preferences_pb")
        files += file
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scopes += scope
        return PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
    }

    @Test fun backup_round_trip_keeps_tombstones_completion_and_parent_identity() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = Room.inMemoryDatabaseBuilder(context, RikoDatabase::class.java).build()
        val target = Room.inMemoryDatabaseBuilder(context, RikoDatabase::class.java).build()
        val scopes = mutableListOf<CoroutineScope>(); val files = mutableListOf<File>()
        try {
            val parent = source.tasks().insert(TaskEntity(syncId="parent", title="Done", notes=null, category="PERSONAL", priority="HIGH", status="DONE", deadlineAt=100, hasDeadlineTime=false, createdAt=10, updatedAt=30, completedAt=20, deletedAt=30, estimatedMinutes=null, recurrence="WEEKLY", checklistJson="[]", reminderMinutesBefore=null, generatedFromTaskId=null, generationPristine=false, recurrenceAnchorAt=10))
            source.tasks().insert(TaskEntity(syncId="child", title="Next", notes=null, category="PERSONAL", priority="LOW", status="NOT_STARTED", deadlineAt=null, hasDeadlineTime=false, createdAt=40, updatedAt=40, completedAt=null, deletedAt=null, estimatedMinutes=null, recurrence="NONE", checklistJson="[]", reminderMinutesBefore=null, generatedFromTaskId=parent, generationPristine=true, recurrenceAnchorAt=null))
            source.events().insert(EventEntity(syncId="event", title="Exam", notes=null, kind="EXAM", category="ACADEMIC", startAt=50, endAt=60, location=null, createdAt=1, updatedAt=3, deletedAt=3))
            val preferences = SettingsDataStore(store(context, "backup-source", scopes, files))
            preferences.setPinnedFocus(parent, LocalDate.parse("2026-10-10"))
            val sourceService = LocalBackupService(source, preferences, clock) {}
            val bytes = sourceService.export()
            val targetPrefs = SettingsDataStore(store(context, "backup-target", scopes, files))
            var reconciles = 0
            val targetService = LocalBackupService(target, targetPrefs, clock) { reconciles++ }
            val preview = targetService.preview(ByteArrayInputStream(bytes))
            assertEquals(2, preview.newTasks)
            assertEquals(1, preview.newEvents)
            val result = targetService.importBackup(preview, restoreSettings = true)
            assertEquals(2, result.insertedTasks)
            assertEquals(1, result.insertedEvents)
            assertEquals(1, reconciles)
            val restoredParent = requireNotNull(target.tasks().adminFindAllForReminders().firstOrNull { it.syncId == "parent" })
            val restoredChild = requireNotNull(target.tasks().adminFindAllForReminders().firstOrNull { it.syncId == "child" })
            assertEquals(restoredParent.id, restoredChild.generatedFromTaskId)
            assertEquals(30L, restoredParent.deletedAt)
            assertEquals(20L, restoredParent.completedAt)
            assertEquals("parent", targetPrefs.backupValues(target.tasks().backupSnapshot().associate { it.id to it.syncId }).pinnedFocusSyncId)
            val repeat = targetService.preview(ByteArrayInputStream(bytes))
            assertEquals(3, repeat.duplicates)
            val repeatedImport = targetService.importBackup(repeat, restoreSettings = true)
            assertEquals(0, repeatedImport.insertedTasks)
            assertEquals(0, repeatedImport.insertedEvents)
            assertEquals(2, reconciles)
            assertEquals(2, target.tasks().adminCount())
        } finally { source.close(); target.close(); scopes.forEach { it.cancel() }; scopes.mapNotNull { it.coroutineContext[Job] }.joinAll(); files.forEach(File::delete) }
    }

    @Test fun rejects_future_version_and_oversized_input_before_touching_database() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, RikoDatabase::class.java).build()
        val scopes = mutableListOf<CoroutineScope>(); val files = mutableListOf<File>()
        try {
            val service = LocalBackupService(db, SettingsDataStore(store(context, "backup-invalid", scopes, files)), clock) {}
            val valid = service.export().toString(Charsets.UTF_8)
            listOf(
                valid.replace("\"Riko Hub\"", "\"Riko\\q Hub\""),
                valid.replace("\"hints\":0", "\"hints\":TRUE"),
                valid.replace("\"displayName\":\"Riko\"", "\"displayName\":\"Ri\nko\""),
            ).forEach { malformed ->
                val failure = runCatching { service.preview(ByteArrayInputStream(malformed.toByteArray())) }.exceptionOrNull() as BackupException
                assertEquals(BackupFailure.INVALID, failure.reason)
            }
            val future = valid.replace("\"formatVersion\":4", "\"formatVersion\":99")
            val e = runCatching { service.preview(ByteArrayInputStream(future.toByteArray())) }.exceptionOrNull() as BackupException
            assertEquals(BackupFailure.UNSUPPORTED_VERSION, e.reason)
            val oversized = ByteArray(8 * 1024 * 1024 + 1)
            val large = runCatching { service.preview(ByteArrayInputStream(oversized)) }.exceptionOrNull() as BackupException
            assertEquals(BackupFailure.TOO_LARGE, large.reason)
            assertEquals(0, db.tasks().adminCount())
        } finally { db.close(); scopes.forEach { it.cancel() }; scopes.mapNotNull { it.coroutineContext[Job] }.joinAll(); files.forEach(File::delete) }
    }
}
