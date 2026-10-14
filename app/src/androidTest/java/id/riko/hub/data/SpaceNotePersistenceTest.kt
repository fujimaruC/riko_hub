package id.riko.hub.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import id.riko.hub.core.time.Clock
import id.riko.hub.data.backup.LocalBackupService
import id.riko.hub.data.local.Migrations
import id.riko.hub.data.local.RikoDatabase
import id.riko.hub.data.prefs.SettingsDataStore
import id.riko.hub.data.repository.SpaceNoteRepository
import id.riko.hub.data.repository.TaskRepository
import id.riko.hub.domain.model.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.time.ZoneId

class SpaceNotePersistenceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val clock = object : Clock { override fun now() = 1000L; override fun zone() = ZoneId.of("UTC") }

    @Test fun journal_edits_latest_day_row_and_keeps_conflicting_history() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, RikoDatabase::class.java).build()
        try {
            val repo = SpaceNoteRepository(db, clock)
            val taskId = TaskRepository(db, clock).upsert(Task(title = "Project task"))
            val taskSyncId = requireNotNull(db.tasks().adminFindForWrite(taskId)).syncId
            assertEquals(taskSyncId, repo.saveIdea(taskSyncId, "linked idea").projectTaskSyncId)
            assertTrue(runCatching { repo.saveIdea("missing", "bad link") }.isFailure)
            val day = LocalDate.parse("2026-10-13")
            val original = repo.saveToday(day, " first ")
            assertEquals("first", original.body)
            assertEquals(original.syncId, repo.saveToday(day, "edited").syncId)
            db.spaceNotes().insert(id.riko.hub.data.local.entity.SpaceNoteEntity(
                syncId = "imported-conflict", kind = "JOURNAL", body = "older content", journalDate = day.toString(),
                projectTaskSyncId = null, createdAt = 1, updatedAt = 2, deletedAt = null,
            ))
            assertEquals(2, repo.observeJournalHistory().first().size)
            assertEquals("edited", repo.observeJournalHistory().first().first().body)
            assertTrue(runCatching { repo.saveToday(day, "  ") }.isFailure)
            assertTrue(runCatching { repo.update(original.syncId, "x".repeat(10_001)) }.isFailure)
        } finally { db.close() }
    }

    @Test fun migration_4_to_5_adds_notes_without_changing_existing_rows() {
        val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), RikoDatabase::class.java.canonicalName!!,
            FrameworkSQLiteOpenHelperFactory())
        val name = "space-note-migration.db"
        context.deleteDatabase(name)
        val old = helper.createDatabase(name, 4)
        old.execSQL("INSERT INTO tasks(syncId,title,category,priority,status,hasDeadlineTime,createdAt,updatedAt,recurrence,checklistJson,generationPristine) VALUES('kept','Keep','PERSONAL','MEDIUM','NOT_STARTED',0,1,1,'NONE','[]',0)")
        old.execSQL("INSERT INTO courses(id,syncId,title,sortOrder,createdAt,updatedAt) VALUES(2,'course-kept','Course kept',0,1,1)")
        old.close()
        val migrated = helper.runMigrationsAndValidate(name, 5, true, Migrations.MIGRATION_4_5)
        migrated.query("SELECT syncId FROM tasks WHERE syncId='kept'").use { assertTrue(it.moveToFirst()) }
        migrated.query("SELECT syncId FROM courses WHERE syncId='course-kept'").use { assertTrue(it.moveToFirst()) }
        migrated.query("SELECT COUNT(*) FROM space_notes").use { assertTrue(it.moveToFirst()); assertEquals(0, it.getInt(0)) }
        migrated.close()
        context.deleteDatabase(name)
    }

    @Test fun backup_round_trip_is_idempotent_for_notes_and_keeps_same_day_conflicts() = runBlocking {
        val source = Room.inMemoryDatabaseBuilder(context, RikoDatabase::class.java).build()
        val target = Room.inMemoryDatabaseBuilder(context, RikoDatabase::class.java).build()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val settings = SettingsDataStore(PreferenceDataStoreFactory.create(scope = scope, produceFile = { context.cacheDir.resolve("space-notes-${System.nanoTime()}.preferences_pb") }))
        try {
            val repo = SpaceNoteRepository(source, clock)
            val day = LocalDate.parse("2026-10-13")
            repo.saveToday(day, "journal from source")
            source.spaceNotes().insert(id.riko.hub.data.local.entity.SpaceNoteEntity(
                syncId = "same-day-import", kind = "JOURNAL", body = "different same-day content", journalDate = day.toString(),
                projectTaskSyncId = null, createdAt = 900, updatedAt = 900, deletedAt = null,
            ))
            repo.saveIdea(body = "a project idea")
            val service = LocalBackupService(source, settings, clock) {}
            val otherSettings = SettingsDataStore(PreferenceDataStoreFactory.create(scope = scope, produceFile = { context.cacheDir.resolve("space-notes-target-${System.nanoTime()}.preferences_pb") }))
            val targetService = LocalBackupService(target, otherSettings, clock) {}
            val bytes = service.export()
            val preview = targetService.preview(ByteArrayInputStream(bytes))
            assertEquals(3, preview.newNotes)
            assertEquals(3, targetService.importBackup(preview, false).insertedNotes)
            val duplicate = targetService.preview(ByteArrayInputStream(bytes))
            assertEquals(3, duplicate.duplicates)
            assertEquals(0, targetService.importBackup(duplicate, false).insertedNotes)
            assertEquals(2, target.spaceNotes().observeJournals().first().size)
            assertEquals(setOf("journal from source", "different same-day content"), target.spaceNotes().observeJournals().first().map { it.body }.toSet())
            assertEquals(1, target.spaceNotes().observeIdeas().first().size)
        } finally { source.close(); target.close(); scope.cancel() }
    }

    @Test fun backup_rejects_invalid_note_body_kind_date_and_timestamps() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, RikoDatabase::class.java).build()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val settings = SettingsDataStore(PreferenceDataStoreFactory.create(scope = scope, produceFile = { context.cacheDir.resolve("space-invalid-${System.nanoTime()}.preferences_pb") }))
        try {
            SpaceNoteRepository(db, clock).saveToday(LocalDate.parse("2026-10-13"), "valid note")
            val service = LocalBackupService(db, settings, clock) {}
            val exported = JSONObject(service.export().toString(Charsets.UTF_8))
            val badNotes = listOf(
                JSONObject(exported.toString()).apply { getJSONArray("spaceNotes").getJSONObject(0).put("body", "  ") },
                JSONObject(exported.toString()).apply { getJSONArray("spaceNotes").getJSONObject(0).put("kind", "OTHER") },
                JSONObject(exported.toString()).apply { getJSONArray("spaceNotes").getJSONObject(0).put("journalDate", "2026-02-30") },
                JSONObject(exported.toString()).apply { getJSONArray("spaceNotes").getJSONObject(0).put("journalDate", "2026-10-13 ") },
                JSONObject(exported.toString()).apply { getJSONArray("spaceNotes").getJSONObject(0).put("deletedAt", 999) },
            )
            badNotes.forEach { invalid ->
                val error = runCatching { service.preview(ByteArrayInputStream(invalid.toString().toByteArray())) }.exceptionOrNull() as id.riko.hub.data.backup.BackupException
                assertEquals(id.riko.hub.data.backup.BackupFailure.INVALID, error.reason)
            }
        } finally { db.close(); scope.cancel() }
    }
}
