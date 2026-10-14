package id.riko.hub.data.backup

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import id.riko.hub.core.time.Clock
import id.riko.hub.data.local.RikoDatabase
import id.riko.hub.data.local.entity.EventEntity
import id.riko.hub.data.local.entity.TaskEntity
import id.riko.hub.data.mapper.toDomain
import id.riko.hub.data.prefs.SettingsDataStore
import id.riko.hub.domain.logic.taskReminderToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.util.concurrent.CancellationException
import java.time.ZoneId
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.startCoroutine

class LocalBackupMergeTest {
    private val clock = object : Clock {
        override fun now() = 1_000L
        override fun zone() = ZoneId.of("UTC")
    }

    private data class Fixture(val scopes: MutableList<CoroutineScope>, val files: MutableList<File>) {
        fun settings(name: String) = SettingsDataStore(store(name))
        private fun store(name: String): DataStore<Preferences> {
            val file = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
                .resolve("$name-${System.nanoTime()}.preferences_pb")
            files += file
            return PreferenceDataStoreFactory.create(
                scope = CoroutineScope(SupervisorJob() + Dispatchers.IO).also(scopes::add),
                produceFile = { file },
            )
        }
        suspend fun close() { scopes.forEach { it.cancel() }; scopes.mapNotNull { it.coroutineContext[Job] }.joinAll(); files.forEach(File::delete) }
    }

    private fun fixture() = Fixture(mutableListOf(), mutableListOf())
    private fun failingSettings() = SettingsDataStore(object : DataStore<Preferences> {
        override val data: Flow<Preferences> = flowOf(emptyPreferences())
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = throw IOException("fixture write failure")
    })
    private fun updatingSettings(onUpdate: suspend () -> Unit) = SettingsDataStore(object : DataStore<Preferences> {
        override val data: Flow<Preferences> = flowOf(emptyPreferences())
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            onUpdate()
            return transform(emptyPreferences())
        }
    })
    private fun db(trigger: String? = null): RikoDatabase {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return Room.inMemoryDatabaseBuilder(context, RikoDatabase::class.java)
            .apply { if (trigger != null) addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) { db.execSQL(trigger) }
            }) }.build()
    }

    private fun task(sync: String, title: String = sync, parent: Long? = null, status: String = "NOT_STARTED", deleted: Long? = null) =
        TaskEntity(syncId = sync, title = title, notes = null, category = "PERSONAL", priority = "MEDIUM", status = status,
            deadlineAt = 100_000_000L, hasDeadlineTime = false, createdAt = 10, updatedAt = 20,
            completedAt = if (status == "DONE") 20 else null, deletedAt = deleted, estimatedMinutes = null,
            recurrence = "NONE", checklistJson = "[]", reminderMinutesBefore = 0, generatedFromTaskId = parent,
            generationPristine = false, recurrenceAnchorAt = null)

    private fun event(sync: String, title: String = sync) = EventEntity(syncId = sync, title = title, notes = null,
        kind = "EXAM", category = "ACADEMIC", startAt = 50, endAt = 60, location = null, createdAt = 1, updatedAt = 2, deletedAt = null)

    private suspend fun export(source: RikoDatabase, settings: SettingsDataStore) = LocalBackupService(source, settings, clock) {}.export()

    @Test fun emptyBackupImportsAndRepeatedImportIsIdempotent() = runBlocking {
        val f = fixture(); val source = db(); val target = db()
        try {
            val bytes = export(source, f.settings("empty-source"))
            var reconciles = 0
            val service = LocalBackupService(target, f.settings("empty-target"), clock) { reconciles++ }
            val firstPreview = service.preview(ByteArrayInputStream(bytes))
            assertEquals(0, firstPreview.newTasks); assertEquals(0, firstPreview.newEvents)
            assertEquals(BackupImportResult(0, 0, false, false), service.importBackup(firstPreview, false))
            val second = service.importBackup(service.preview(ByteArrayInputStream(bytes)), false)
            assertEquals(0, second.insertedTasks); assertEquals(0, second.insertedEvents)
            assertEquals(2, reconciles)
        } finally { source.close(); target.close(); f.close() }
    }

    @Test fun exactDuplicatesAreKeptAndDifferingSyncIdConflictsAreSkipped() = runBlocking {
        val f = fixture(); val source = db(); val target = db()
        try {
            source.tasks().insert(task("same", "same")); source.tasks().insert(task("changed", "incoming"))
            source.events().insert(event("same-event", "same")); source.events().insert(event("changed-event", "incoming"))
            target.tasks().insert(task("same", "same")); target.tasks().insert(task("changed", "local"))
            target.events().insert(event("same-event", "same")); target.events().insert(event("changed-event", "local"))
            val service = LocalBackupService(target, f.settings("merge"), clock) {}
            val preview = service.preview(ByteArrayInputStream(export(source, f.settings("merge-source"))))
            assertEquals(2, preview.duplicates)
            assertEquals(2, preview.conflicts)
            assertEquals(0, preview.newTasks); assertEquals(0, preview.newEvents)
            val result = service.importBackup(preview, false)
            assertEquals(0, result.insertedTasks); assertEquals(0, result.insertedEvents)
            assertEquals("local", target.tasks().backupSnapshot().single { it.syncId == "changed" }.title)
            assertEquals("local", target.events().backupSnapshot().single { it.syncId == "changed-event" }.title)
        } finally { source.close(); target.close(); f.close() }
    }

    @Test fun exportImportPreservesLongDisplayNamesAndEventLocations() = runBlocking {
        val f = fixture(); val source = db(); val target = db()
        try {
            val displayName = "N".repeat(121)
            val location = "L".repeat(501)
            source.tasks().insert(task("task"))
            source.events().insert(event("long-location").copy(location = location))
            val sourcePrefs = f.settings("long-text-source")
            sourcePrefs.setDisplayName(displayName)
            val targetPrefs = f.settings("long-text-target")
            val service = LocalBackupService(target, targetPrefs, clock) {}
            val preview = service.preview(ByteArrayInputStream(export(source, sourcePrefs)))
            service.importBackup(preview, true)
            assertEquals(displayName, targetPrefs.settings.first().displayName)
            assertEquals(location, target.events().backupSnapshot().single().location)
        } finally { source.close(); target.close(); f.close() }
    }

    @Test fun occupiedParentSkipsIncomingChildAndItsDescendantInPreview() = runBlocking {
        val f = fixture(); val source = db(); val target = db()
        try {
            val sourceParent = source.tasks().insert(task("parent", "incoming parent"))
            val targetParent = target.tasks().insert(task("parent", "local parent"))
            target.tasks().insert(task("local-child", parent = targetParent))
            val child = source.tasks().insert(task("child", parent = sourceParent))
            source.tasks().insert(task("grandchild", parent = child))
            val service = LocalBackupService(target, f.settings("owner"), clock) {}
            val preview = service.preview(ByteArrayInputStream(export(source, f.settings("owner-source"))))
            assertEquals(0, preview.newTasks)
            assertEquals(3, preview.conflicts)
            service.importBackup(preview, false)
            assertEquals(setOf("parent", "local-child"), target.tasks().backupSnapshot().map { it.syncId }.toSet())
        } finally { source.close(); target.close(); f.close() }
    }

    @Test fun importedAncestryAndPinnedFocusUseRemappedLocalIds() = runBlocking {
        val f = fixture(); val source = db(); val target = db()
        try {
            target.tasks().insert(task("local-first"))
            target.tasks().insert(task("local-second")); target.tasks().insert(task("local-third"))
            val sourceParent = source.tasks().insert(task("parent"))
            source.tasks().insert(task("child", parent = sourceParent))
            val sourcePrefs = f.settings("mapped-source")
            sourcePrefs.setPinnedFocus(sourceParent, java.time.LocalDate.parse("2026-10-10"))
            val targetPrefs = f.settings("mapped-target")
            val service = LocalBackupService(target, targetPrefs, clock) {}
            service.importBackup(service.preview(ByteArrayInputStream(export(source, sourcePrefs))), true)
            val rows = target.tasks().backupSnapshot().associateBy { it.syncId }
            assertNotEquals(sourceParent, rows.getValue("parent").id)
            assertEquals(rows.getValue("parent").id, rows.getValue("child").generatedFromTaskId)
            val values = targetPrefs.backupValues(rows.values.associate { it.id to it.syncId })
            assertEquals("parent", values.pinnedFocusSyncId)
        } finally { source.close(); target.close(); f.close() }
    }

    @Test fun stalePreviewRejectsBeforeAnyImportWritesOrReconciliation() = runBlocking {
        val f = fixture(); val source = db(); val target = db()
        try {
            source.tasks().insert(task("incoming")); val bytes = export(source, f.settings("stale-source"))
            var reconciles = 0; val service = LocalBackupService(target, f.settings("stale-target"), clock) { reconciles++ }
            val preview = service.preview(ByteArrayInputStream(bytes))
            target.tasks().insert(task("new-local"))
            val failure = runCatching { service.importBackup(preview, false) }.exceptionOrNull() as BackupException
            assertEquals(BackupFailure.STALE_PREVIEW, failure.reason)
            assertEquals(setOf("new-local"), target.tasks().backupSnapshot().map { it.syncId }.toSet())
            assertEquals(0, reconciles)
        } finally { source.close(); target.close(); f.close() }
    }

    @Test fun semanticallyInvalidBackupIsRejectedBeforeWrites() = runBlocking {
        val f = fixture(); val source = db(); val target = db()
        try {
            source.tasks().insert(task("incoming"))
            val sourcePrefs = f.settings("invalid-source")
            val valid = export(source, sourcePrefs).toString(Charsets.UTF_8)
            val noDeadlineSource = db()
            noDeadlineSource.tasks().insert(task("no-deadline").copy(deadlineAt = null, reminderMinutesBefore = null))
            val noDeadline = export(noDeadlineSource, sourcePrefs).toString(Charsets.UTF_8)
            noDeadlineSource.close()
            val malformed = listOf(
                valid.replace("\"updatedAt\":20", "\"updatedAt\":9"),
                valid.replace("\"status\":\"NOT_STARTED\"", "\"status\":\"DONE\""),
                noDeadline.replace("\"hasDeadlineTime\":false", "\"hasDeadlineTime\":true"),
            )
            var reconciles = 0
            val service = LocalBackupService(target, f.settings("invalid-target"), clock) { reconciles++ }
            malformed.forEach { text ->
                val failure = runCatching { service.preview(ByteArrayInputStream(text.toByteArray())) }.exceptionOrNull() as BackupException
                assertEquals(BackupFailure.INVALID, failure.reason)
            }
            assertTrue(target.tasks().backupSnapshot().isEmpty())
            assertEquals(0, reconciles)
        } finally { source.close(); target.close(); f.close() }
    }

    @Test fun malformedStructureRejectsMissingFieldsDuplicateKeysAndSyncIdsCyclesAndBadNumbers() = runBlocking {
        val f = fixture(); val source = db(); val target = db()
        try {
            val parentId = source.tasks().insert(task("parent"))
            source.tasks().insert(task("child", parent = parentId))
            source.events().insert(event("event"))
            val valid = export(source, f.settings("structure-source")).toString(Charsets.UTF_8)
            val taskOnly = JSONObject(valid).apply { getJSONArray("tasks").getJSONObject(0).remove("notes") }.toString()
            val duplicate = JSONObject(valid).apply {
                val tasks = getJSONArray("tasks")
                tasks.put(JSONObject(tasks.getJSONObject(0).toString()))
            }.toString()
            val cycle = JSONObject(valid).apply {
                getJSONArray("tasks").getJSONObject(0).put("generatedFromSyncId", "child")
            }.toString()
            val cases = listOf(
                taskOnly,
                valid.replace("\"formatVersion\":4", "\"formatVersion\":4,\"formatVersion\":4"),
                JSONObject(valid).apply { getJSONObject("settings").put("hints", "wrong") }.toString(),
                valid.replace("\"createdAt\":10", "\"createdAt\":9223372036854775808"),
                duplicate,
                cycle,
            )
            val service = LocalBackupService(target, f.settings("structure-target"), clock) {}
            cases.forEach { text ->
                val failure = runCatching { service.preview(ByteArrayInputStream(text.toByteArray())) }.exceptionOrNull() as BackupException
                assertEquals(BackupFailure.INVALID, failure.reason)
            }
            val invalidUtf8 = ByteArrayInputStream(byteArrayOf(0x7b, 0x22, 0x78, 0x22, 0x3a, 0xc3.toByte(), 0x7d))
            val failure = runCatching { service.preview(invalidUtf8) }.exceptionOrNull() as BackupException
            assertEquals(BackupFailure.INVALID, failure.reason)
            assertTrue(target.tasks().backupSnapshot().isEmpty()); assertEquals(0, target.events().backupSnapshot().size)
        } finally { source.close(); target.close(); f.close() }
    }

    @Test fun eventInsertFailureRollsBackTasksAndSkipsPostCommitEffects() = runBlocking {
        val f = fixture(); val source = db()
        val target = db("CREATE TRIGGER reject_backup_event BEFORE INSERT ON events BEGIN SELECT RAISE(ABORT, 'fixture failure'); END")
        try {
            source.tasks().insert(task("incoming")); source.events().insert(event("incoming-event"))
            val prefs = f.settings("rollback")
            prefs.setDisplayName("Keep me")
            val before = prefs.settings.first()
            var reconciles = 0; val service = LocalBackupService(target, prefs, clock) { reconciles++ }
            val preview = service.preview(ByteArrayInputStream(export(source, f.settings("rollback-source"))))
            assertNotNull(runCatching { service.importBackup(preview, true) }.exceptionOrNull())
            assertTrue(target.tasks().backupSnapshot().isEmpty()); assertTrue(target.events().backupSnapshot().isEmpty())
            assertEquals(before, prefs.settings.first())
            assertEquals(0, reconciles)
        } finally { source.close(); target.close(); f.close() }
    }

    @Test fun restorePreferenceWriteFailureAndReminderFailureAreReportedAfterCommit() = runBlocking {
        val f = fixture(); val source = db(); val target = db()
        try {
            source.tasks().insert(task("incoming")); val bytes = export(source, f.settings("post-source"))
            var reconciles = 0
            val service = LocalBackupService(target, failingSettings(), clock) { reconciles++; error("reconcile failed") }
            val result = service.importBackup(service.preview(ByteArrayInputStream(bytes)), true)
            assertEquals(1, result.insertedTasks); assertTrue(result.preferencesFailed); assertTrue(result.remindersFailed)
            assertEquals(1, reconciles); assertEquals(listOf("incoming"), target.tasks().backupSnapshot().map { it.syncId })
        } finally { source.close(); target.close(); f.close() }
    }

    @Test fun optOutPreservesSettingsAndImportedDoneOrDeletedTasksHaveNoReminderToken() = runBlocking {
        val f = fixture(); val source = db(); val target = db()
        try {
            source.tasks().insert(task("done", status = "DONE")); source.tasks().insert(task("deleted", deleted = 20).copy(updatedAt = 20))
            val sourcePrefs = f.settings("opt-source")
            sourcePrefs.setDisplayName("Imported")
            val targetPrefs = f.settings("opt-target")
            targetPrefs.setDisplayName("Local")
            val before = targetPrefs.settings.first()
            var reconciledCommittedRows = false
            val service = LocalBackupService(target, targetPrefs, clock) {
                val committed = target.tasks().backupSnapshot()
                reconciledCommittedRows = committed.size == 2 && committed.all { taskReminderToken(it.toDomain()) == null }
            }
            service.importBackup(service.preview(ByteArrayInputStream(export(source, sourcePrefs))), false)
            assertEquals(before, targetPrefs.settings.first())
            val tasks = target.tasks().backupSnapshot()
            assertEquals(2, tasks.size)
            assertTrue(tasks.all { taskReminderToken(it.toDomain()) == null })
            assertTrue(reconciledCommittedRows)
        } finally { source.close(); target.close(); f.close() }
    }

    @Test fun cancellationDuringSettingsRestoreStillReconcilesCommittedImport() = runBlocking {
        val f = fixture(); val source = db(); val target = db()
        try {
            source.tasks().insert(task("incoming"))
            val bytes = export(source, f.settings("cancel-tail-source"))
            var caller: Job? = null
            var reconciledCommittedRows = false
            val service = LocalBackupService(target, updatingSettings { caller?.cancel() }, clock) {
                reconciledCommittedRows = target.tasks().backupSnapshot().map { it.syncId } == listOf("incoming")
            }
            val preview = service.preview(ByteArrayInputStream(bytes))
            val importJob = launch {
                caller = kotlinx.coroutines.currentCoroutineContext()[Job]
                service.importBackup(preview, true)
            }
            importJob.join()
            assertTrue(importJob.isCancelled)
            assertEquals(listOf("incoming"), target.tasks().backupSnapshot().map { it.syncId })
            assertTrue(reconciledCommittedRows)
        } finally { source.close(); target.close(); f.close() }
    }

    @Test fun cancelledCallerBeforeImportEntryWritesNothing() = runBlocking {
        val f = fixture(); val source = db(); val target = db()
        try {
            source.tasks().insert(task("incoming"))
            val service = LocalBackupService(target, f.settings("cancel-before"), clock) {}
            val preview = service.preview(ByteArrayInputStream(export(source, f.settings("cancel-before-source"))))
            val cancelledJob = Job().also { it.cancel() }
            var failure: Throwable? = null
            val operation: suspend () -> BackupImportResult = { service.importBackup(preview, false) }
            operation.startCoroutine(object : Continuation<BackupImportResult> {
                override val context: CoroutineContext = cancelledJob
                override fun resumeWith(result: Result<BackupImportResult>) { failure = result.exceptionOrNull() }
            })
            assertTrue(failure is CancellationException)
            assertTrue(target.tasks().backupSnapshot().isEmpty())
        } finally { source.close(); target.close(); f.close() }
    }
}
