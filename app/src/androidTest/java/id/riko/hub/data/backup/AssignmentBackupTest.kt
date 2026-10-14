package id.riko.hub.data.backup

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import id.riko.hub.core.time.Clock
import id.riko.hub.data.local.RikoDatabase
import id.riko.hub.data.local.entity.AcademicAssignmentEntity
import id.riko.hub.data.local.entity.AcademicDeadlineChangeEntity
import id.riko.hub.data.local.entity.TaskEntity
import id.riko.hub.data.prefs.SettingsDataStore
import id.riko.hub.data.repository.AssignmentRepository
import id.riko.hub.data.repository.CourseRepository
import id.riko.hub.data.repository.TaskRepository
import id.riko.hub.domain.model.AcademicState
import id.riko.hub.domain.model.Course
import id.riko.hub.domain.model.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.time.ZoneId

class AssignmentBackupTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val scopes = mutableListOf<CoroutineScope>()
    private val files = mutableListOf<File>()
    private var now = 100L
    private val clock = object : Clock {
        override fun now() = now
        override fun zone() = ZoneId.of("UTC")
    }

    private fun settings(name: String): SettingsDataStore {
        val file = context.cacheDir.resolve("$name-${System.nanoTime()}.preferences_pb")
        files += file
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scopes += scope
        val store: DataStore<Preferences> = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        return SettingsDataStore(store)
    }

    private fun db() = Room.inMemoryDatabaseBuilder(context, RikoDatabase::class.java).build()
    private fun service(db: RikoDatabase, name: String) = LocalBackupService(db, settings(name), clock) {}
    private suspend fun export(db: RikoDatabase, name: String) = service(db, name).export()

    @Test fun round_trip_keeps_academic_lifecycle_course_submission_and_deadline_history() = runBlocking {
        val source = db(); val target = db()
        try {
            val tasks = TaskRepository(source, clock)
            val courses = CourseRepository(source, clock)
            val assignments = AssignmentRepository(source, clock)
            val taskId = tasks.upsert(Task(syncId = "essay-task", title = "Essay", deadlineAt = 500))
            val courseId = courses.upsert(Course(syncId = "writing-course", title = "Writing"))
            assignments.save(taskId, courseId, AcademicState.PLANNED, null)
            now = 200
            tasks.upsert(requireNotNull(tasks.observeById(taskId).first()).copy(deadlineAt = 600, hasDeadlineTime = true))
            now = 300
            assignments.save(taskId, courseId, AcademicState.SUBMITTED, "https://example.test/essay")
            now = 400
            assignments.save(taskId, courseId, AcademicState.IN_PROGRESS, null)

            val backup = export(source, "assignment-roundtrip-source")
            val importer = service(target, "assignment-roundtrip-target")
            val preview = importer.preview(ByteArrayInputStream(backup))
            assertEquals(1, preview.totalAssignments)
            assertEquals(1, preview.newAssignments)
            assertEquals(1, preview.totalDeadlineChanges)
            assertEquals(1, preview.newDeadlineChanges)
            val result = importer.importBackup(preview, false)
            assertEquals(1, result.insertedAssignments)
            assertEquals(1, result.insertedDeadlineChanges)

            val task = target.tasks().backupSnapshot().single()
            val assignment = requireNotNull(AssignmentRepository(target, clock).observeByTask(task.id).first())
            assertEquals("essay-task", task.syncId)
            assertEquals("writing-course", target.courses().backupSnapshot().single().course.syncId)
            assertEquals(AcademicState.IN_PROGRESS, assignment.state)
            assertEquals(300L, assignment.submittedAt)
            assertEquals("https://example.test/essay", assignment.submissionReference)
            assertEquals(500L, assignment.originalDeadlineAt)
            assertFalse(assignment.originalHasDeadlineTime)
            assertEquals(1, assignment.deadlineChanges.size)
            assertEquals(500L, assignment.deadlineChanges.single().previousDeadlineAt)
            assertEquals(600L, assignment.deadlineChanges.single().newDeadlineAt)
            assertTrue(assignment.deadlineChanges.single().newHasDeadlineTime)
            assertEquals("NOT_STARTED", task.status)
        } finally { source.close(); target.close(); closeFiles() }
    }

    @Test fun legacy_formats_preserve_local_assignments_and_task_mismatch_blocks_incoming_assignment() = runBlocking {
        val source = db(); val legacyTarget = db(); val mismatchTarget = db()
        try {
            val taskRow = task("legacy-task", "Same task")
            source.tasks().insert(taskRow)
            source.assignments().insert(assignment("incoming-assignment", 1, "PLANNED"))
            val sourceBytes = export(source, "assignment-legacy-source").toString(Charsets.UTF_8)

            legacyTarget.tasks().insert(taskRow)
            legacyTarget.assignments().insert(assignment("local-assignment", 1, "REVIEW"))
            val importer = service(legacyTarget, "assignment-legacy-target")
            listOf(2, 1).forEach { version ->
                val legacy = JSONObject(sourceBytes).apply {
                    put("formatVersion", version)
                    remove("spaceNotes")
                    remove("assignments")
                    if (version == 1) remove("courses")
                }.toString().toByteArray()
                importer.importBackup(importer.preview(ByteArrayInputStream(legacy)), false)
            }
            assertEquals("REVIEW", legacyTarget.assignments().backupSnapshot().single().state)

            mismatchTarget.tasks().insert(taskRow.copy(title = "Different local task"))
            val mismatchImporter = service(mismatchTarget, "assignment-mismatch")
            val mismatchPreview = mismatchImporter.preview(ByteArrayInputStream(sourceBytes.toByteArray()))
            assertEquals(0, mismatchPreview.newAssignments)
            assertTrue(mismatchPreview.conflicts > 0)
            mismatchImporter.importBackup(mismatchPreview, false)
            assertTrue(mismatchTarget.assignments().backupSnapshot().isEmpty())
        } finally { source.close(); legacyTarget.close(); mismatchTarget.close(); closeFiles() }
    }

    @Test fun local_assignment_owner_wins_and_lifecycle_changes_stale_a_preview() = runBlocking {
        val source = db(); val target = db(); val conflictTarget = db(); val ownerTarget = db()
        try {
            val taskRow = task("stable-task", "Task")
            source.tasks().insert(taskRow)
            source.assignments().insert(assignment("same-assignment", 1, "PLANNED"))
            target.tasks().insert(taskRow)
            target.assignments().insert(assignment("same-assignment", 1, "PLANNED"))
            val importer = service(target, "assignment-stale")
            val preview = importer.preview(ByteArrayInputStream(export(source, "assignment-stale-source")))
            assertTrue(preview.duplicates >= 2)
            val local = target.assignments().backupSnapshot().single()
            target.assignments().update(local.copy(state = "COMPLETED", updatedAt = 120))
            assertEquals(BackupFailure.STALE_PREVIEW,
                (runCatching { importer.importBackup(preview, false) }.exceptionOrNull() as BackupException).reason)
            assertEquals("COMPLETED", target.assignments().backupSnapshot().single().state)
            val historyPreview = importer.preview(ByteArrayInputStream(export(source, "assignment-stale-history-source")))
            val current = target.assignments().backupSnapshot().single()
            target.assignments().insertDeadlineChange(AcademicDeadlineChangeEntity(
                syncId = "local-history", assignmentId = current.id, previousDeadlineAt = null,
                previousHasDeadlineTime = false, newDeadlineAt = 600, newHasDeadlineTime = false, changedAt = 130,
            ))
            target.assignments().update(current.copy(updatedAt = 130))
            assertEquals(BackupFailure.STALE_PREVIEW,
                (runCatching { importer.importBackup(historyPreview, false) }.exceptionOrNull() as BackupException).reason)
            conflictTarget.tasks().insert(taskRow)
            conflictTarget.assignments().insert(assignment("same-assignment", 1, "COMPLETED", updated = 30))
            val conflictImporter = service(conflictTarget, "assignment-local-wins")
            val conflictPreview = conflictImporter.preview(ByteArrayInputStream(export(source, "assignment-local-wins-source")))
            assertEquals(0, conflictPreview.newAssignments)
            assertTrue(conflictPreview.conflicts > 0)
            conflictImporter.importBackup(conflictPreview, false)
            assertEquals("COMPLETED", conflictTarget.assignments().backupSnapshot().single().state)

            ownerTarget.tasks().insert(taskRow)
            ownerTarget.assignments().insert(assignment("other-local-assignment", 1, "REVIEW"))
            val ownerImporter = service(ownerTarget, "assignment-owner-wins")
            val ownerPreview = ownerImporter.preview(ByteArrayInputStream(export(source, "assignment-owner-wins-source")))
            assertEquals(0, ownerPreview.newAssignments)
            assertTrue(ownerPreview.conflicts > 0)
            ownerImporter.importBackup(ownerPreview, false)
            assertEquals("other-local-assignment", ownerTarget.assignments().backupSnapshot().single().syncId)
        } finally { source.close(); target.close(); conflictTarget.close(); ownerTarget.close(); closeFiles() }
    }

    @Test fun invalid_reopened_submission_time_and_broken_history_are_rejected_before_writes() = runBlocking {
        val source = db(); val target = db()
        try {
            source.tasks().insert(task("history-task", "Task", deadline = 600, timed = true))
            val assignmentId = source.assignments().insert(assignment("history-assignment", 1, "IN_PROGRESS", submittedAt = 20, original = 500, updated = 30))
            source.assignments().insertDeadlineChange(AcademicDeadlineChangeEntity(
                syncId = "change-1", assignmentId = assignmentId, previousDeadlineAt = 500,
                previousHasDeadlineTime = false, newDeadlineAt = 600, newHasDeadlineTime = true, changedAt = 20,
            ))
            val valid = export(source, "assignment-invalid-source").toString(Charsets.UTF_8)
            val badSubmittedTime = JSONObject(valid).apply {
                getJSONArray("assignments").getJSONObject(0).put("submittedAt", 31)
            }.toString()
            val earlySubmittedTime = JSONObject(valid).apply {
                getJSONArray("assignments").getJSONObject(0).put("submittedAt", 9)
            }.toString()
            val badHistory = JSONObject(valid).apply {
                getJSONArray("assignments").getJSONObject(0).getJSONArray("deadlineChanges")
                    .getJSONObject(0).put("previousDeadlineAt", 400)
            }.toString()
            val missingTask = JSONObject(valid).apply {
                getJSONArray("assignments").getJSONObject(0).put("taskSyncId", "missing-task")
            }.toString()
            val importer = service(target, "assignment-invalid-target")
            assertEquals(1, importer.preview(ByteArrayInputStream(valid.toByteArray())).totalAssignments)
            listOf(badSubmittedTime, earlySubmittedTime, badHistory, missingTask).forEach { text ->
                val failure = runCatching { importer.preview(ByteArrayInputStream(text.toByteArray())) }.exceptionOrNull() as BackupException
                assertEquals(BackupFailure.INVALID, failure.reason)
            }
            assertTrue(target.tasks().backupSnapshot().isEmpty())
            assertTrue(target.assignments().backupSnapshot().isEmpty())
        } finally { source.close(); target.close(); closeFiles() }
    }

    private fun task(syncId: String, title: String, deadline: Long? = null, timed: Boolean = false) = TaskEntity(
        syncId = syncId, title = title, notes = null, category = "PERSONAL", priority = "MEDIUM", status = "NOT_STARTED",
        deadlineAt = deadline, hasDeadlineTime = timed, createdAt = 10, updatedAt = 10, completedAt = null, deletedAt = null,
        estimatedMinutes = null, recurrence = "NONE", checklistJson = "[]", reminderMinutesBefore = null,
        generatedFromTaskId = null, generationPristine = false, recurrenceAnchorAt = null,
    )

    private fun assignment(syncId: String, taskId: Long, state: String, submittedAt: Long? = null, original: Long? = null, updated: Long = 20) = AcademicAssignmentEntity(
        syncId = syncId, taskId = taskId, courseId = null, state = state, submittedAt = submittedAt,
        submissionReference = null, originalDeadlineAt = original, originalHasDeadlineTime = false, createdAt = 10, updatedAt = updated,
    )

    private suspend fun closeFiles() {
        scopes.forEach { it.cancel() }
        scopes.mapNotNull { it.coroutineContext[Job] }.joinAll()
        files.forEach(File::delete)
    }
}
