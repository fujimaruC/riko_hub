package id.riko.hub.data.remote.device

data class HandoffTransfer(
    val transferId: String,
    val direction: String,
    val state: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val sha256: String,
    val offset: Long,
    val createdAt: Long,
    val error: String?,
    val available: Boolean,
    val interrupted: Boolean = false
)

data class HandoffOffer(val transfer: HandoffTransfer)
data class HandoffTextOffer(val textId: String, val createdAt: Long, val sizeBytes: Int, val direction: String)
data class HandoffTextPreview(val textId: String, val text: String, val createdAt: Long, val direction: String)
data class HandoffMaterial(val title: String, val reference: String, val transferId: String? = null)
data class HandoffTaskReference(
    val syncId: String, val title: String, val deadline: Long?, val updatedAt: Long, val fingerprint: String
)
data class HandoffWorkspace(
    val sessionId: String, val title: String, val createdAt: Long,
    val task: HandoffTaskReference?, val materials: List<HandoffMaterial>, val transferIds: List<String>,
    val transferAvailable: Boolean = false
)
data class HandoffPreparedFile(val displayName: String, val mimeType: String, val sizeBytes: Long, val sha256: String)
data class HandoffProgress(val transferId: String?, val displayName: String, val completedBytes: Long, val totalBytes: Long)

data class HandoffSnapshot(
    val connected: Boolean = false,
    val supported: Boolean = false,
    val handoffAllowed: Boolean = false,
    val busy: Boolean = false,
    val preparedFile: HandoffPreparedFile? = null,
    val pendingOutgoingTransferId: String? = null,
    val progress: HandoffProgress? = null,
    val history: List<HandoffTransfer> = emptyList(),
    val offers: List<HandoffOffer> = emptyList(),
    val textOffers: List<HandoffTextOffer> = emptyList(),
    val textPreview: HandoffTextPreview? = null,
    val workspaces: List<HandoffWorkspace> = emptyList(),
    val listsTruncated: Boolean = false,
    val error: String? = null
)

fun canRetryHandoffUpload(transfer: HandoffTransfer, pendingTransferId: String?): Boolean =
    transfer.direction == "phone_to_laptop" && transfer.interrupted && transfer.transferId == pendingTransferId

fun handoffSafeName(input: String): String {
    val output = StringBuilder()
    var byteCount = 0
    var index = 0
    while (index < input.length) {
        val codePoint = input.codePointAt(index)
        val value = if (!isPrintableHandoffCodePoint(codePoint) || codePoint == '/'.code || codePoint == '\\'.code) "_"
            else String(Character.toChars(codePoint))
        val bytes = when {
            value == "_" -> 1
            codePoint <= 0x7f -> 1
            codePoint <= 0x7ff -> 2
            codePoint <= 0xffff -> 3
            else -> 4
        }
        if (byteCount + bytes > 128) break
        output.append(value)
        byteCount += bytes
        index += Character.charCount(codePoint)
    }
    return output.toString().trim().ifBlank { "Shared file" }
}

internal fun isPrintableHandoffCodePoint(value: Int): Boolean = value == ' '.code || Character.getType(value) !in setOf(
    Character.CONTROL.toInt(), Character.FORMAT.toInt(), Character.SURROGATE.toInt(),
    Character.PRIVATE_USE.toInt(), Character.UNASSIGNED.toInt(), Character.LINE_SEPARATOR.toInt(),
    Character.PARAGRAPH_SEPARATOR.toInt(), Character.SPACE_SEPARATOR.toInt()
)

/** Stable task snapshot for an explicit workspace handoff; no Room entity is serialized. */
fun handoffTaskReference(syncId: String, title: String, deadline: Long?, updatedAt: Long): HandoffTaskReference {
    require(isUuid(syncId) &&
        title.isNotBlank() && title.toByteArray(Charsets.UTF_8).size <= 128 &&
        title.codePoints().allMatch { cp -> isPrintableHandoffCodePoint(cp) })
    val canonical = "$syncId\n$title\n${deadline ?: ""}\n$updatedAt"
    val fingerprint = java.security.MessageDigest.getInstance("SHA-256")
        .digest(canonical.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 0xff) }
    return HandoffTaskReference(syncId, title, deadline, updatedAt, fingerprint)
}
