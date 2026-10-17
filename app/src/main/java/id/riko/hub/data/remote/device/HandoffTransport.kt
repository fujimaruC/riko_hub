package id.riko.hub.data.remote.device

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.net.ssl.HttpsURLConnection

internal class HandoffTransport(private val companion: CompanionRepository) {
    data class BinaryResponse(val bytes: ByteArray, val sha256: String)

    suspend fun hello(session: CompanionHandoffSession): JSONObject = json(session, "GET", "/v2/hello")

    suspend fun json(
        session: CompanionHandoffSession, method: String, path: String, body: JSONObject? = null
    ): JSONObject = withContext(Dispatchers.IO) {
        val bytes = body?.toString()?.toByteArray(Charsets.UTF_8)
        require(bytes == null || bytes.size <= REQUEST_LIMIT) { "Handoff request is too large." }
        val conn = companion.openHandoffConnection(session, path, method)
        try {
            if (bytes != null) {
                checkCurrent(session)
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setFixedLengthStreamingMode(bytes.size)
                conn.outputStream.use { it.write(bytes); checkCurrent(session) }
            }
            val code = conn.responseCode
            checkCurrent(session)
            val response = readBounded(session, if (code in 200..299) conn.inputStream else conn.errorStream, RESPONSE_LIMIT)
            if (code !in 200..299) throw httpError(code)
            checkCurrent(session)
            JSONObject(response.toString(Charsets.UTF_8)).also { verifyEnvelope(it, session) }
        } finally { companion.releaseHandoffConnection(conn) }
    }

    suspend fun putChunk(session: CompanionHandoffSession, transferId: String, offset: Long, bytes: ByteArray, sha256: String): JSONObject = withContext(Dispatchers.IO) {
        requireId(transferId); require(bytes.size in 1..CHUNK_LIMIT && sha256.matches(HASH))
        checkCurrent(session)
        val conn = companion.openHandoffConnection(session, "/v2/transfers/$transferId/chunks?offset=$offset", "PUT")
        try {
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/octet-stream")
            conn.setRequestProperty("X-Riko-Chunk-SHA256", sha256)
            conn.setFixedLengthStreamingMode(bytes.size)
            conn.outputStream.use { stream ->
                var at = 0
                while (at < bytes.size) {
                    checkCurrent(session)
                    val count = minOf(32 * 1024, bytes.size - at)
                    stream.write(bytes, at, count); at += count
                    checkCurrent(session)
                }
            }
            val code = conn.responseCode; checkCurrent(session)
            val response = readBounded(session, if (code in 200..299) conn.inputStream else conn.errorStream, RESPONSE_LIMIT)
            if (code !in 200..299) throw httpError(code)
            JSONObject(response.toString(Charsets.UTF_8)).also { verifyEnvelope(it, session) }
        } finally { companion.releaseHandoffConnection(conn) }
    }

    suspend fun getChunk(session: CompanionHandoffSession, transferId: String, offset: Long, length: Int): BinaryResponse = withContext(Dispatchers.IO) {
        requireId(transferId); require(length in 1..CHUNK_LIMIT)
        checkCurrent(session)
        val conn = companion.openHandoffConnection(session, "/v2/transfers/$transferId/chunks?offset=$offset&length=$length", "GET")
        try {
            conn.setRequestProperty("Accept", "application/octet-stream")
            val code = conn.responseCode; checkCurrent(session)
            if (code !in 200..299) { readBounded(session, conn.errorStream, RESPONSE_LIMIT); throw httpError(code) }
            val hash = conn.getHeaderField("X-Riko-Chunk-SHA256")?.lowercase().orEmpty()
            require(hash.matches(HASH)) { "Laptop returned invalid chunk metadata." }
            val data = readBounded(session, conn.inputStream, CHUNK_LIMIT)
            checkCurrent(session)
            BinaryResponse(data, hash)
        } finally { companion.releaseHandoffConnection(conn) }
    }

    fun checkCurrent(session: CompanionHandoffSession) {
        check(companion.isCurrent(session)) { "Laptop connection changed; reconnect and retry." }
    }

    private fun verifyEnvelope(json: JSONObject, session: CompanionHandoffSession) {
        require(json.optInt("protocol") == 2 && json.optString("peer_id") == session.peerId) {
            "Laptop response identity or protocol does not match."
        }
    }

    private fun readBounded(session: CompanionHandoffSession, stream: java.io.InputStream?, limit: Int): ByteArray {
        if (stream == null) return byteArrayOf()
        stream.use { input ->
            val output = ByteArrayOutputStream(minOf(limit, 8192))
            val buffer = ByteArray(16 * 1024)
            while (true) {
                checkCurrent(session)
                val remaining = limit + 1 - output.size()
                if (remaining <= 0) throw IOException("Laptop response exceeds the protocol limit.")
                val count = input.read(buffer, 0, minOf(buffer.size, remaining))
                if (count < 0) break
                output.write(buffer, 0, count)
                checkCurrent(session)
                if (output.size() > limit) throw IOException("Laptop response exceeds the protocol limit.")
            }
            return output.toByteArray()
        }
    }

    private fun httpError(code: Int): IOException {
        return HandoffHttpException(code, when (code) {
            401, 403 -> "Laptop authorization was revoked or denied."
            404 -> "This handoff feature is unavailable on the paired laptop."
            409 -> "Transfer state changed on the laptop; refresh and retry."
            413 -> "The transfer exceeds the laptop storage limit."
            429 -> "The laptop is busy; retry shortly."
            in 500..599 -> "The laptop handoff service is temporarily unavailable."
            else -> "Laptop rejected the handoff request ($code)."
        })
    }

    companion object {
        const val CHUNK_LIMIT = 262_144
        const val FILE_LIMIT = 67_108_864L
        const val STORE_LIMIT = 268_435_456L
        const val TEXT_LIMIT = 2_048
        const val REQUEST_LIMIT = 8_192
        const val RESPONSE_LIMIT = 65_536
        private val HASH = Regex("[0-9a-f]{64}")
        private fun requireId(value: String) = require(isUuid(value))
    }
}

internal class HandoffHttpException(val statusCode: Int, message: String) : IOException(message)
