package id.riko.hub.data.remote.drive

import android.net.Uri
import java.io.File

data class DriveFile(
    val id: String,
    val name: String,
    val mimeType: String,
    val size: Long? = null,
    val modifiedTime: String? = null,
    val webViewLink: String? = null,
    val isAppAuthorized: Boolean = false,
    val canAddChildren: Boolean = false,
) {
    val isFolder: Boolean get() = mimeType == "application/vnd.google-apps.folder"
}

data class DrivePage(val files: List<DriveFile>, val nextPageToken: String?)

data class DriveAccount(val permissionId: String, val displayName: String, val emailAddress: String)

enum class DriveErrorKind { AUTH, PERMISSION, OFFLINE, NOT_FOUND, QUOTA, OTHER }

class DriveException(kind: DriveErrorKind, message: String, cause: Throwable? = null) : Exception(message, cause) {
    val kind: DriveErrorKind = kind
}

interface MaterialsRepository {
    suspend fun account(token: String): DriveAccount
    suspend fun getFile(token: String, id: String): DriveFile
    suspend fun list(token: String, folderId: String = "root", search: String = "", pageToken: String? = null): DrivePage
    suspend fun upload(token: String, parentId: String, uri: Uri, onProgress: (Long, Long?) -> Unit): DriveFile
    suspend fun downloadForOpen(token: String, file: DriveFile): File
    fun clearCache()
}
