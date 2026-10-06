package id.riko.hub.data

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import id.riko.hub.core.time.Clock
import id.riko.hub.data.local.DebugSeeder
import id.riko.hub.data.local.RikoDatabase
import id.riko.hub.data.prefs.SettingsDataStore
import id.riko.hub.domain.model.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.time.Instant
import java.time.ZoneId

// Run write_debug_seed, adb force-stop, then read_after_process_death as separate instrumentation runs.
class SeedPersistenceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val marker get() = File(context.filesDir, "phase2-smoke-theme.txt")
    private val clock = object : Clock {
        override fun now() = Instant.parse("2026-10-06T00:00:00Z").toEpochMilli()
        override fun zone(): ZoneId = ZoneId.of("Asia/Makassar")
    }
    private fun openDatabase() = Room.databaseBuilder(context, RikoDatabase::class.java, "phase2-persistence-check.db").build()

    @Test fun write_debug_seed() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("persistenceStage") == "write")
        val database = openDatabase()
        try {
            DebugSeeder.seedIfEmpty(database, clock)
            assertEquals(3, database.tasks().observeAll().first().size)
            assertFalse(DebugSeeder.seedIfEmpty(database, clock))
            val settings = SettingsDataStore(context)
            val original = if (marker.exists()) ThemeMode.valueOf(marker.readText()) else settings.settings.first().themeMode
            marker.writeText(original.name)
            settings.setTheme(if (original == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK)
        } finally { database.close() }
    }

    @Test fun read_after_process_death() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("persistenceStage") == "read")
        val original = ThemeMode.valueOf(marker.readText())
        val settings = SettingsDataStore(context)
        val database = openDatabase()
        try {
            assertEquals(if (original == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK, settings.settings.first().themeMode)
            val tasks = database.tasks().observeAll().first()
            assertEquals(3, tasks.size)
            assertTrue(tasks.any { it.title == "Minum air" })
            assertTrue(tasks.all { it.createdAt == clock.now() })
            assertEquals(1, database.events().observeBetween(clock.now(), clock.now() + 86_400_000).first().size)
        } finally {
            settings.setTheme(original)
            marker.delete()
            database.close()
        }
    }
}
