package id.riko.hub.data.backup

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import id.riko.hub.core.time.Clock
import id.riko.hub.data.local.RikoDatabase
import id.riko.hub.data.prefs.SettingsDataStore
import id.riko.hub.data.repository.CourseRepository
import id.riko.hub.domain.model.Course
import id.riko.hub.domain.model.CourseWeeklySlot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.time.ZoneId

class CourseBackupTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val clock = object : Clock {
        override fun now() = 1_000L
        override fun zone() = ZoneId.of("UTC")
    }

    private data class Fixture(val scopes: MutableList<CoroutineScope>, val files: MutableList<File>) {
        fun settings(name: String) = SettingsDataStore(PreferenceDataStoreFactory.create(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO).also(scopes::add),
            produceFile = { contextFile(name).also(files::add) },
        ))
        private fun contextFile(name: String) = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
            .resolve("$name-${System.nanoTime()}.preferences_pb")
        suspend fun close() {
            scopes.forEach { it.cancel() }
            scopes.mapNotNull { it.coroutineContext[Job] }.joinAll()
            files.forEach(File::delete)
        }
    }

    private fun fixture() = Fixture(mutableListOf(), mutableListOf())
    private fun database() = Room.inMemoryDatabaseBuilder(context, RikoDatabase::class.java).build()
    private fun service(db: RikoDatabase, settings: SettingsDataStore) = LocalBackupService(db, settings, clock) {}
    private suspend fun export(db: RikoDatabase, settings: SettingsDataStore) = service(db, settings).export()

    @Test fun archived_order_and_weekly_slots_round_trip_and_repeat_as_duplicates() = runBlocking {
        val f = fixture(); val source = database(); val target = database()
        try {
            val courses = CourseRepository(source, clock)
            courses.upsert(Course(syncId = "active", title = "Biology", code = "BIO", color = "GREEN", sortOrder = 0,
                weeklySlots = listOf(CourseWeeklySlot(3, 600, 660, "Lab"), CourseWeeklySlot(1, 480, 540))))
            val archived = courses.upsert(Course(syncId = "archived", title = "History", academicPeriod = "2026", sortOrder = 1,
                weeklySlots = listOf(CourseWeeklySlot(5, 840, 900, null))))
            courses.setArchived(archived, true)
            val targetService = service(target, f.settings("round-target"))
            val bytes = export(source, f.settings("round-source"))
            val preview = targetService.preview(ByteArrayInputStream(bytes))
            assertEquals(2, preview.totalCourses)
            assertEquals(2, preview.newCourses)
            assertEquals(3, preview.totalWeeklySlots)
            assertEquals(3, preview.newWeeklySlots)
            val result = targetService.importBackup(preview, false)
            assertEquals(2, result.insertedCourses)
            assertEquals(3, result.insertedWeeklySlots)
            val rows = target.courses().backupSnapshot().associateBy { it.course.syncId }
            assertEquals(listOf("active", "archived"), target.courses().observeAll().first().map { it.course.syncId })
            assertEquals(listOf(1, 3), rows.getValue("active").weeklySlots.map { it.dayOfWeek })
            assertEquals("Lab", rows.getValue("active").weeklySlots.last().location)
            assertNotNull(rows.getValue("archived").course.archivedAt)
            assertEquals(0L, rows.getValue("active").course.sortOrder)
            assertEquals(1L, rows.getValue("archived").course.sortOrder)
            val repeated = targetService.preview(ByteArrayInputStream(bytes))
            assertEquals(2, repeated.duplicates)
            assertEquals(0, repeated.newCourses)
            assertEquals(0, targetService.importBackup(repeated, false).insertedCourses)
            assertEquals(2, target.courses().backupSnapshot().size)
        } finally { source.close(); target.close(); f.close() }
    }

    @Test fun conflicts_keep_the_entire_local_course_and_new_courses_include_all_slots() = runBlocking {
        val f = fixture(); val source = database(); val target = database()
        try {
            CourseRepository(source, clock).apply {
                upsert(Course(syncId = "shared", title = "Incoming", weeklySlots = listOf(CourseWeeklySlot(1, 100, 200))))
                upsert(Course(syncId = "new", title = "New", weeklySlots = listOf(CourseWeeklySlot(2, 300, 400), CourseWeeklySlot(4, 500, 600))))
            }
            CourseRepository(target, clock).upsert(Course(syncId = "shared", title = "Local", weeklySlots = listOf(CourseWeeklySlot(7, 700, 800, "Local room"))))
            val targetService = service(target, f.settings("merge-target"))
            val preview = targetService.preview(ByteArrayInputStream(export(source, f.settings("merge-source"))))
            assertEquals(1, preview.conflicts)
            assertEquals(1, preview.newCourses)
            assertEquals(2, preview.newWeeklySlots)
            val result = targetService.importBackup(preview, false)
            assertEquals(1, result.insertedCourses)
            assertEquals(2, result.insertedWeeklySlots)
            val rows = target.courses().backupSnapshot().associateBy { it.course.syncId }
            assertEquals("Local", rows.getValue("shared").course.title)
            assertEquals("Local room", rows.getValue("shared").weeklySlots.single().location)
            assertEquals(2, rows.getValue("new").weeklySlots.size)
        } finally { source.close(); target.close(); f.close() }
    }

    @Test fun format_one_import_preserves_existing_courses() = runBlocking {
        val f = fixture(); val source = database(); val target = database()
        try {
            CourseRepository(source, clock).upsert(Course(syncId = "incoming", title = "Imported"))
            CourseRepository(target, clock).upsert(Course(syncId = "local", title = "Keep me"))
            val current = export(source, f.settings("v1-source")).toString(Charsets.UTF_8)
            val legacy = JSONObject(current)
                .put("formatVersion", 1).apply { remove("courses"); remove("assignments"); remove("spaceNotes") }.toString().toByteArray()
            val targetService = service(target, f.settings("v1-target"))
            val preview = targetService.preview(ByteArrayInputStream(legacy))
            assertEquals(0, preview.totalCourses)
            assertEquals(0, targetService.importBackup(preview, false).insertedCourses)
            assertEquals(listOf("local"), target.courses().backupSnapshot().map { it.course.syncId })

            val version2 = JSONObject(current).apply { put("formatVersion", 2); remove("assignments"); remove("spaceNotes") }.toString().toByteArray()
            val version2Preview = targetService.preview(ByteArrayInputStream(version2))
            assertEquals(1, version2Preview.totalCourses)
            assertEquals(1, targetService.importBackup(version2Preview, false).insertedCourses)
            assertEquals(setOf("incoming", "local"), target.courses().backupSnapshot().map { it.course.syncId }.toSet())
        } finally { source.close(); target.close(); f.close() }
    }

    @Test fun schedule_or_order_change_after_preview_rejects_import() = runBlocking {
        val f = fixture(); val source = database(); val target = database()
        try {
            val repo = CourseRepository(target, clock)
            val first = repo.upsert(Course(syncId = "first", title = "First", weeklySlots = listOf(CourseWeeklySlot(1, 100, 200))))
            val second = repo.upsert(Course(syncId = "second", title = "Second"))
            val targetService = service(target, f.settings("stale-target"))
            val bytes = export(source, f.settings("stale-source"))
            var preview = targetService.preview(ByteArrayInputStream(bytes))
            repo.upsert(requireNotNull(repo.observeById(first).first()).copy(weeklySlots = listOf(CourseWeeklySlot(1, 200, 300))))
            assertEquals(BackupFailure.STALE_PREVIEW, runCatching { targetService.importBackup(preview, false) }.exceptionOrNull().let { (it as BackupException).reason })
            preview = targetService.preview(ByteArrayInputStream(bytes))
            repo.move(second, -1)
            assertEquals(BackupFailure.STALE_PREVIEW, runCatching { targetService.importBackup(preview, false) }.exceptionOrNull().let { (it as BackupException).reason })
            assertEquals(2, target.courses().backupSnapshot().size)
        } finally { source.close(); target.close(); f.close() }
    }

    @Test fun invalid_course_slot_and_record_limit_reject_without_mutating_database() = runBlocking {
        val f = fixture(); val source = database(); val target = database()
        try {
            CourseRepository(source, clock).upsert(Course(syncId = "course", title = "Valid", weeklySlots = listOf(CourseWeeklySlot(1, 100, 200))))
            val bytes = export(source, f.settings("invalid-source")).toString(Charsets.UTF_8)
            val invalidCourse = JSONObject(bytes).apply { getJSONArray("courses").getJSONObject(0).put("title", " ") }.toString()
            val invalidSlot = JSONObject(bytes).apply { getJSONArray("courses").getJSONObject(0).getJSONArray("weeklySlots").getJSONObject(0).put("endMinute", 99) }.toString()
            val targetService = service(target, f.settings("invalid-target"))
            listOf(invalidCourse, invalidSlot).forEach { text ->
                val failure = runCatching { targetService.preview(ByteArrayInputStream(text.toByteArray())) }.exceptionOrNull() as BackupException
                assertEquals(BackupFailure.INVALID, failure.reason)
            }
            val tooMany = JSONObject(bytes).apply {
                val courses = getJSONArray("courses")
                for (index in 0..5_000) courses.put(JSONObject()
                    .put("syncId", "extra-$index").put("title", "Extra").put("code", JSONObject.NULL)
                    .put("instructor", JSONObject.NULL).put("academicPeriod", JSONObject.NULL).put("color", JSONObject.NULL)
                    .put("icon", JSONObject.NULL).put("sortOrder", index + 2).put("archivedAt", JSONObject.NULL)
                    .put("createdAt", 1).put("updatedAt", 1).put("weeklySlots", org.json.JSONArray().put(JSONObject()
                        .put("dayOfWeek", 1).put("startMinute", 0).put("endMinute", 1).put("location", JSONObject.NULL))))
            }.toString()
            val largeFailure = runCatching { targetService.preview(ByteArrayInputStream(tooMany.toByteArray())) }.exceptionOrNull() as BackupException
            assertEquals(BackupFailure.INVALID, largeFailure.reason)
            assertTrue(target.courses().backupSnapshot().isEmpty())
            assertTrue(target.tasks().backupSnapshot().isEmpty())
            assertTrue(target.events().backupSnapshot().isEmpty())
        } finally { source.close(); target.close(); f.close() }
    }
}
