package id.riko.hub.data.backup

import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import androidx.room.withTransaction
import id.riko.hub.core.time.Clock
import id.riko.hub.data.local.RikoDatabase
import id.riko.hub.data.local.entity.EventEntity
import id.riko.hub.data.local.entity.TaskEntity
import id.riko.hub.data.prefs.SettingsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.StringReader
import java.io.OutputStreamWriter
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.nio.ByteBuffer
import java.time.LocalDate

enum class BackupFailure { INVALID, TOO_LARGE, UNSUPPORTED_VERSION, STALE_PREVIEW }
class BackupException(val reason: BackupFailure, cause: Throwable? = null) : Exception("Backup ${reason.name.lowercase()}", cause)
class BackupPreview internal constructor(val totalTasks: Int, val totalEvents: Int, val newTasks: Int, val newEvents: Int, val duplicates: Int, val conflicts: Int, internal val document: Document, internal val fingerprint: List<Any>)
data class BackupImportResult(val insertedTasks: Int, val insertedEvents: Int, val preferencesFailed: Boolean, val remindersFailed: Boolean)

internal data class Document(val tasks: List<TaskRecord>, val events: List<EventRecord>, val settings: SettingsDataStore.BackupValues, val createdAt: Long)
internal data class TaskRecord(
    val syncId: String, val title: String, val notes: String?, val category: String, val priority: String, val status: String,
    val deadlineAt: Long?, val hasDeadlineTime: Boolean, val createdAt: Long, val updatedAt: Long, val completedAt: Long?, val deletedAt: Long?,
    val estimatedMinutes: Int?, val recurrence: String, val checklistJson: String, val reminderMinutesBefore: Int?, val generatedFromSyncId: String?,
    val generationPristine: Boolean, val recurrenceAnchorAt: Long?
)
internal data class EventRecord(val syncId: String, val title: String, val notes: String?, val kind: String, val category: String, val startAt: Long, val endAt: Long?, val location: String?, val createdAt: Long, val updatedAt: Long, val deletedAt: Long?)

class LocalBackupService(
    private val database: RikoDatabase,
    private val settings: SettingsDataStore,
    private val clock: Clock,
    private val reconcileReminders: suspend () -> Unit,
) {
    suspend fun export(): ByteArray {
        val (tasks, events) = database.withTransaction { database.tasks().backupSnapshot() to database.events().backupSnapshot() }
        if (tasks.size + events.size > MAX_RECORDS) throw BackupException(BackupFailure.TOO_LARGE)
        val taskRecords = tasks.toRecords()
        val prefs = settings.backupValues(tasks.associate { it.id to it.syncId }).let { p ->
            if (p.theme !in setOf("SYSTEM", "LIGHT", "DARK") || p.navigation !in setOf("SPATIAL_COMPASS", "COMPASS_ONLY") || p.hints < 0 || p.displayName.isBlank()) throw BackupException(BackupFailure.INVALID)
            if (p.pinnedFocusSyncId == null || p.pinnedFocusDate == null || runCatching { LocalDate.parse(p.pinnedFocusDate) }.isFailure) p.copy(pinnedFocusSyncId = null, pinnedFocusDate = null) else p
        }
        val bytes = encode(Document(taskRecords, events.map { it.toRecord() }, prefs, clock.now()))
        if (bytes.size > MAX_BYTES) throw BackupException(BackupFailure.TOO_LARGE)
        try { decode(bytes) } catch (e: Exception) { throw BackupException(BackupFailure.INVALID, e) }
        return bytes
    }

    suspend fun preview(input: InputStream): BackupPreview {
        val bytes = try { readLimited(input) } catch (e: BackupException) { throw e } catch (e: Exception) { throw BackupException(BackupFailure.INVALID, e) }
        val doc = try { decode(bytes) } catch (e: BackupException) { throw e } catch (e: Exception) { throw BackupException(BackupFailure.INVALID, e) }
        val (tasks, events) = database.withTransaction { database.tasks().backupSnapshot() to database.events().backupSnapshot() }
        val existingT = tasks.associateBy { it.syncId }; val existingE = events.associateBy { it.syncId }
        val entityRecords = tasks.toRecords().associateBy { it.syncId }
        val dupT = doc.tasks.count { entityRecords[it.syncId] == it }
        val confT = doc.tasks.count { entityRecords[it.syncId] != null && entityRecords[it.syncId] != it }
        val eventRecords = events.associate { it.syncId to it.toRecord() }
        val dupE = doc.events.count { eventRecords[it.syncId] == it }
        val confE = doc.events.count { eventRecords[it.syncId]?.let { old -> old != it } == true }
        val syncById = tasks.associate { it.id to it.syncId }
        val owners = tasks.mapNotNull { child -> child.generatedFromTaskId?.let { id -> syncById[id]?.let { it to child.syncId } } }.toMap().toMutableMap()
        val incomingTasks = doc.tasks.filter { it.syncId !in existingT }
        val accepted = acceptedTasks(incomingTasks, existingT.keys, owners)
        val blocked = incomingTasks.size - accepted.size
        val newEvents = doc.events.count { it.syncId !in existingE }
        return BackupPreview(doc.tasks.size, doc.events.size, accepted.size, newEvents, dupT + dupE, confT + confE + blocked, doc, (tasks.sortedBy { it.syncId } + events.map { it.toRecord() }.sortedBy { it.syncId }))
    }

    suspend fun importBackup(preview: BackupPreview, restoreSettings: Boolean): BackupImportResult {
        currentCoroutineContext().ensureActive()
        // Once confirmed, finish the bounded transaction and reconciliation even if the screen closes.
        return withContext(NonCancellable + Dispatchers.IO) {
            val doc = preview.document
            var insertedT = 0; var insertedE = 0
            database.withTransaction {
                val existingTasks = database.tasks().backupSnapshot(); val existingEvents = database.events().backupSnapshot()
                val fingerprint = existingTasks.sortedBy { it.syncId } + existingEvents.map { it.toRecord() }.sortedBy { it.syncId }
                if (fingerprint != preview.fingerprint) throw BackupException(BackupFailure.STALE_PREVIEW)
                val ids = existingTasks.associate { it.syncId to it.id }.toMutableMap()
                val localParentOwners = existingTasks.mapNotNull { t -> t.generatedFromTaskId?.let { it to t.syncId } }.toMap().toMutableMap()
                val parentSyncById = existingTasks.associate { it.id to it.syncId }
                val ownerBySync = localParentOwners.mapNotNull { (parentId, childSync) -> parentSyncById[parentId]?.let { it to childSync } }.toMap().toMutableMap()
                val incomingTasks = doc.tasks.filter { it.syncId !in ids }
                acceptedTasks(incomingTasks, ids.keys, ownerBySync).forEach { r ->
                    val parentId = r.generatedFromSyncId?.let(ids::get)
                    val id = database.tasks().insert(r.toEntity(parentId)); ids[r.syncId] = id
                    if (parentId != null) localParentOwners[parentId] = r.syncId
                    insertedT++
                }
                val existingEventIds = existingEvents.mapTo(mutableSetOf()) { it.syncId }
                doc.events.filter { it.syncId !in existingEventIds }.forEach { database.events().insert(it.toEntity()); insertedE++ }
            }
            var preferencesFailed = false
            var remindersFailed = false
            if (restoreSettings) try {
                val restoredTasks = database.tasks().backupSnapshot()
                val pinId = doc.settings.pinnedFocusSyncId?.let { sync -> restoredTasks.firstOrNull { it.syncId == sync }?.id }
                if (doc.settings.pinnedFocusSyncId != null && pinId == null) preferencesFailed = true
                else settings.restoreBackup(doc.settings, pinId)
            } catch (_: Exception) { preferencesFailed = true }
            try { reconcileReminders() } catch (_: Exception) { remindersFailed = true }
            BackupImportResult(insertedT, insertedE, preferencesFailed, remindersFailed)
        }
    }
}

private fun List<TaskEntity>.toRecords(): List<TaskRecord> {
    val syncById = associate { it.id to it.syncId }
    return map { task ->
        if (task.generatedFromTaskId != null && task.generatedFromTaskId !in syncById) {
            throw BackupException(BackupFailure.INVALID)
        }
        task.toRecord(syncById)
    }
}

private fun TaskEntity.toRecord(syncById: Map<Long, String>) = TaskRecord(
    syncId, title, notes, category, priority, status, deadlineAt, hasDeadlineTime,
    createdAt, updatedAt, completedAt, deletedAt, estimatedMinutes, recurrence,
    canonicalChecklist(checklistJson), reminderMinutesBefore,
    generatedFromTaskId?.let(syncById::get), generationPristine, recurrenceAnchorAt,
)

private fun EventEntity.toRecord() = EventRecord(
    syncId, title, notes, kind, category, startAt, endAt, location, createdAt, updatedAt, deletedAt,
)

private fun TaskRecord.toEntity(parentId: Long?) = TaskEntity(
    syncId = syncId,
    title = title,
    notes = notes,
    category = category,
    priority = priority,
    status = status,
    deadlineAt = deadlineAt,
    hasDeadlineTime = hasDeadlineTime,
    createdAt = createdAt,
    updatedAt = updatedAt,
    completedAt = completedAt,
    deletedAt = deletedAt,
    estimatedMinutes = estimatedMinutes,
    recurrence = recurrence,
    checklistJson = checklistJson,
    reminderMinutesBefore = reminderMinutesBefore,
    generatedFromTaskId = parentId,
    generationPristine = generationPristine,
    recurrenceAnchorAt = recurrenceAnchorAt,
)

private fun EventRecord.toEntity() = EventEntity(
    syncId = syncId,
    title = title,
    notes = notes,
    kind = kind,
    category = category,
    startAt = startAt,
    endAt = endAt,
    location = location,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

private fun acceptedTasks(rows: List<TaskRecord>, existing: Set<String>, owners: Map<String, String>): List<TaskRecord> {
    val children = rows.groupBy { it.generatedFromSyncId }
    val queue = ArrayDeque<TaskRecord>()
    children[null].orEmpty().forEach(queue::addLast)
    rows.filter { it.generatedFromSyncId?.let(existing::contains) == true }.forEach(queue::addLast)
    val parentOwner = owners.toMutableMap()
    val result = mutableListOf<TaskRecord>()
    val visited = mutableSetOf<String>()
    while (queue.isNotEmpty()) {
        val row = queue.removeFirst()
        if (!visited.add(row.syncId)) continue
        val parent = row.generatedFromSyncId
        if (parent != null && parentOwner[parent]?.let { it != row.syncId } == true) continue
        result += row
        if (parent != null) parentOwner[parent] = row.syncId
        children[row.syncId].orEmpty().forEach(queue::addLast)
    }
    return result
}

private fun canonicalChecklist(json: String): String {
    val source = JSONArray(json)
    if (source.length() > 100) throw BackupException(BackupFailure.INVALID)
    val result = JSONArray()
    val ids = mutableSetOf<String>()
    for (i in 0 until source.length()) {
        val item = source.optJSONObject(i) ?: throw BackupException(BackupFailure.INVALID)
        val id = item.opt("id") as? String ?: throw BackupException(BackupFailure.INVALID)
        val title = item.opt("title") as? String ?: throw BackupException(BackupFailure.INVALID)
        val done = item.opt("done") as? Boolean ?: throw BackupException(BackupFailure.INVALID)
        if (id.isBlank() || id.length > 120 || !ids.add(id) || title.trim().length !in 1..120) throw BackupException(BackupFailure.INVALID)
        result.put(org.json.JSONObject().put("id", id).put("title", title).put("done", done))
    }
    return result.toString()
}

private const val MAX_BYTES = 8 * 1024 * 1024
private const val MAX_RECORDS = 10000

private fun readLimited(input: InputStream): ByteArray {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        if (count == 0) {
            val value = input.read()
            if (value < 0) break
            if (out.size() == MAX_BYTES) throw BackupException(BackupFailure.TOO_LARGE)
            out.write(value)
            continue
        }
        if (out.size() + count > MAX_BYTES) throw BackupException(BackupFailure.TOO_LARGE)
        out.write(buffer, 0, count)
    }
    return out.toByteArray()
}

private class BoundedOutput : ByteArrayOutputStream() {
    override fun write(value: Int) {
        if (size() == MAX_BYTES) throw BackupException(BackupFailure.TOO_LARGE)
        super.write(value)
    }

    override fun write(buffer: ByteArray, offset: Int, length: Int) {
        if (size() + length > MAX_BYTES) throw BackupException(BackupFailure.TOO_LARGE)
        super.write(buffer, offset, length)
    }
}

private fun encode(doc: Document): ByteArray {
    val out = BoundedOutput()
    JsonWriter(OutputStreamWriter(out, Charsets.UTF_8)).use { writer ->
        writer.beginObject()
            .name("formatVersion").value(1)
            .name("metadata").beginObject()
                .name("createdAt").value(doc.createdAt)
                .name("app").value("Riko Hub")
            .endObject()
            .name("tasks").beginArray()

        doc.tasks.forEach { task -> writeTask(writer, task) }
        writer.endArray().name("events").beginArray()
        doc.events.forEach { event -> writeEvent(writer, event) }
        writer.endArray()
        writeSettings(writer, doc.settings)
        writer.endObject()
    }
    return out.toByteArray()
}

private fun writeTask(writer: JsonWriter, task: TaskRecord) {
    writer.beginObject()
        .name("syncId").value(task.syncId)
        .name("title").value(task.title)
        .name("notes").nullOr(task.notes)
        .name("category").value(task.category)
        .name("priority").value(task.priority)
        .name("status").value(task.status)
        .name("deadlineAt").nullOr(task.deadlineAt)
        .name("hasDeadlineTime").value(task.hasDeadlineTime)
        .name("createdAt").value(task.createdAt)
        .name("updatedAt").value(task.updatedAt)
        .name("completedAt").nullOr(task.completedAt)
        .name("deletedAt").nullOr(task.deletedAt)
        .name("estimatedMinutes").nullOr(task.estimatedMinutes)
        .name("recurrence").value(task.recurrence)
        .name("checklist").beginArray()

    val checklist = JSONArray(task.checklistJson)
    for (index in 0 until checklist.length()) {
        val item = checklist.getJSONObject(index)
        writer.beginObject()
            .name("id").value(item.getString("id"))
            .name("title").value(item.getString("title"))
            .name("done").value(item.getBoolean("done"))
            .endObject()
    }

    writer.endArray()
        .name("reminderMinutesBefore").nullOr(task.reminderMinutesBefore)
        .name("generatedFromSyncId").nullOr(task.generatedFromSyncId)
        .name("generationPristine").value(task.generationPristine)
        .name("recurrenceAnchorAt").nullOr(task.recurrenceAnchorAt)
        .endObject()
}

private fun writeEvent(writer: JsonWriter, event: EventRecord) {
    writer.beginObject()
        .name("syncId").value(event.syncId)
        .name("title").value(event.title)
        .name("notes").nullOr(event.notes)
        .name("kind").value(event.kind)
        .name("category").value(event.category)
        .name("startAt").value(event.startAt)
        .name("endAt").nullOr(event.endAt)
        .name("location").nullOr(event.location)
        .name("createdAt").value(event.createdAt)
        .name("updatedAt").value(event.updatedAt)
        .name("deletedAt").nullOr(event.deletedAt)
        .endObject()
}

private fun writeSettings(writer: JsonWriter, settings: SettingsDataStore.BackupValues) {
    writer.name("settings").beginObject()
        .name("theme").value(settings.theme)
        .name("displayName").value(settings.displayName)
        .name("navigation").value(settings.navigation)
        .name("hints").value(settings.hints)
        .name("pinnedFocusSyncId").nullOr(settings.pinnedFocusSyncId)
        .name("pinnedFocusDate").nullOr(settings.pinnedFocusDate)
        .endObject()
}

private fun JsonWriter.nullOr(value: Any?): JsonWriter = when (value) {
    null -> nullValue()
    is String -> value(value)
    is Long -> value(value)
    is Int -> value(value)
    else -> error("unsupported JSON value")
}

private fun decode(bytes: ByteArray): Document {
    val decoder = StandardCharsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
    val text = decoder.decode(ByteBuffer.wrap(bytes)).toString()
    validateLexicalJson(text)
    val reader = JsonReader(StringReader(text)).apply { isLenient = false }
    val root = readValue(reader, 0, ParserBudget()) as? Map<*, *> ?: error("root")
    validateUnicode(root)
    if (reader.peek() != JsonToken.END_DOCUMENT) error("trailing")
    root.only(ROOT_KEYS)

    val version = root["formatVersion"] as? Long ?: error("format version")
    if (version != 1L) throw BackupException(BackupFailure.UNSUPPORTED_VERSION)
    val metadata = root.obj("metadata", setOf("createdAt", "app"))
    if (metadata.str("app") != "Riko Hub") error("app")
    val createdAt = metadata.long("createdAt").also(::validTime)

    val tasks = root.arr("tasks").map(::parseTask)
    val events = root.arr("events").map(::parseEvent)
    if (tasks.size + events.size > MAX_RECORDS) error("record count")
    if (tasks.map { it.syncId }.toSet().size != tasks.size) error("duplicate task syncId")
    if (events.map { it.syncId }.toSet().size != events.size) error("duplicate event syncId")

    validateTaskRelationships(tasks)
    val settings = parseSettings(root.obj("settings", SETTINGS_KEYS), tasks.associateBy { it.syncId })
    return Document(tasks, events, settings, createdAt)
}

private val ROOT_KEYS = setOf("formatVersion", "metadata", "tasks", "events", "settings")
private val TASK_KEYS = setOf(
    "syncId", "title", "notes", "category", "priority", "status", "deadlineAt", "hasDeadlineTime",
    "createdAt", "updatedAt", "completedAt", "deletedAt", "estimatedMinutes", "recurrence", "checklist",
    "reminderMinutesBefore", "generatedFromSyncId", "generationPristine", "recurrenceAnchorAt",
)
private val EVENT_KEYS = setOf("syncId", "title", "notes", "kind", "category", "startAt", "endAt", "location", "createdAt", "updatedAt", "deletedAt")
private val CHECKLIST_KEYS = setOf("id", "title", "done")
private val SETTINGS_KEYS = setOf("theme", "displayName", "navigation", "hints", "pinnedFocusSyncId", "pinnedFocusDate")

private fun parseTask(value: Any?): TaskRecord {
    val fields = value as? Map<*, *> ?: error("task")
    fields.only(TASK_KEYS)
    val checklist = fields.arr("checklist")
    if (checklist.size > 100) error("checklist count")

    val ids = mutableSetOf<String>()
    val canonicalChecklist = JSONArray()
    checklist.forEach { value ->
        val item = value as? Map<*, *> ?: error("checklist item")
        item.only(CHECKLIST_KEYS)
        val id = item.str("id")
        val title = item.str("title")
        val done = item.bool("done")
        if (id.isBlank() || id.length > 120 || !ids.add(id) || title.trim().length !in 1..120) error("checklist item")
        canonicalChecklist.put(org.json.JSONObject().put("id", id).put("title", title).put("done", done))
    }

    return TaskRecord(
        syncId = fields.str("syncId"),
        title = fields.str("title"),
        notes = fields.nullStr("notes"),
        category = fields.str("category"),
        priority = fields.str("priority"),
        status = fields.str("status"),
        deadlineAt = fields.nullLong("deadlineAt"),
        hasDeadlineTime = fields.bool("hasDeadlineTime"),
        createdAt = fields.long("createdAt"),
        updatedAt = fields.long("updatedAt"),
        completedAt = fields.nullLong("completedAt"),
        deletedAt = fields.nullLong("deletedAt"),
        estimatedMinutes = fields.nullInt("estimatedMinutes"),
        recurrence = fields.str("recurrence"),
        checklistJson = canonicalChecklist.toString(),
        reminderMinutesBefore = fields.nullInt("reminderMinutesBefore"),
        generatedFromSyncId = fields.nullStr("generatedFromSyncId"),
        generationPristine = fields.bool("generationPristine"),
        recurrenceAnchorAt = fields.nullLong("recurrenceAnchorAt"),
    ).also(::validate)
}

private fun parseEvent(value: Any?): EventRecord {
    val fields = value as? Map<*, *> ?: error("event")
    fields.only(EVENT_KEYS)
    return EventRecord(
        syncId = fields.str("syncId"),
        title = fields.str("title"),
        notes = fields.nullStr("notes"),
        kind = fields.str("kind"),
        category = fields.str("category"),
        startAt = fields.long("startAt"),
        endAt = fields.nullLong("endAt"),
        location = fields.nullStr("location"),
        createdAt = fields.long("createdAt"),
        updatedAt = fields.long("updatedAt"),
        deletedAt = fields.nullLong("deletedAt"),
    ).also(::validate)
}

private fun parseSettings(fields: Map<*, *>, tasks: Map<String, TaskRecord>): SettingsDataStore.BackupValues {
    val pin = fields.nullStr("pinnedFocusSyncId")
    val date = fields.nullStr("pinnedFocusDate")
    if ((pin == null) != (date == null)) error("pin")
    if (pin != null && (pin !in tasks || runCatching { LocalDate.parse(requireNotNull(date)) }.isFailure)) error("pin")

    return SettingsDataStore.BackupValues(
        theme = fields.str("theme"),
        displayName = fields.str("displayName"),
        navigation = fields.str("navigation"),
        hints = fields.int("hints"),
        pinnedFocusSyncId = pin,
        pinnedFocusDate = date,
    ).also { settings ->
        if (settings.theme !in setOf("SYSTEM", "LIGHT", "DARK") ||
            settings.navigation !in setOf("SPATIAL_COMPASS", "COMPASS_ONLY") ||
            settings.hints < 0 || settings.displayName.isBlank()
        ) error("settings")
    }
}

private fun validateTaskRelationships(tasks: List<TaskRecord>) {
    val byId = tasks.associateBy { it.syncId }
    tasks.forEach { task ->
        task.generatedFromSyncId?.let { parent ->
            if (parent == task.syncId || parent !in byId) error("parent")
        }
    }

    val complete = mutableSetOf<String>()
    byId.keys.forEach { start ->
        val path = mutableSetOf<String>()
        var current: String? = start
        while (current != null && current !in complete) {
            if (!path.add(current)) error("cycle")
            current = byId[current]?.generatedFromSyncId
        }
        complete.addAll(path)
    }

    val parents = tasks.mapNotNull { it.generatedFromSyncId }
    if (parents.toSet().size != parents.size) error("multiple children")
}

private fun validate(task: TaskRecord) {
    if (task.syncId.isBlank() || task.syncId.length > 120 ||
        task.title.trim().length !in 1..120 || task.notes?.length?.let { it > 2000 } == true ||
        task.category !in setOf("ACADEMIC", "ORGANIZATION", "PERSONAL", "PROJECTS") ||
        task.priority !in setOf("LOW", "MEDIUM", "HIGH") ||
        task.status !in setOf("NOT_STARTED", "IN_PROGRESS", "DONE") ||
        task.recurrence !in setOf("NONE", "DAILY", "WEEKLY", "MONTHLY") ||
        task.estimatedMinutes?.let { it !in 1..1440 } == true ||
        task.reminderMinutesBefore?.let { it !in 0..10080 } == true ||
        (task.reminderMinutesBefore != null && task.deadlineAt == null) ||
        (task.recurrence != "NONE" && task.deadlineAt == null) ||
        (task.hasDeadlineTime && task.deadlineAt == null) ||
        ((task.status == "DONE") != (task.completedAt != null)) ||
        (task.generationPristine && task.generatedFromSyncId == null) ||
        task.updatedAt < task.createdAt ||
        (task.completedAt != null && task.completedAt > task.updatedAt) ||
        (task.deletedAt != null && task.deletedAt > task.updatedAt)
    ) error("task value")

    listOfNotNull(task.createdAt, task.updatedAt, task.deadlineAt, task.completedAt, task.deletedAt, task.recurrenceAnchorAt)
        .forEach(::validTime)
}

private fun validate(event: EventRecord) {
    if (event.syncId.isBlank() || event.syncId.length > 120 ||
        event.title.trim().length !in 1..120 || event.notes?.length?.let { it > 2000 } == true ||
        event.kind !in setOf("CLASS", "EXAM", "MEETING", "ACTIVITY", "NOTE", "OTHER") ||
        event.category !in setOf("ACADEMIC", "ORGANIZATION", "PERSONAL", "PROJECTS") ||
        (event.endAt != null && event.endAt < event.startAt) ||
        event.updatedAt < event.createdAt ||
        (event.deletedAt != null && event.deletedAt > event.updatedAt)
    ) error("event value")

    listOfNotNull(event.startAt, event.endAt, event.createdAt, event.updatedAt, event.deletedAt).forEach(::validTime)
}

private fun validTime(value: Long) {
    if (value !in 0..253402300799999L) error("time")
}

private class ParserBudget(var nodes: Int = 0)

private fun readValue(reader: JsonReader, depth: Int, budget: ParserBudget): Any? {
    if (depth > 16) error("depth")
    if (++budget.nodes > 250_000) throw BackupException(BackupFailure.TOO_LARGE)

    return when (reader.peek()) {
        JsonToken.BEGIN_OBJECT -> {
            reader.beginObject()
            val fields = linkedMapOf<String, Any?>()
            while (reader.hasNext()) {
                if (fields.size >= 32) throw BackupException(BackupFailure.TOO_LARGE)
                val key = reader.nextName()
                if (key.length > 64) throw BackupException(BackupFailure.TOO_LARGE)
                if (fields.containsKey(key)) error("duplicate key")
                fields[key] = readValue(reader, depth + 1, budget)
            }
            reader.endObject()
            fields
        }
        JsonToken.BEGIN_ARRAY -> {
            reader.beginArray()
            val values = mutableListOf<Any?>()
            while (reader.hasNext()) {
                if (values.size >= MAX_RECORDS) throw BackupException(BackupFailure.TOO_LARGE)
                values += readValue(reader, depth + 1, budget)
            }
            reader.endArray()
            values
        }
        JsonToken.STRING -> reader.nextString().also {
            if (it.length > MAX_BYTES) throw BackupException(BackupFailure.TOO_LARGE)
        }
        JsonToken.NUMBER -> reader.nextString().also {
            if (!it.matches(Regex("-?(0|[1-9][0-9]*)"))) error("integer")
        }.toLong()
        JsonToken.BOOLEAN -> reader.nextBoolean()
        JsonToken.NULL -> { reader.nextNull(); null }
        else -> error("token")
    }
}
private fun validateLexicalJson(text: String) {
    if (text.startsWith('\uFEFF')) error("BOM")
    var i = 0
    while (i < text.length) {
        when {
            text[i] == '"' -> {
                i++
                while (i < text.length && text[i] != '"') {
                    val c = text[i]
                    if (c.code < 0x20) error("raw control character")
                    if (c == '\\') {
                        i++
                        if (i >= text.length || text[i] !in "\"\\/bfnrtu") error("invalid escape")
                        if (text[i] == 'u') {
                            if (i + 4 >= text.length || text.substring(i + 1, i + 5).any { it !in "0123456789abcdefABCDEF" }) {
                                error("invalid unicode escape")
                            }
                            i += 4
                        }
                    }
                    i++
                }
                if (i >= text.length) error("unterminated string")
            }
            text[i].isLetter() -> {
                val start = i
                while (i < text.length && text[i].isLetter()) i++
                if (text.substring(start, i) !in setOf("true", "false", "null")) error("invalid literal")
                continue
            }
        }
        i++
    }
}

private fun validateUnicode(value: Any?) {
    when (value) {
        is String -> {
            var index = 0
            while (index < value.length) {
                val char = value[index]
                if (char.isHighSurrogate()) {
                    if (index + 1 >= value.length || !value[index + 1].isLowSurrogate()) error("invalid unicode")
                    index++
                } else if (char.isLowSurrogate()) {
                    error("invalid unicode")
                }
                index++
            }
        }
        is Map<*, *> -> value.forEach { (key, child) -> validateUnicode(key); validateUnicode(child) }
        is List<*> -> value.forEach(::validateUnicode)
    }
}

private fun Map<*, *>.only(keys: Set<String>) {
    if (this.keys.any { key -> key !is String || key !in keys } || this.keys.size != keys.size) {
        error("unknown or missing key")
    }
}

private fun Map<*, *>.obj(key: String, keys: Set<String>): Map<*, *> =
    (this[key] as? Map<*, *>)?.also { it.only(keys) } ?: error(key)

private fun Map<*, *>.arr(key: String): List<*> = this[key] as? List<*> ?: error(key)
private fun Map<*, *>.str(key: String): String = this[key] as? String ?: error(key)
private fun Map<*, *>.nullStr(key: String): String? = this[key]?.let { it as? String ?: error(key) }
private fun Map<*, *>.long(key: String): Long = this[key] as? Long ?: error(key)
private fun Map<*, *>.nullLong(key: String): Long? = this[key]?.let { it as? Long ?: error(key) }

private fun Map<*, *>.int(key: String): Int = (this[key] as? Long)
    ?.takeIf { it in Int.MIN_VALUE..Int.MAX_VALUE }
    ?.toInt() ?: error(key)

private fun Map<*, *>.nullInt(key: String): Int? = this[key]?.let { value ->
    (value as? Long)?.takeIf { it in Int.MIN_VALUE..Int.MAX_VALUE }?.toInt() ?: error(key)
}

private fun Map<*, *>.bool(key: String): Boolean = this[key] as? Boolean ?: error(key)
