package id.riko.hub.data.remote.drive

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.coroutines.coroutineContext

class DriveRepository(context: Context) : MaterialsRepository {
    private val appContext = context.applicationContext
    private val api = DriveApi()
    private val cacheDir = File(appContext.cacheDir, "vault-open")
    private val cacheMutex = Mutex()
    @Volatile private var clearRequested = false

    override suspend fun account(token: String) = io { api.account(token) }

    override suspend fun getFile(token: String, id: String) = io { api.getFile(token, id) }

    override suspend fun list(token: String, folderId: String, search: String, pageToken: String?) = io {
        api.list(token, folderId, search, pageToken)
    }

    override suspend fun upload(token: String, parentId: String, uri: Uri, onProgress: (Long, Long?) -> Unit): DriveFile = io {
        validateSelectedMaterial(uri.scheme, "", null)
        val metadata = metadata(uri)
        validateSelectedMaterial(uri.scheme, metadata.name, metadata.mimeType)
        val stream = appContext.contentResolver.openInputStream(uri) ?: throw IOException("Cannot open selected file")
        stream.use { api.upload(token, parentId, metadata.name, metadata.mimeType, metadata.size, it, onProgress) }
    }

    override suspend fun downloadForOpen(token: String, file: DriveFile): File = io {
        if (file.isFolder || file.mimeType.startsWith("application/vnd.google-apps.")) throw DriveException(DriveErrorKind.OTHER, "This Drive file must be opened in Drive")
        if (file.size == null || file.size > MAX_FILE_BYTES) throw DriveException(DriveErrorKind.OTHER, "File is too large or has unknown size")
        cacheMutex.withLock {
            if (clearRequested) { clearCacheFiles(); clearRequested = false }
            prepareCache(MAX_FILE_BYTES)
            if (!cacheDir.mkdirs() && !cacheDir.isDirectory) throw IOException("Cannot create private download cache")
            val destination = File(cacheDir, "vault-${UUID.randomUUID()}.bin")
            val partial = File(cacheDir, "${destination.name}.part")
            try {
                api.download(token, file, partial, minOf(MAX_FILE_BYTES, MAX_CACHE_BYTES))
                coroutineContext.ensureActive()
                if (clearRequested) throw CancellationException("Drive cache was cleared")
                if (!partial.renameTo(destination)) throw IOException("Cannot finalize private download")
                destination
            } catch (error: Exception) {
                partial.delete()
                destination.delete()
                throw error
            } finally {
                if (clearRequested) {
                    clearCacheFiles()
                    clearRequested = false
                    throw CancellationException("Drive cache was cleared")
                }
            }
        }
    }

    override fun clearCache() {
        clearRequested = true
        if (cacheMutex.tryLock()) {
            try { clearCacheFiles() } finally { clearRequested = false; cacheMutex.unlock() }
        }
    }

    private fun prepareCache(requiredBytes: Long) {
        if (!cacheDir.mkdirs() && !cacheDir.isDirectory) throw IOException("Cannot create private download cache")
        val files = cacheDir.listFiles().orEmpty()
        files.filter { it.name.endsWith(".part") }.forEach { it.delete() }
        var cachedBytes = cacheDir.listFiles().orEmpty().sumOf(File::length)
        var bytesToPrune = cacheBytesNeededToPrune(cachedBytes, requiredBytes, MAX_CACHE_BYTES)
        for (oldest in cacheDir.listFiles().orEmpty().sortedBy(File::lastModified)) {
            if (bytesToPrune == 0L) break
            val bytes = oldest.length()
            if (oldest.delete()) {
                cachedBytes -= bytes
                bytesToPrune = cacheBytesNeededToPrune(cachedBytes, requiredBytes, MAX_CACHE_BYTES)
            }
        }
        if (bytesToPrune > 0L) throw DriveException(DriveErrorKind.OTHER, "Download cache limit exceeded")
    }

    private fun clearCacheFiles() { cacheDir.deleteRecursively() }

    private fun metadata(uri: Uri): UploadMetadata {
        var name = "upload"
        var size: Long? = null
        appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameColumn >= 0) name = cursor.getString(nameColumn)?.substringAfterLast('/')?.substringAfterLast('\\')?.takeIf(String::isNotBlank) ?: name
                val sizeColumn = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeColumn >= 0 && !cursor.isNull(sizeColumn)) size = cursor.getLong(sizeColumn).takeIf { it >= 0 }
            }
        }
        return UploadMetadata(name, appContext.contentResolver.getType(uri) ?: "application/octet-stream", size)
    }

    private suspend fun <T> io(block: suspend () -> T): T = withContext(Dispatchers.IO) {
        try { block() } catch (cancelled: CancellationException) { throw cancelled } catch (error: DriveException) { throw error } catch (error: IOException) {
            throw DriveException(DriveErrorKind.OFFLINE, "Network or file access failed", error)
        } catch (error: SecurityException) {
            throw DriveException(DriveErrorKind.PERMISSION, "File access was denied", error)
        }
    }

    private data class UploadMetadata(val name: String, val mimeType: String, val size: Long?)

    companion object {
        private const val MAX_FILE_BYTES = 32L * 1024 * 1024
        private const val MAX_CACHE_BYTES = 64L * 1024 * 1024
    }
}

internal fun cacheBytesNeededToPrune(cachedBytes: Long, requiredBytes: Long, cacheLimit: Long): Long =
    (cachedBytes + requiredBytes - cacheLimit).coerceAtLeast(0L)

internal fun validateSelectedMaterial(scheme: String?, name: String, mimeType: String?): Unit {
    if (!scheme.equals("content", ignoreCase = true)) throw DriveException(DriveErrorKind.PERMISSION, "Choose a file from the system picker")
    val basename = name.substringAfterLast('/').substringAfterLast('\\')
    val extension = basename.substringAfterLast('.', "").lowercase()
    if (extension in setOf("db", "sqlite", "sqlite3")) throw DriveException(DriveErrorKind.PERMISSION, "Database files can't be uploaded")
    val isJson = extension == "json" || mimeType?.endsWith("json", ignoreCase = true) == true
    val normalized = basename.lowercase().filter { it.isLetterOrDigit() }
    if (isJson && normalized.contains("riko") && normalized.contains("hub") && normalized.contains("backup")) {
        throw DriveException(DriveErrorKind.PERMISSION, "Riko Hub backups can't be uploaded")
    }
}
