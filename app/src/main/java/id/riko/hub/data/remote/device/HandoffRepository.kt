package id.riko.hub.data.remote.device

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.UUID
import javax.net.ssl.SSLPeerUnverifiedException

/** Explicit, foreground-only handoff operations over the currently trusted Companion connection. */
class HandoffRepository(context: Context, private val companion: CompanionRepository) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("handoff_history", Context.MODE_PRIVATE)
    private val staging = File(app.filesDir, "handoff_staging")
    private val transport = HandoffTransport(companion)
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val restoredPrepared = readPrepared()
    private val restoredOutgoing = readPendingOutgoing()
    @Volatile private var lastOutgoingTransferId: String? = restoredOutgoing?.first
    @Volatile private var lastOutgoingPeerId: String? = restoredOutgoing?.second
    private var preparedFile: File? = restoredPrepared?.first
    private var currentPrepared: HandoffPreparedFile? = restoredPrepared?.second
    private val mutable = MutableStateFlow(HandoffSnapshot(
        preparedFile = currentPrepared,
        pendingOutgoingTransferId = lastOutgoingTransferId?.takeIf { lastOutgoingPeerId == companion.state.value.trusted?.id },
        history = (readHistory(companion.state.value.trusted?.id) + pendingTransfer(companion.state.value.trusted?.id)).distinctBy { it.transferId }
    ))
    val state = mutable.asStateFlow()
    @Volatile private var screenActive = false
    @Volatile private var activeJob: Job? = null
    @Volatile private var remoteTransferId: String? = null
    @Volatile private var currentSession: CompanionHandoffSession? = null
    private var previewExpiry: Job? = null

    init {
        scope.launch {
            var previousPeerId: String? = null
            companion.state.collect { companionState ->
                val peerId = companionState.trusted?.id
                if (peerId != previousPeerId) {
                    previousPeerId = peerId
                    mutable.value = mutable.value.copy(history = (readHistory(peerId) + pendingTransfer(peerId)).distinctBy { it.transferId },
                        pendingOutgoingTransferId = lastOutgoingTransferId?.takeIf { lastOutgoingPeerId == peerId },
                        supported = false, handoffAllowed = false, offers = emptyList(), textOffers = emptyList(),
                        textPreview = null, workspaces = emptyList(), listsTruncated = false)
                    previewExpiry?.cancel(); previewExpiry = null
                }
                mutable.value = mutable.value.copy(connected = companionState.connected)
                if (!companionState.connected) {
                    mutable.value = mutable.value.copy(supported = false, handoffAllowed = false,
                        offers = emptyList(), textOffers = emptyList(), textPreview = null, workspaces = emptyList(), listsTruncated = false)
                    previewExpiry?.cancel(); previewExpiry = null
                    activeJob?.cancel(CancellationException("Companion connection changed"))
                    companion.cancelHandoffConnections()
                }
            }
        }
    }

    fun setActive(active: Boolean) {
        screenActive = active
        if (!active) {
            activeJob?.cancel(CancellationException("Handoff screen closed"))
            companion.cancelHandoffConnections()
            previewExpiry?.cancel(); previewExpiry = null
            mutable.value = mutable.value.copy(connected = false, textPreview = null)
        } else {
            mutable.value = mutable.value.copy(connected = companion.state.value.connected)
        }
    }

    suspend fun refresh() = exclusive {
        val session = sessionOrThrow()
        mutable.value = mutable.value.copy(connected = true, supported = false, handoffAllowed = false,
            offers = emptyList(), textOffers = emptyList(), textPreview = null, workspaces = emptyList(), listsTruncated = false, error = null)
        try {
            val hello = transport.hello(session)
            verifyHello(hello, session)
            val allowed = hello.getBoolean("handoff_allowed")
            mutable.value = mutable.value.copy(supported = true, handoffAllowed = allowed)
            if (!allowed) return@exclusive
            val transfers = transport.json(session, "GET", "/v2/transfers")
            val offers = transport.json(session, "GET", "/v2/offers")
            val texts = transport.json(session, "GET", "/v2/texts")
            val workspaces = transport.json(session, "GET", "/v2/workspaces")
            transport.checkCurrent(session)
            val saved = (readHistory(session.peerId) + pendingTransfer(session.peerId)).associateBy { it.transferId }
            val serverTransfers = parseTransferList(transfers.optJSONArray("transfers")).map { transfer ->
                if (transfer.transferId == mutable.value.pendingOutgoingTransferId && transfer.direction == "phone_to_laptop" && transfer.state in setOf("cancelled", "failed"))
                    transfer.copy(error = transfer.error ?: "Upload stopped; retry to request laptop approval again.", interrupted = true)
                else if (saved[transfer.transferId]?.interrupted == true)
                    transfer.copy(interrupted = true)
                else transfer
            }
            val parsedOffers = parseTransferList(offers.optJSONArray("offers")).map { HandoffOffer(it) }
            val parsedTexts = parseTextOffers(texts.optJSONArray("texts"))
            val parsedWorkspaces = parseWorkspaces(workspaces.optJSONArray("workspaces"))
            val truncated = transfers.optBoolean("truncated") || offers.optBoolean("truncated") ||
                texts.optBoolean("truncated") || workspaces.optBoolean("truncated")
            mutable.value = mutable.value.copy(
                history = (serverTransfers + saved.values + pendingTransfer(session.peerId)).distinctBy { it.transferId }.take(50),
                offers = parsedOffers, textOffers = parsedTexts, workspaces = parsedWorkspaces,
                listsTruncated = truncated, error = null
            )
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { mutable.value = mutable.value.copy(progress = null); fail(e); throw e }
    }

    /** Copies the user-selected SAF document to private bounded staging before the send preview. */
    suspend fun prepareSend(uri: Uri): HandoffPreparedFile = exclusive {
        val preparedOnDisk = withContext(Dispatchers.IO) {
            val sourceName = app.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            } ?: "Shared file"
            val displayName = safeName(sourceName)
            val mime = app.contentResolver.getType(uri)?.take(127)?.takeIf { it.isNotBlank() && it.none { c -> c.code < 32 || c.code == 127 } }
                ?: "application/octet-stream"
            ensureStaging()
            val baseline = stagingUsage()
            val target = File(staging, "${UUID.randomUUID()}.part")
            val digest = MessageDigest.getInstance("SHA-256")
            var size = 0L
            try {
                val input = app.contentResolver.openInputStream(uri) ?: error("The selected document cannot be opened.")
                input.use { source -> FileOutputStream(target).use { output ->
                    val buffer = ByteArray(32 * 1024)
                while (true) {
                    currentCoroutineContextEnsureActive()
                    val count = source.read(buffer)
                    if (count < 0) break
                    size += count
                    require(size <= HandoffTransport.FILE_LIMIT) { "Files must be 64 MiB or smaller." }
                    output.write(buffer, 0, count); digest.update(buffer, 0, count)
                    require(baseline + size <= STAGING_LIMIT) { "Private handoff storage is full; clear old local transfers first." }
                    }
                    output.fd.sync()
                } }
                require(stagingUsage() <= STAGING_LIMIT) { "Private handoff storage is full; clear old local transfers first." }
                Triple(target, displayName, mime)
            } catch (e: Exception) { target.delete(); throw e }
        }
        val (file, displayName, mime) = preparedOnDisk
        val prepared = HandoffPreparedFile(displayName, mime, file.length(), sha256(file))
        try {
            withContext(Dispatchers.IO) {
                check(prefs.edit().putString("prepared", JSONObject().put("file", file.name).put("display_name", prepared.displayName)
                    .put("mime_type", prepared.mimeType).put("size_bytes", prepared.sizeBytes).put("sha256", prepared.sha256).toString())
                    .remove("outgoing_id").remove("outgoing_peer").commit())
            }
        } catch (e: Exception) {
            file.delete()
            throw e
        }
        withContext(Dispatchers.IO) { preparedFile?.takeIf { it != file }?.delete() }
        preparedFile = file
        currentPrepared = prepared
        lastOutgoingTransferId = null
        lastOutgoingPeerId = null
        mutable.value = mutable.value.copy(preparedFile = prepared, error = null,
            pendingOutgoingTransferId = null,
            history = (readHistory(companion.state.value.trusted?.id) + pendingTransfer(companion.state.value.trusted?.id)).distinctBy { it.transferId })
        prepared
    }

    /** Starts a transfer only after the UI has shown and the user has accepted the prepared-file preview. */
    suspend fun sendPrepared(): HandoffTransfer = exclusive {
        require(lastOutgoingTransferId == null) { "Resume or discard the previous upload before starting another." }
        val prepared = mutable.value.preparedFile ?: error("Choose and preview a file before sending.")
        val file = preparedFile?.takeIf { it.isFile } ?: error("Prepared file is no longer available; choose it again.")
        require(file.length() == prepared.sizeBytes && sha256(file) == prepared.sha256) { "Prepared file changed; choose it again." }
        val session = sessionOrThrow(); requireAllowed(session)
        val transferId = UUID.randomUUID().toString()
        persistOutgoing(transferId, session.peerId)
        val manifest = transferManifest(transferId, prepared)
        remoteTransferId = transferId
        remember(manifest)
        val transfer = createTransfer(session, transferId, prepared)
        require(transfer.transferId == transferId && transfer.direction == "phone_to_laptop" && transfer.sizeBytes == prepared.sizeBytes && transfer.sha256 == prepared.sha256)
        remoteTransferId = transfer.transferId
        lastOutgoingTransferId = transfer.transferId
        lastOutgoingPeerId = session.peerId
        sendChunks(session, file, prepared, transfer)
    }

    /** `destination` must be a fresh URI returned by SAF CreateDocument so an incomplete copy can be removed safely. */
    suspend fun receive(offer: HandoffOffer, destination: Uri): HandoffTransfer = exclusive {
        receiveWithCleanup(destination) { verified ->
            val session = sessionOrThrow(); requireAllowed(session)
            val advertised = offer.transfer
            requireUuid(advertised.transferId)
            require(advertised.direction == "laptop_to_phone" && advertised.available) { "This laptop offer is not available to receive." }
            val remote = transferFrom(transport.json(session, "GET", "/v2/transfers/${advertised.transferId}")).also {
                require(it.transferId == advertised.transferId && it.direction == "laptop_to_phone" && it.state in setOf("offered", "ready", "transferring"))
                require(it.sizeBytes <= HandoffTransport.FILE_LIMIT && it.available)
            }
            receiveTransfer(session, remote, destination, verified)
        }
    }

    suspend fun sendText(text: String) = exclusive {
        val bytes = text.toByteArray(Charsets.UTF_8)
        require(bytes.size <= HandoffTransport.TEXT_LIMIT) { "Text must be 2 KiB or smaller." }
        require(text.isNotEmpty() && text.codePoints().allMatch { cp -> cp == '\n'.code || cp == '\t'.code || isPrintableHandoffCodePoint(cp) }) { "Text contains unsupported characters." }
        val session = sessionOrThrow(); requireAllowed(session)
        transport.json(session, "POST", "/v2/text", JSONObject().put("protocol", 2).put("text", text))
        transport.checkCurrent(session)
        mutable.value = mutable.value.copy(error = null)
    }

    /** Fetches text for an explicit preview. The payload stays in memory and expires after five minutes. */
    suspend fun previewText(textId: String): HandoffTextPreview = exclusive {
        requireUuid(textId)
        val session = sessionOrThrow(); requireAllowed(session)
        val response = transport.json(session, "GET", "/v2/texts/$textId")
        val json = response.getJSONObject("text")
        require(json.getString("text_id") == textId)
        val text = json.getString("text")
        require(text.toByteArray(Charsets.UTF_8).size <= HandoffTransport.TEXT_LIMIT)
        val preview = HandoffTextPreview(textId, text, json.getLong("created_at"), json.getString("direction"))
        require(preview.createdAt >= 0 && preview.direction == "laptop_to_phone")
        mutable.value = mutable.value.copy(textPreview = preview, error = null)
        previewExpiry?.cancel()
        previewExpiry = scope.launch { delay(300_000); mutable.value = mutable.value.copy(textPreview = null) }
        preview
    }

    /** Consume only after the user confirms the preview; this never writes to the system clipboard. */
    suspend fun consumeText(textId: String) = exclusive {
        require(mutable.value.textPreview?.textId == textId) { "Preview this text and confirm it before consuming." }
        require(mutable.value.textPreview?.direction == "laptop_to_phone")
        val session = sessionOrThrow(); requireAllowed(session)
        transport.json(session, "POST", "/v2/texts/$textId/consume", JSONObject().put("protocol", 2))
        mutable.value = mutable.value.copy(textPreview = null, textOffers = mutable.value.textOffers.filterNot { it.textId == textId })
    }

    suspend fun sendWorkspace(workspace: HandoffWorkspace): HandoffWorkspace = exclusive {
        validateWorkspace(workspace)
        val session = sessionOrThrow(); requireAllowed(session)
        val task = workspace.task?.let { JSONObject().put("sync_id", it.syncId).put("title", it.title)
            .put("deadline", it.deadline ?: JSONObject.NULL).put("updated_at", it.updatedAt).put("fingerprint", it.fingerprint) } ?: JSONObject.NULL
        val materials = JSONArray().also { array -> workspace.materials.forEach { array.put(JSONObject()
            .put("title", it.title).put("reference", it.reference).put("transfer_id", it.transferId ?: JSONObject.NULL)) } }
        val body = JSONObject().put("protocol", 2).put("session_id", workspace.sessionId).put("title", workspace.title)
            .put("created_at", workspace.createdAt).put("task", task).put("materials", materials)
            .put("transfer_ids", JSONArray(workspace.transferIds))
        transport.json(session, "POST", "/v2/workspaces", body)
        transport.checkCurrent(session)
        refreshWorkspaceList(session)
        workspace
    }

    /** Cancels the foreground operation; server cancellation is best-effort and bounded. */
    fun cancel() {
        val job = activeJob
        if (job != null) job.cancel(CancellationException("Transfer cancelled by user"))
        companion.cancelHandoffConnections()
    }

    suspend fun retry(transferId: String): HandoffTransfer = exclusive {
        require(transferId == mutable.value.pendingOutgoingTransferId) { "This upload is no longer the pending staged transfer." }
        var id = lastOutgoingTransferId ?: error("There is no resumable upload in this session.")
        require(id == transferId)
        val prepared = mutable.value.preparedFile ?: error("The original staged file is unavailable; choose it again.")
        val file = preparedFile?.takeIf { it.isFile } ?: error("The staged source was removed; choose it again.")
        require(file.length() == prepared.sizeBytes && sha256(file) == prepared.sha256) { "Staged source changed; choose it again." }
        val session = sessionOrThrow(); requireAllowed(session)
        require(lastOutgoingPeerId == session.peerId) { "This upload belongs to a different paired laptop; start a new transfer." }
        remoteTransferId = id
        var remote = try { transferFrom(transport.json(session, "GET", "/v2/transfers/$id")) }
        catch (e: HandoffHttpException) {
            if (e.statusCode != 404) throw e
            id = UUID.randomUUID().toString()
            persistOutgoing(id, session.peerId)
            remoteTransferId = id
            val manifest = transferManifest(id, prepared)
            remember(manifest)
            createTransfer(session, id, prepared)
        }
        require(remote.transferId == id && remote.direction == "phone_to_laptop" && remote.sha256 == prepared.sha256 && remote.sizeBytes == prepared.sizeBytes)
        if (remote.state == "completed") {
            require(remote.sha256 == prepared.sha256 && remote.sizeBytes == prepared.sizeBytes)
            remember(remote); clearOutgoing(); mutable.value = mutable.value.copy(progress = null); return@exclusive remote
        }
        if (remote.state in setOf("cancelled", "failed")) {
            remember(remote.copy(interrupted = true))
            id = UUID.randomUUID().toString()
            persistOutgoing(id, session.peerId)
            remoteTransferId = id
            remote = transferManifest(id, prepared)
            remember(remote)
            remote = createTransfer(session, id, prepared)
            require(remote.transferId == id && remote.direction == "phone_to_laptop" && remote.sha256 == prepared.sha256 && remote.sizeBytes == prepared.sizeBytes)
        }
        require(remote.state in setOf("awaiting_approval", "offered", "ready", "transferring")) {
            "Laptop transfer is ${remote.state}; start a new transfer if needed."
        }
        sendChunks(session, file, prepared, remote)
    }

    /** Re-downloads an interrupted incoming transfer into a fresh SAF CreateDocument destination. */
    suspend fun retryReceive(transferId: String, destination: Uri): HandoffTransfer = exclusive {
        requireUuid(transferId)
        receiveWithCleanup(destination) { verified ->
            val session = sessionOrThrow(); requireAllowed(session)
            val remote = transferFrom(transport.json(session, "GET", "/v2/transfers/$transferId"))
            require(remote.transferId == transferId && remote.direction == "laptop_to_phone" &&
                remote.state in setOf("offered", "ready", "transferring") && remote.available)
            receiveTransfer(session, remote, destination, verified)
        }
    }

    fun clearHistory() {
        prefs.edit().remove("items").apply()
        mutable.value = mutable.value.copy(history = emptyList())
    }

    suspend fun clearLocalStorage() {
        discardPrepared()
        withContext(kotlinx.coroutines.NonCancellable + Dispatchers.IO) {
            check(!staging.exists() || (staging.isDirectory && staging.canonicalFile.parentFile == app.filesDir.canonicalFile))
            staging.listFiles()?.filter { it.name.matches(Regex("[0-9a-fA-F-]{36}\\.part")) && it.isFile }?.forEach {
                check(it.delete()) { "Could not remove a staged handoff file." }
            }
        }
        clearHistory()
    }

    suspend fun discardPrepared() {
        val running = activeJob
        if (running != null) { cancel(); running.join() }
        val id = lastOutgoingTransferId
        val session = withContext(Dispatchers.Main.immediate) { companion.captureHandoffSession() }
        if (id != null && session != null && session.peerId == lastOutgoingPeerId) bestEffortCancel(session, id)
        val file = preparedFile
        withContext(kotlinx.coroutines.NonCancellable + Dispatchers.IO) {
            file?.delete()
            check(prefs.edit().remove("prepared").remove("outgoing_id").remove("outgoing_peer").commit())
        }
        preparedFile = null; lastOutgoingTransferId = null; lastOutgoingPeerId = null
        currentPrepared = null
        mutable.value = mutable.value.copy(preparedFile = null, pendingOutgoingTransferId = null, progress = null)
    }

    private suspend fun sendChunks(
        session: CompanionHandoffSession, file: File, prepared: HandoffPreparedFile, initial: HandoffTransfer
    ): HandoffTransfer {
        var remote = initial
        remoteTransferId = remote.transferId
        mutable.value = mutable.value.copy(progress = HandoffProgress(remote.transferId, prepared.displayName, remote.offset, remote.sizeBytes), error = null)
        try {
            val deadline = System.currentTimeMillis() + APPROVAL_WAIT_MS
            while (remote.state == "awaiting_approval" || remote.state == "offered") {
                if (System.currentTimeMillis() >= deadline) throw HandoffOperationException("Waiting for laptop approval timed out. You can retry this transfer.")
                delay(2_000); transport.checkCurrent(session)
                remote = transferFrom(transport.json(session, "GET", "/v2/transfers/${remote.transferId}"))
                requireSameFile(remote, initial)
            }
            require(remote.state in setOf("ready", "transferring")) { "Laptop transfer is ${remote.state}." }
            val source = withContext(Dispatchers.IO) { RandomAccessFile(file, "r") }
            try {
                var offset = remote.offset
                while (offset < prepared.sizeBytes) {
                    transport.checkCurrent(session)
                    val (chunk, digest) = withContext(Dispatchers.IO) {
                        source.seek(offset)
                        val data = ByteArray(minOf(HandoffTransport.CHUNK_LIMIT.toLong(), prepared.sizeBytes - offset).toInt())
                        source.readFully(data)
                        data to digest(data)
                    }
                    val ack = transferFrom(transport.putChunk(session, remote.transferId, offset, chunk, digest))
                    require(ack.transferId == remote.transferId && ack.offset == offset + chunk.size)
                    offset = ack.offset; remote = ack
                    mutable.value = mutable.value.copy(progress = HandoffProgress(remote.transferId, prepared.displayName, offset, prepared.sizeBytes))
                }
            } finally { withContext(kotlinx.coroutines.NonCancellable + Dispatchers.IO) { source.close() } }
            transport.checkCurrent(session)
            val committed = transport.json(session, "POST", "/v2/transfers/${remote.transferId}/commit", JSONObject()
                .put("protocol", 2).put("sha256", prepared.sha256).put("size_bytes", prepared.sizeBytes))
            transport.checkCurrent(session)
            val result = transferFrom(committed)
            require(result.state == "completed" && result.sha256 == prepared.sha256 && result.sizeBytes == prepared.sizeBytes)
            remember(result); clearOutgoing(); mutable.value = mutable.value.copy(progress = null, error = null); return result
        } catch (e: CancellationException) {
            mutable.value = mutable.value.copy(progress = null, error = "Transfer cancelled or interrupted. Refresh laptop status before retrying.")
            throw e
        } catch (e: Exception) {
            mutable.value = mutable.value.copy(progress = null)
            remember(remote.copy(error = safeError(e), available = false, interrupted = true))
            fail(e); throw e
        }
    }

    private suspend fun createTransfer(session: CompanionHandoffSession, id: String, prepared: HandoffPreparedFile): HandoffTransfer {
        val response = transport.json(session, "POST", "/v2/transfers", createBody(id, prepared))
        return parseTransfer(response.getJSONObject("transfer")).also {
            require(it.transferId == id && it.direction == "phone_to_laptop" && it.sizeBytes == prepared.sizeBytes && it.sha256 == prepared.sha256)
        }
    }

    private fun createBody(id: String, prepared: HandoffPreparedFile) = JSONObject()
        .put("protocol", 2).put("transfer_id", id).put("display_name", prepared.displayName)
        .put("mime_type", prepared.mimeType).put("size_bytes", prepared.sizeBytes).put("sha256", prepared.sha256)

    private fun transferManifest(id: String, prepared: HandoffPreparedFile) = HandoffTransfer(
        id, "phone_to_laptop", "awaiting_approval", prepared.displayName, prepared.mimeType,
        prepared.sizeBytes, prepared.sha256, 0, System.currentTimeMillis(), null, false
    )

    private suspend fun persistOutgoing(id: String, peerId: String) {
        requireUuid(id); requireUuid(peerId)
        withContext(Dispatchers.IO) {
            check(prefs.edit().putString("outgoing_id", id).putString("outgoing_peer", peerId).commit())
        }
        lastOutgoingTransferId = id; lastOutgoingPeerId = peerId
        mutable.value = mutable.value.copy(pendingOutgoingTransferId = id)
    }

    private suspend fun clearOutgoing() {
        withContext(Dispatchers.IO) {
            check(prefs.edit().remove("outgoing_id").remove("outgoing_peer").commit())
        }
        lastOutgoingTransferId = null; lastOutgoingPeerId = null
        mutable.value = mutable.value.copy(pendingOutgoingTransferId = null)
    }

    private suspend fun receiveTransfer(
        session: CompanionHandoffSession, transfer: HandoffTransfer, destination: Uri, onVerified: () -> Unit
    ): HandoffTransfer {
        remoteTransferId = transfer.transferId
        if (transfer.state == "offered")
            transport.json(session, "POST", "/v2/transfers/${transfer.transferId}/consent", JSONObject().put("protocol", 2))
        mutable.value = mutable.value.copy(progress = HandoffProgress(transfer.transferId, transfer.displayName, 0, transfer.sizeBytes))
        val file = receiveToStaging(session, transfer)
        try {
            transport.checkCurrent(session)
            copyToSafAndVerify(session, file, destination, transfer)
            onVerified()
            transport.checkCurrent(session)
            val committed = transport.json(session, "POST", "/v2/transfers/${transfer.transferId}/commit", JSONObject()
                .put("protocol", 2).put("sha256", transfer.sha256).put("size_bytes", transfer.sizeBytes))
            transport.checkCurrent(session)
            val result = transferFrom(committed)
            require(result.state == "completed" && result.sha256 == transfer.sha256 && result.sizeBytes == transfer.sizeBytes)
            remember(result); mutable.value = mutable.value.copy(progress = null, error = null); return result
        } catch (e: CancellationException) {
            remember(transfer.copy(interrupted = true, available = false))
            throw e
        } catch (e: Exception) {
            remember(transfer.copy(interrupted = true, available = transfer.available))
            throw e
        } finally { withContext(kotlinx.coroutines.NonCancellable + Dispatchers.IO) { file.delete() } }
    }

    private suspend fun receiveWithCleanup(
        destination: Uri, block: suspend (() -> Unit) -> HandoffTransfer
    ): HandoffTransfer {
        var destinationVerified = false
        try {
            return block { destinationVerified = true }
        } catch (e: CancellationException) {
            if (destinationVerified) throw CancellationException("File saved and verified; laptop completion is uncertain. Refresh before retrying.")
            if (!discardFreshDestination(destination))
                throw CancellationException("Receive cancelled; the incomplete destination could not be removed. Remove it before retrying.")
            throw e
        } catch (e: Exception) {
            if (destinationVerified)
                throw HandoffOperationException("File saved and verified; laptop completion is uncertain. Refresh before retrying.")
            if (!discardFreshDestination(destination))
                throw HandoffOperationException("Receive failed; the incomplete destination could not be removed. Remove it before retrying.")
            throw e
        }
    }

    private suspend fun receiveToStaging(session: CompanionHandoffSession, transfer: HandoffTransfer): File = withContext(Dispatchers.IO) {
        ensureStaging()
        require(stagingUsage() + transfer.sizeBytes <= STAGING_LIMIT) { "Private handoff storage is full." }
        val file = File(staging, "${UUID.randomUUID()}.part")
        val digest = MessageDigest.getInstance("SHA-256")
        try {
            FileOutputStream(file).use { output ->
                var offset = 0L
                while (offset < transfer.sizeBytes) {
                    currentCoroutineContextEnsureActive(); transport.checkCurrent(session)
                    val requested = minOf(HandoffTransport.CHUNK_LIMIT.toLong(), transfer.sizeBytes - offset).toInt()
                    val chunk = transport.getChunk(session, transfer.transferId, offset, requested)
                    require(chunk.bytes.isNotEmpty() && chunk.bytes.size <= requested && digest(chunk.bytes) == chunk.sha256) {
                        "Laptop returned a corrupt file chunk."
                    }
                    output.write(chunk.bytes); digest.update(chunk.bytes)
                    offset += chunk.bytes.size
                    withContext(Dispatchers.Main.immediate) {
                        mutable.value = mutable.value.copy(progress = HandoffProgress(transfer.transferId, transfer.displayName, offset, transfer.sizeBytes))
                    }
                }
                output.fd.sync()
            }
            require(file.length() == transfer.sizeBytes && digest.digest().hex() == transfer.sha256) { "Downloaded file failed SHA-256 verification." }
            file
        } catch (e: Exception) { file.delete(); throw e }
    }

    private suspend fun copyToSafAndVerify(session: CompanionHandoffSession, file: File, uri: Uri, transfer: HandoffTransfer) = withContext(Dispatchers.IO) {
        try {
            val output = app.contentResolver.openOutputStream(uri, "w") ?: error("The selected destination cannot be written.")
            output.use { target -> FileInputStream(file).use { source ->
                val buffer = ByteArray(32 * 1024)
                while (true) {
                    currentCoroutineContextEnsureActive(); transport.checkCurrent(session)
                    val size = source.read(buffer)
                    if (size < 0) break
                    target.write(buffer, 0, size)
                }
            } }
            val digest = MessageDigest.getInstance("SHA-256")
            var count = 0L
            val input = app.contentResolver.openInputStream(uri) ?: error("The saved destination cannot be read back for verification.")
            input.use { source ->
                val buffer = ByteArray(32 * 1024)
                while (true) {
                    currentCoroutineContextEnsureActive(); transport.checkCurrent(session)
                    val size = source.read(buffer)
                    if (size < 0) break
                    count += size; require(count <= transfer.sizeBytes) { "Saved destination has unexpected extra data." }
                    digest.update(buffer, 0, size)
                }
            }
            require(count == transfer.sizeBytes && digest.digest().hex() == transfer.sha256) {
                "Destination could not be verified; it may contain a partial file. Choose a new destination and retry."
            }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { throw HandoffOperationException("Destination verification failed; choose a new destination and retry.") }
    }

    private fun removeCreatedDestination(uri: Uri): Boolean = runCatching {
        DocumentsContract.deleteDocument(app.contentResolver, uri)
    }.getOrDefault(false)

    suspend fun discardFreshDestination(uri: Uri): Boolean = withContext(kotlinx.coroutines.NonCancellable + Dispatchers.IO) {
        removeCreatedDestination(uri)
    }

    private suspend fun bestEffortCancel(session: CompanionHandoffSession, id: String) {
        if (!companion.isCurrent(session)) return
        withContext(kotlinx.coroutines.NonCancellable) {
            runCatching { withTimeout(3_000) {
                val current = transferFrom(transport.json(session, "GET", "/v2/transfers/$id"))
                if (current.state != "completed" && current.state !in setOf("cancelled", "failed"))
                    transport.json(session, "POST", "/v2/transfers/$id/cancel", JSONObject().put("protocol", 2))
            } }
        }
    }

    private suspend fun refreshWorkspaceList(session: CompanionHandoffSession) {
        val response = transport.json(session, "GET", "/v2/workspaces")
        val list = parseWorkspaces(response.optJSONArray("workspaces"))
        mutable.value = mutable.value.copy(workspaces = list, listsTruncated = response.optBoolean("truncated"))
    }

    private suspend fun <T> exclusive(block: suspend () -> T): T = mutex.withLock {
        if (!screenActive) error("Open the handoff screen to begin a foreground transfer.")
        activeJob = kotlinx.coroutines.currentCoroutineContext()[Job]
        mutable.value = mutable.value.copy(busy = true, error = null)
        try { block() }
        catch (e: CancellationException) {
            val session = currentSession
            val id = remoteTransferId
            if (session != null && id != null) bestEffortCancel(session, id)
            mutable.value = mutable.value.copy(progress = null, error = e.message?.takeIf { it.contains("partial file", true) || it.contains("destination", true) || it.contains("saved and verified", true) }
                ?: "Transfer cancelled or interrupted. Refresh laptop status before retrying.")
            throw e
        }
        catch (e: Exception) { mutable.value = mutable.value.copy(progress = null); fail(e); throw e }
        finally { activeJob = null; currentSession = null; remoteTransferId = null; mutable.value = mutable.value.copy(busy = false) }
    }

    private suspend fun sessionOrThrow(): CompanionHandoffSession = withContext(Dispatchers.Main.immediate) {
        check(screenActive) { "Open the handoff screen to begin a foreground transfer." }
        val session = companion.captureHandoffSession() ?: error("Connect to the trusted laptop over Wi-Fi, then retry.")
        currentSession = session
        session
    }

    private suspend fun requireAllowed(session: CompanionHandoffSession) {
        val hello = transport.hello(session); verifyHello(hello, session)
        val allowed = hello.getBoolean("handoff_allowed")
        mutable.value = mutable.value.copy(supported = true, handoffAllowed = allowed)
        if (!allowed) mutable.value = mutable.value.copy(offers = emptyList(), textOffers = emptyList(),
            textPreview = null, workspaces = emptyList(), listsTruncated = false)
        check(allowed) { "Enable handoff for this paired phone in the laptop Companion first." }
    }

    private fun verifyHello(json: JSONObject, session: CompanionHandoffSession) {
        require(json.optInt("protocol") == 2 && json.optString("peer_id") == session.peerId) { "Laptop identity or handoff protocol does not match." }
        val caps = json.optJSONArray("capabilities") ?: error("Laptop returned invalid capabilities.")
        require(listOf("files", "text", "workspace").all { cap -> (0 until caps.length()).any { caps.optString(it) == cap } })
        val limits = json.optJSONObject("limits") ?: error("Laptop returned invalid transfer limits.")
        require(limits.optInt("chunk_bytes") == HandoffTransport.CHUNK_LIMIT && limits.optLong("file_bytes") == HandoffTransport.FILE_LIMIT &&
            limits.optLong("store_bytes") == HandoffTransport.STORE_LIMIT && limits.optInt("text_bytes") == HandoffTransport.TEXT_LIMIT &&
            limits.optInt("json_response_bytes") == HandoffTransport.RESPONSE_LIMIT) { "Laptop handoff limits are incompatible." }
    }

    private fun parseTransferList(array: JSONArray?): List<HandoffTransfer> {
        require(array != null && array.length() <= 32) { "Laptop returned too many transfer records." }
        return (0 until array.length()).map { parseTransfer(array.getJSONObject(it)) }
    }

    private fun transferFrom(response: JSONObject) = parseTransfer(response.optJSONObject("transfer") ?: response)

    private fun parseTransfer(json: JSONObject): HandoffTransfer {
        val id = json.getString("transfer_id"); requireUuid(id)
        val direction = json.getString("direction"); require(direction in setOf("phone_to_laptop", "laptop_to_phone"))
        val status = json.getString("state"); require(status in TRANSFER_STATES) { "Laptop returned an unknown transfer state." }
        val name = safeName(json.getString("display_name")); val mime = json.getString("mime_type")
        require(mime.length <= 127 && mime.isNotBlank())
        require(json.opt("size_bytes") is Number && json.opt("offset") is Number && json.opt("created_at") is Number)
        require(json.opt("available") is Boolean && (json.isNull("error") || json.opt("error") is String))
        val size = json.getLong("size_bytes"); val offset = json.getLong("offset")
        require(size in 0..HandoffTransport.FILE_LIMIT && offset in 0..size)
        val createdAt = json.getLong("created_at"); require(createdAt >= 0)
        val hash = json.getString("sha256"); require(hash.matches(HASH))
        val error = if (json.isNull("error")) null else "Laptop reported a transfer error."
        return HandoffTransfer(id, direction, status, name, mime, size, hash, offset, createdAt, error, json.getBoolean("available"))
    }

    private fun parseTextOffers(array: JSONArray?): List<HandoffTextOffer> {
        require(array != null && array.length() <= 8) { "Laptop returned too many text offers." }
        return (0 until array.length()).map { i ->
            val item = array.getJSONObject(i); val id = item.getString("text_id"); requireUuid(id)
            require(item.opt("size_bytes") is Number && item.opt("created_at") is Number)
            val size = item.getInt("size_bytes"); require(size in 0..HandoffTransport.TEXT_LIMIT)
            val direction = item.getString("direction"); require(direction in setOf("phone_to_laptop", "laptop_to_phone"))
            val createdAt = item.getLong("created_at"); require(createdAt >= 0)
            HandoffTextOffer(id, createdAt, size, direction)
        }
    }

    private fun parseWorkspaces(array: JSONArray?): List<HandoffWorkspace> {
        require(array != null && array.length() <= 7) { "Laptop returned too many workspace sessions." }
        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            val id = obj.getString("session_id"); requireUuid(id)
            val taskObj = if (obj.isNull("task")) null else obj.optJSONObject("task") ?: error("Laptop returned invalid task metadata.")
            val task = taskObj?.let {
                val taskId = it.getString("sync_id"); requireUuid(taskId)
                val deadline = if (it.isNull("deadline")) null else it.getLong("deadline")
                val updatedAt = it.getLong("updated_at")
                require(updatedAt >= 0 && (deadline == null || deadline >= 0))
                val fingerprint = it.getString("fingerprint"); require(fingerprint.matches(HASH))
                val title = it.getString("title"); requireLabel(title, 128)
                require(handoffTaskReference(taskId, title, deadline, updatedAt).fingerprint == fingerprint)
                HandoffTaskReference(taskId, title, deadline, updatedAt, fingerprint)
            }
            val materialArray = obj.optJSONArray("materials") ?: error("Laptop returned invalid workspace materials.")
            require(materialArray.length() <= 8)
            val materials = (0 until materialArray.length()).map { index ->
                val material = materialArray.getJSONObject(index)
                val transfer = if (material.isNull("transfer_id")) null else material.getString("transfer_id").also(::requireUuid)
                val reference = safeReference(material.getString("reference"))
                HandoffMaterial(safeName(material.getString("title")), reference, transfer)
            }
            val ids = obj.optJSONArray("transfer_ids") ?: error("Laptop returned invalid workspace transfer references.")
            require(ids.length() <= 8)
            val transferIds = (0 until ids.length()).map { ids.getString(it).also(::requireUuid) }
            val createdAt = obj.getLong("created_at"); require(createdAt >= 0)
            require(obj.opt("transfer_available") is Boolean)
            HandoffWorkspace(id, safeName(obj.getString("title")), createdAt, task, materials, transferIds, obj.optBoolean("transfer_available"))
        }
    }

    private fun validateWorkspace(workspace: HandoffWorkspace) {
        requireUuid(workspace.sessionId); requireLabel(workspace.title, 128)
        require(workspace.materials.size <= 8 && workspace.transferIds.size <= 8)
        workspace.task?.let {
            requireUuid(it.syncId); requireLabel(it.title, 128); require(it.fingerprint.matches(HASH))
            require(handoffTaskReference(it.syncId, it.title, it.deadline, it.updatedAt).fingerprint == it.fingerprint)
            require(it.updatedAt >= 0 && (it.deadline == null || it.deadline >= 0))
        }
        workspace.materials.forEach { requireLabel(it.title, 128); safeReference(it.reference); it.transferId?.let(::requireUuid) }
        workspace.transferIds.forEach(::requireUuid)
    }

    private fun requireLabel(value: String, limit: Int) {
        require(value.isNotBlank() && value.toByteArray(Charsets.UTF_8).size <= limit && value.codePoints().allMatch { cp -> isPrintableHandoffCodePoint(cp) })
    }

    private fun safeReference(value: String): String {
        require(value.isNotBlank() && value.toByteArray(Charsets.UTF_8).size <= 512 && value.codePoints().allMatch { cp -> isPrintableHandoffCodePoint(cp) })
        if (value.startsWith("https://", ignoreCase = true)) {
            val uri = runCatching { java.net.URI(value) }.getOrNull()
            require(uri?.scheme.equals("https", ignoreCase = true) && uri?.host != null && uri.userInfo == null)
        } else {
            require(value.matches(Regex("[A-Za-z0-9._:-]{1,512}")))
        }
        return value
    }

    private fun requireSameFile(actual: HandoffTransfer, expected: HandoffTransfer) {
        require(actual.transferId == expected.transferId && actual.sha256 == expected.sha256 && actual.sizeBytes == expected.sizeBytes &&
            actual.direction == "phone_to_laptop") { "Laptop transfer metadata changed; cancel and start again." }
    }

    private fun ensureStaging() {
        if (!staging.exists()) check(staging.mkdirs()) { "Private handoff storage is unavailable." }
        check(staging.isDirectory && staging.canonicalFile.parentFile == app.filesDir.canonicalFile) { "Private handoff storage is unsafe." }
        staging.listFiles()?.filter { it.isFile && it.name.matches(Regex("[0-9a-fA-F-]{36}\\.part")) && System.currentTimeMillis() - it.lastModified() > STAGING_RETENTION_MS }
            ?.forEach { it.delete() }
    }

    private fun stagingUsage(): Long = staging.listFiles()?.filter { it.isFile && it.name.matches(Regex("[0-9a-fA-F-]{36}\\.part")) }?.sumOf { it.length() } ?: 0L
    private suspend fun sha256(file: File): String = withContext(Dispatchers.IO) {
        FileInputStream(file).use { input ->
            val digest = MessageDigest.getInstance("SHA-256"); val buffer = ByteArray(32 * 1024)
            while (true) { currentCoroutineContextEnsureActive(); val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) }
            digest.digest().hex()
        }
    }
    private fun digest(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).hex()
    private fun ByteArray.hex() = joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun remember(transfer: HandoffTransfer) {
        val peerId = currentSession?.peerId ?: return
        val items = (listOf(transfer) + readHistory(peerId).filterNot { it.transferId == transfer.transferId }).take(50)
        val array = JSONArray().also { out -> items.forEach { t -> out.put(JSONObject().put("transfer_id", t.transferId).put("direction", t.direction)
            .put("state", t.state).put("display_name", t.displayName).put("mime_type", t.mimeType).put("size_bytes", t.sizeBytes)
            .put("sha256", t.sha256).put("offset", t.offset).put("created_at", t.createdAt).put("error", t.error ?: JSONObject.NULL)
            .put("available", t.available).put("interrupted", t.interrupted).put("peer_id", peerId)) } }
        prefs.edit().putString("items", array.toString()).apply()
        mutable.value = mutable.value.copy(history = items)
    }

    private fun readHistory(peerId: String?): List<HandoffTransfer> {
        if (peerId == null) return emptyList()
        return runCatching {
            val array = JSONArray(prefs.getString("items", "[]"))
            (0 until minOf(array.length(), 50)).mapNotNull { index ->
                val item = array.getJSONObject(index)
                if (item.optString("peer_id") != peerId) return@mapNotNull null
                parseTransfer(item).let { transfer ->
                if (item.optBoolean("interrupted") || transfer.state in setOf("awaiting_approval", "offered", "ready", "transferring"))
                        transfer.copy(error = "Interrupted; refresh status with the paired laptop.",
                            available = transfer.direction == "laptop_to_phone" && transfer.available, interrupted = true)
                    else transfer
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun readPrepared(): Pair<File, HandoffPreparedFile>? = runCatching {
        val json = prefs.getString("prepared", null)?.let(::JSONObject) ?: return null
        val name = json.getString("file")
        require(name.matches(Regex("[0-9a-f-]{36}\\.part")))
        val file = File(staging, name)
        require(file.isFile && file.canonicalFile.parentFile == staging.canonicalFile)
        val prepared = HandoffPreparedFile(safeName(json.getString("display_name")), json.getString("mime_type"),
            json.getLong("size_bytes"), json.getString("sha256"))
        require(prepared.sizeBytes in 0..HandoffTransport.FILE_LIMIT && file.length() == prepared.sizeBytes &&
            prepared.mimeType.isNotBlank() && prepared.mimeType.length <= 127 && prepared.sha256.matches(HASH))
        file to prepared
    }.getOrElse {
        prefs.edit().remove("prepared").apply()
        null
    }

    private fun readPendingOutgoing(): Pair<String, String>? = runCatching {
        val id = prefs.getString("outgoing_id", null) ?: return null
        val peerId = prefs.getString("outgoing_peer", null) ?: return null
        requireUuid(id); requireUuid(peerId)
        id to peerId
    }.getOrElse {
        prefs.edit().remove("outgoing_id").remove("outgoing_peer").apply()
        null
    }

    private fun pendingTransfer(peerId: String?): List<HandoffTransfer> {
        val id = lastOutgoingTransferId ?: return emptyList()
        val outgoingPeer = lastOutgoingPeerId ?: return emptyList()
        val prepared = currentPrepared ?: return emptyList()
        if (peerId == null || outgoingPeer != peerId) return emptyList()
        return listOf(transferManifest(id, prepared).copy(
            error = "Interrupted; refresh status with the paired laptop.", available = false, interrupted = true
        ))
    }

    private fun safeName(input: String) = handoffSafeName(input)

    private fun requireUuid(value: String) = require(isUuid(value)) { "Invalid handoff identifier." }

    private fun fail(error: Exception) {
        val message = if (error is CancellationException) "Transfer was interrupted. Refresh laptop status before retrying." else safeError(error)
        mutable.value = mutable.value.copy(error = message)
    }

    private fun safeError(error: Exception) = when (error) {
        is SSLPeerUnverifiedException -> "Laptop certificate changed; forget it and pair again."
        is HandoffHttpException -> error.message ?: "Laptop rejected the handoff request."
        is HandoffOperationException -> error.message ?: "Handoff failed. Check the laptop connection and retry."
        is IOException -> "Laptop or storage I/O failed. Check the connection and retry."
        is IllegalArgumentException, is IllegalStateException -> error.message?.take(180)?.takeIf { it.isNotBlank() }
            ?: "Laptop returned invalid handoff data."
        else -> "Handoff failed. Check the laptop connection and retry."
    }

    private suspend fun currentCoroutineContextEnsureActive() = kotlinx.coroutines.currentCoroutineContext().ensureActive()

    companion object {
        private const val STAGING_LIMIT = 134_217_728L
        private const val STAGING_RETENTION_MS = 7L * 24 * 60 * 60 * 1000
        private const val APPROVAL_WAIT_MS = 120_000L
        private val HASH = Regex("[0-9a-f]{64}")
        private val TRANSFER_STATES = setOf("awaiting_approval", "offered", "ready", "transferring", "completed", "cancelled", "failed")
    }
}

private class HandoffOperationException(message: String) : Exception(message)
