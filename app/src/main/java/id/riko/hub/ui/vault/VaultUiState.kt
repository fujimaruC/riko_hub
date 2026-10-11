package id.riko.hub.ui.vault

import id.riko.hub.data.remote.drive.DriveFile
import id.riko.hub.data.remote.drive.DrivePage

data class VaultUiState(
    val connectedEmail: String? = null,
    val connectedName: String? = null,
    val files: List<DriveFile> = emptyList(),
    val folderId: String = "root",
    val breadcrumbs: List<Pair<String, String>> = listOf("root" to "Drive"),
    val search: String = "",
    val loading: Boolean = false,
    val authenticating: Boolean = false,
    val loadingMore: Boolean = false,
    val nextPageToken: String? = null,
    val pageLimitReached: Boolean = false,
    val error: String? = null,
    val uploading: Boolean = false,
    val uploadProgress: Long? = null,
    val uploadTotal: Long? = null,
    val uploadMessage: String? = null,
    val pickerDestination: Pair<String, String>? = null,
)

internal fun appendVaultPage(current: List<DriveFile>, page: DrivePage, reset: Boolean): Pair<List<DriveFile>, Boolean> {
    val files = (if (reset) page.files else current + page.files).take(500)
    return files to (files.size == 500 && page.nextPageToken != null)
}

internal fun isCurrentUpload(session: Int, operation: Int, currentSession: Int, currentOperation: Int,
    accessToken: String, currentToken: String?): Boolean =
    session == currentSession && operation == currentOperation && accessToken == currentToken
