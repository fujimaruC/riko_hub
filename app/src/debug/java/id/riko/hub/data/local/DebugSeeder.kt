package id.riko.hub.data.local

import androidx.room.withTransaction
import id.riko.hub.core.time.Clock
import id.riko.hub.data.mapper.toEntity
import id.riko.hub.domain.logic.prepareEvent
import id.riko.hub.domain.logic.prepareTask
import id.riko.hub.domain.model.*

// Debug source set only. Explicitly invoked; normal app launches and release builds never seed.
object DebugSeeder {
    suspend fun seedIfEmpty(database: RikoDatabase, clock: Clock): Boolean = database.withTransaction {
        if (database.tasks().adminCount() != 0 || database.events().adminCount() != 0) return@withTransaction false
        val now = clock.now()
        listOf(
            Task(title = "Persiapan UTS Sistem Informasi", category = Category.ACADEMIC, priority = Priority.HIGH, deadlineAt = now + 86_400_000),
            Task(title = "Evaluasi Program Kerja Divisi Acara", category = Category.ORGANIZATION, deadlineAt = now + 172_800_000),
            Task(title = "Minum air", priority = Priority.LOW),
        ).forEach { database.tasks().insert(prepareTask(it, null, now).toEntity()) }
        database.events().insert(prepareEvent(
            Event(title = "Praktikum Basis Data", kind = EventKind.CLASS, startAt = now + 3_600_000, endAt = now + 7_200_000),
            null, now,
        ).toEntity())
        true
    }
}
