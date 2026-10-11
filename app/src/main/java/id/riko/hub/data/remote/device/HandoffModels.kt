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
    val progress: HandoffProgress? = null,
    val history: List<HandoffTransfer> = emptyList(),
    val offers: List<HandoffOffer> = emptyList(),
    val textOffers: List<HandoffTextOffer> = emptyList(),
    val textPreview: HandoffTextPreview? = null,
    val workspaces: List<HandoffWorkspace> = emptyList(),
    val listsTruncated: Boolean = false,
    val error: String? = null
)

/** Stable task snapshot for an explicit workspace handoff; no Room entity is serialized. */
fun handoffTaskReference(syncId: String, title: String, deadline: Long?, updatedAt: Long): HandoffTaskReference {
    require(isUuid(syncId) &&
        title.isNotBlank() && title.length <= 128 && title.none { it.code < 32 || it.code == 127 })
    val canonical = "$syncId\n$title\n${deadline ?: ""}\n$updatedAt"
    val fingerprint = java.security.MessageDigest.getInstance("SHA-256")
        .digest(canonical.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 0xff) }
    return HandoffTaskReference(syncId, title, deadline, updatedAt, fingerprint)
}
