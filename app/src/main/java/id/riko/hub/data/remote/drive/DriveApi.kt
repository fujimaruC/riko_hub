package id.riko.hub.data.remote.drive

import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.coroutines.coroutineContext

internal class DriveApi {
    suspend fun account(token: String): DriveAccount = requestJson(
        token, "GET", "https://www.googleapis.com/drive/v3/about?fields=user(displayName,emailAddress,permissionId)"
    ).getJSONObject("user").let {
        DriveAccount(it.optString("permissionId"), it.optString("displayName"), it.optString("emailAddress"))
    }

    suspend fun list(token: String, folderId: String, search: String, pageToken: String?): DrivePage {
        val params = linkedMapOf(
            "q" to driveQuery(folderId, search),
            "pageSize" to "50",
            "fields" to "nextPageToken,files($FILE_FIELDS)",
        )
        if (pageToken != null) params["pageToken"] = pageToken
        return parseDrivePage(requestJson(token, "GET", "https://www.googleapis.com/drive/v3/files?${params.entries.joinToString("&") { "${enc(it.key)}=${enc(it.value)}" }}"))
    }

    suspend fun getFile(token: String, id: String): DriveFile = parseDriveFile(requestJson(
        token, "GET", "https://www.googleapis.com/drive/v3/files/${enc(id)}?fields=${enc(FILE_FIELDS)}"
    ))

    suspend fun upload(token: String, parentId: String, name: String, mimeType: String, size: Long?, input: java.io.InputStream, onProgress: (Long, Long?) -> Unit): DriveFile {
        val metadata = JSONObject().put("name", name).put("mimeType", mimeType).put("parents", org.json.JSONArray().put(parentId))
        val init = open("POST", "https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable&fields=id,name,mimeType,size,modifiedTime,webViewLink,isAppAuthorized,capabilities(canAddChildren)", token).apply {
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("X-Upload-Content-Type", mimeType)
            if (size != null) setRequestProperty("X-Upload-Content-Length", size.toString())
            doOutput = true
        }
        val location = try {
            coroutineContext.ensureActive()
            init.outputStream.use { it.write(metadata.toString().toByteArray(Charsets.UTF_8)) }
            val code = init.responseCode
            if (code !in 200..299) throw responseError(init, code)
            validateUploadSession(init.getHeaderField("Location") ?: throw DriveException(DriveErrorKind.OTHER, "Drive did not return an upload session"))
        } finally { init.disconnect() }
        var accepted = 0L
        var pending = -1
        val buffer = ByteArray(CHUNK_SIZE)
        while (true) {
            coroutineContext.ensureActive()
            var count = 0
            if (pending >= 0) { buffer[count++] = pending.toByte(); pending = -1 }
            while (count < buffer.size) {
                coroutineContext.ensureActive()
                val read = input.read(buffer, count, buffer.size - count)
                if (read < 0) break
                if (read == 0) continue
                count += read
            }
            val next = if (count == buffer.size) input.read() else -1
            coroutineContext.ensureActive()
            val final = next < 0
            if (count == 0 && accepted == 0L && final) {
                // Empty content is sent as a zero-length PUT.
            } else if (count == 0) break
            val total = if (final) accepted + count else size
            coroutineContext.ensureActive()
            val conn = open("PUT", location, token).apply {
                doOutput = true
                setRequestProperty("Content-Type", mimeType)
                setFixedLengthStreamingMode(count)
                uploadContentRange(accepted, count, total, final)?.let { setRequestProperty("Content-Range", it) }
            }
            try {
                coroutineContext.ensureActive()
                conn.outputStream.use { if (count > 0) it.write(buffer, 0, count) }
                val code = conn.responseCode
                if (code == 200 || code == 201) {
                    accepted += count
                    val uploadedFile = parseDriveFile(JSONObject(conn.inputStream.bufferedReader().use { it.readText() }))
                    onProgress(accepted, total)
                    return uploadedFile
                }
                if (code != 308) throw responseError(conn, code)
                val attemptedEnd = accepted + count
                val confirmed = acknowledgedOffset(conn.getHeaderField("Range")) ?: 0L
                if (confirmed != attemptedEnd) throw DriveException(DriveErrorKind.OTHER, "Drive accepted $confirmed of $attemptedEnd upload bytes")
                accepted = confirmed
                onProgress(accepted, total)
            } finally { conn.disconnect() }
            if (final) throw DriveException(DriveErrorKind.OTHER, "Drive did not complete the upload")
            pending = next
        }
        throw DriveException(DriveErrorKind.OTHER, "Drive did not complete the upload")
    }

    suspend fun download(token: String, file: DriveFile, destination: java.io.File, maxBytes: Long) {
        val conn = open("GET", "https://www.googleapis.com/drive/v3/files/${enc(file.id)}?alt=media", token)
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw responseError(conn, code)
            val declared = conn.getHeaderField("Content-Length")?.toLongOrNull() ?: file.size
            if (declared == null || declared > maxBytes) throw DriveException(DriveErrorKind.OTHER, "File is too large or has unknown size")
            conn.inputStream.use { input -> destination.outputStream().use { output ->
                val buffer = ByteArray(32 * 1024)
                var total = 0L
                while (true) {
                    coroutineContext.ensureActive()
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > maxBytes) throw DriveException(DriveErrorKind.OTHER, "File is too large")
                    output.write(buffer, 0, count)
                }
            } }
        } finally { conn.disconnect() }
    }

    private suspend fun requestJson(token: String, method: String, address: String): JSONObject {
        val conn = open(method, address, token)
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw responseError(conn, code)
            return JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
        } finally { conn.disconnect() }
    }

    private fun open(method: String, address: String, token: String) = (URL(address).openConnection() as HttpURLConnection).apply {
        requestMethod = method
        connectTimeout = TIMEOUT_MS
        readTimeout = TIMEOUT_MS
        instanceFollowRedirects = false
        setRequestProperty("Authorization", "Bearer $token")
        setRequestProperty("Accept", "application/json")
    }

    private fun responseError(conn: HttpURLConnection, code: Int): DriveException {
        val body = runCatching { conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty() }.getOrDefault("")
        val reason = runCatching { JSONObject(body).getJSONObject("error").getJSONArray("errors").optJSONObject(0)?.optString("reason") }.getOrNull().orEmpty()
        val kind = classifyDriveError(code, reason)
        return DriveException(kind, "Drive request failed ($code)")
    }

    private fun validateUploadSession(address: String): String {
        val uri = Uri.parse(address)
        if (uri.scheme != "https" || uri.host !in setOf("www.googleapis.com", "content.googleapis.com")) throw DriveException(DriveErrorKind.OTHER, "Invalid Drive upload session")
        return address
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")
    companion object {
        private const val CHUNK_SIZE = 256 * 1024
        private const val TIMEOUT_MS = 30_000
        private const val FILE_FIELDS = "id,name,mimeType,size,modifiedTime,webViewLink,isAppAuthorized,capabilities(canAddChildren)"
    }
}

internal fun escapeDriveQuery(value: String): String = value.replace("\\", "\\\\").replace("'", "\\'")

internal fun driveQuery(folderId: String, search: String): String = buildList {
    add("trashed = false")
    if (search.isBlank()) add(0, "'${escapeDriveQuery(folderId)}' in parents")
    else add("name contains '${escapeDriveQuery(search)}'")
}.joinToString(" and ")

internal fun parseDrivePage(json: JSONObject): DrivePage {
    val files = json.optJSONArray("files")
    return DrivePage((0 until (files?.length() ?: 0)).map { parseDriveFile(files!!.getJSONObject(it)) }, json.optString("nextPageToken").takeIf(String::isNotEmpty))
}

internal fun parseDriveFile(item: JSONObject): DriveFile {
    val capabilities = item.optJSONObject("capabilities")
    return DriveFile(item.optString("id"), item.optString("name"), item.optString("mimeType"), item.optString("size").toLongOrNull(), item.optString("modifiedTime").takeIf(String::isNotEmpty), item.optString("webViewLink").takeIf(String::isNotEmpty), item.optBoolean("isAppAuthorized"), capabilities?.optBoolean("canAddChildren") ?: false)
}

internal fun classifyDriveError(code: Int, reason: String): DriveErrorKind = when {
    code == 401 -> DriveErrorKind.AUTH
    code == 403 && listOf("quota", "rateLimit", "dailyLimit").any { reason.contains(it, true) } -> DriveErrorKind.QUOTA
    code == 403 -> DriveErrorKind.PERMISSION
    code == 404 -> DriveErrorKind.NOT_FOUND
    code == 429 -> DriveErrorKind.QUOTA
    else -> DriveErrorKind.OTHER
}

internal fun uploadContentRange(start: Long, count: Int, total: Long?, final: Boolean): String? {
    if (count == 0 && final) return null
    val end = start + count - 1
    return "bytes $start-$end/${if (final) total ?: start + count else total ?: "*"}"
}

internal fun acknowledgedOffset(range: String?): Long? = range?.substringAfterLast('-')?.toLongOrNull()?.plus(1)
