package id.riko.hub.ui.vault

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.riko.hub.data.remote.drive.DriveAuthorizer
import id.riko.hub.data.remote.drive.DriveErrorKind
import id.riko.hub.data.remote.drive.DriveException
import id.riko.hub.data.remote.drive.DriveFile
import id.riko.hub.data.remote.drive.MaterialsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface VaultEffect {
    data class LaunchAuthorization(val pendingIntent: android.app.PendingIntent) : VaultEffect
    data object SelectUploadFile : VaultEffect
    data class OpenFile(val file: DriveFile, val session: Int) : VaultEffect
}

class VaultViewModel(
    private val repository: MaterialsRepository,
    private val authorization: DriveAuthorizer,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val mutableState = MutableStateFlow(VaultUiState())
    val state = mutableState.asStateFlow()
    private val effectsChannel = Channel<VaultEffect>(Channel.BUFFERED)
    val effects = effectsChannel.receiveAsFlow()
    private var token: String? = null
    private var uploadToken: String? = null
    private var uploadDestination: Pair<String, String>? = null
    private var browseAccountPermissionId: String? = null
    private var generation = 0
    private var listJob: Job? = null
    private var searchJob: Job? = null
    private var browseCleanupJob: Job? = null
    private var revokeJob: Job? = null
    private var authPurpose: AuthPurpose? = null
    private var authGeneration = 0
    private var authJob: Job? = null
    private var uploadJob: Job? = null
    private var uploadCleanupJob: Job? = null
    private var uploadOperation = 0

    fun connect() {
        if (token != null || authPurpose != null) return
        authPurpose = AuthPurpose.READ
        authGeneration = generation
        mutableState.update { it.copy(authenticating = true, error = null) }
        authJob = viewModelScope.launch { requestRead(null, authGeneration) }
    }

    fun reconnect() {
        val email = mutableState.value.connectedEmail ?: return connect()
        invalidateSession()
        mutableState.update { it.copy(error = null, authenticating = true, loading = true, files = emptyList(),
            folderId = "root", breadcrumbs = listOf("root" to "Drive"), nextPageToken = null, pageLimitReached = false) }
        authPurpose = AuthPurpose.READ
        authGeneration = generation
        authJob = viewModelScope.launch { requestRead(email, authGeneration) }
    }

    private suspend fun requestRead(email: String?, requestGeneration: Int) {
        try {
            browseCleanupJob?.join()
            revokeJob?.join()
            if (requestGeneration != generation) return
            val result = authorization.authorizeRead(email)
            if (requestGeneration == generation) handleAuthorization(result, AuthPurpose.READ, requestGeneration)
        }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { if (requestGeneration == generation) authFailure(error, AuthPurpose.READ) }
    }

    private suspend fun handleAuthorization(result: id.riko.hub.data.remote.drive.DriveAuthorizationResult, purpose: AuthPurpose, requestGeneration: Int) {
        if (requestGeneration != generation) return
        if (result.resolution != null) {
            authPurpose = purpose
            effectsChannel.send(VaultEffect.LaunchAuthorization(result.resolution))
            return
        }
        when (purpose) {
            AuthPurpose.READ -> {
                val access = result.accessToken ?: throw IllegalStateException("Google did not return a Drive token")
                token = access
                if (DRIVE_READ_SCOPE !in result.grantedScopes) throw SecurityException("Drive read access was not granted")
                val account = repository.account(access)
                if (requestGeneration != generation) return
                if (account.permissionId.isBlank() || account.emailAddress.isBlank()) throw SecurityException("Google did not identify the authorized Drive account")
                if (!mutableState.value.connectedEmail.equals(account.emailAddress, ignoreCase = true)) {
                    withContext(ioDispatcher) { repository.clearCache() }
                    if (requestGeneration != generation) return
                }
                generation++
                browseAccountPermissionId = account.permissionId
                mutableState.update { it.copy(connectedEmail = account.emailAddress, connectedName = account.displayName.takeIf(String::isNotBlank),
                    folderId = "root", breadcrumbs = listOf("root" to "Drive"), error = null, uploadMessage = null, authenticating = false) }
                authPurpose = null
                loadFirstPage()
            }
            AuthPurpose.PICKER -> {
                val access = result.accessToken ?: throw IllegalStateException("Google did not return upload access")
                uploadToken = access
                if (DRIVE_FILE_SCOPE !in result.grantedScopes) throw SecurityException("Drive file access was not granted")
                val ids = result.pickedFileIds
                if (ids.size != 1) throw IllegalStateException("Choose exactly one destination folder")
                val account = repository.account(access)
                if (requestGeneration != generation) return
                if (account.permissionId.isBlank() || account.emailAddress.isBlank() || account.permissionId != browseAccountPermissionId ||
                    !account.emailAddress.equals(mutableState.value.connectedEmail, ignoreCase = true)) {
                    throw SecurityException("The selected Google account does not match the connected account")
                }
                val folder = repository.getFile(access, ids.single())
                if (requestGeneration != generation) return
                if (!folder.isFolder || !folder.canAddChildren) throw SecurityException("The selected destination is not writable")
                uploadDestination = folder.id to folder.name
                authPurpose = null
                mutableState.update { it.copy(authenticating = false) }
                if (folder.id == mutableState.value.folderId) effectsChannel.send(VaultEffect.SelectUploadFile)
                else mutableState.update { it.copy(pickerDestination = folder.id to folder.name) }
            }
        }
    }

    fun authorizationResult(resultCodeOk: Boolean, intent: Intent?) {
        val purpose = authPurpose ?: return
        if (authGeneration != generation) return
        if (!resultCodeOk || intent == null) {
            authPurpose = null
            mutableState.update { it.copy(loading = false, authenticating = false,
                error = if (purpose == AuthPurpose.READ && it.connectedEmail != null) "reauth" else null,
                uploadMessage = if (purpose == AuthPurpose.PICKER) null else it.uploadMessage) }
            return
        }
        val resultGeneration = authGeneration
        authJob = viewModelScope.launch {
            try {
                val result = authorization.resultFromIntent(intent)
                if (resultGeneration == generation) handleAuthorization(result, purpose, resultGeneration)
            }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { if (resultGeneration == generation) authFailure(error, purpose) }
        }
    }

    fun confirmPickerDestination() {
        if (mutableState.value.pickerDestination == null) return
        mutableState.update { it.copy(pickerDestination = null, authenticating = false) }
        effectsChannel.trySend(VaultEffect.SelectUploadFile)
    }

    fun cancelPickerDestination() {
        uploadOperation++
        uploadToken?.let(::clearUploadToken)
        uploadToken = null
        uploadDestination = null
        mutableState.update { it.copy(pickerDestination = null, authenticating = uploadCleanupJob?.isActive == true) }
    }

    fun enterFolder(file: DriveFile) {
        if (!file.isFolder) return
        mutableState.update { it.copy(folderId = file.id,
            breadcrumbs = if (it.search.isBlank()) it.breadcrumbs + (file.id to file.name) else listOf("root" to "Drive", file.id to file.name),
            search = "", error = null) }
        loadFirstPage()
    }

    fun navigateToBreadcrumb(index: Int) {
        val crumbs = mutableState.value.breadcrumbs.take(index + 1)
        if (crumbs.isEmpty()) return
        mutableState.update { it.copy(folderId = crumbs.last().first, breadcrumbs = crumbs, search = "", error = null) }
        loadFirstPage()
    }

    fun backFolder(): Boolean {
        val crumbs = mutableState.value.breadcrumbs
        if (crumbs.size <= 1) return false
        navigateToBreadcrumb(crumbs.lastIndex - 1)
        return true
    }

    fun search(value: String) {
        val canLoad = token != null
        mutableState.update { it.copy(search = value, files = emptyList(), nextPageToken = null, pageLimitReached = false,
            loading = canLoad, error = if (canLoad) null else it.error) }
        if (!canLoad) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(350)
            loadFirstPage()
        }
    }

    fun refresh() = loadFirstPage()
    fun loadMore() = loadPage(reset = false)

    suspend fun download(file: DriveFile, expectedSession: Int): java.io.File? {
        val access = token ?: return null
        if (expectedSession != generation) return null
        val session = expectedSession
        return try {
            val cached = repository.downloadForOpen(access, file)
            if (session != generation || access != token) { cached.delete(); null } else cached
        }
        catch (error: DriveException) {
            if (error.kind == DriveErrorKind.AUTH) {
                browseCleanupJob = clearBrowseToken(access)
                browseCleanupJob?.join()
                if (session != generation) return null
                token = null
                mutableState.update { it.copy(error = "reauth") }
            }
            throw error
        }
    }

    private fun loadFirstPage() = loadPage(reset = true)

    private fun loadPage(reset: Boolean) {
        val access = token ?: return
        val current = mutableState.value
        if (!reset && (current.loading || current.loadingMore || current.nextPageToken == null || current.pageLimitReached)) return
        listJob?.cancel()
        val requestGeneration = generation
        val folder = current.folderId
        val query = current.search.trim()
        val pageToken = if (reset) null else current.nextPageToken
        mutableState.update { it.copy(loading = reset, loadingMore = !reset, error = if (reset) null else it.error,
            nextPageToken = if (reset) null else it.nextPageToken, pageLimitReached = if (reset) false else it.pageLimitReached) }
        listJob = viewModelScope.launch {
            try {
                val page = repository.list(access, folder, query, pageToken)
                if (requestGeneration != generation || folder != mutableState.value.folderId || query != mutableState.value.search.trim()) return@launch
                mutableState.update {
                    val (files, limited) = appendVaultPage(it.files, page, reset)
                    it.copy(files = files, nextPageToken = if (limited) null else page.nextPageToken,
                        pageLimitReached = limited, loading = false, loadingMore = false, error = null)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (requestGeneration != generation) return@launch
                if ((error as? DriveException)?.kind == DriveErrorKind.AUTH) {
                    browseCleanupJob = clearBrowseToken(access)
                    browseCleanupJob?.join()
                    if (requestGeneration != generation) return@launch
                    token = null
                }
                mutableState.update { it.copy(loading = false, loadingMore = false, error = errorMessage(error)) }
            }
        }
    }

    fun startUpload() {
        val email = mutableState.value.connectedEmail ?: return
        if (mutableState.value.uploading || mutableState.value.authenticating || authPurpose != null) return
        uploadOperation++
        val operation = uploadOperation
        val previousUpload = uploadJob
        authPurpose = AuthPurpose.PICKER
        authGeneration = generation
        val requestGeneration = generation
        mutableState.update { it.copy(authenticating = true, error = null) }
        authJob = viewModelScope.launch {
            try {
                previousUpload?.join()
                while (uploadCleanupJob?.isActive == true) uploadCleanupJob?.join()
                if (requestGeneration != generation || operation != uploadOperation) return@launch
                val result = authorization.authorizePicker(email)
                if (requestGeneration == generation) handleAuthorization(result, AuthPurpose.PICKER, requestGeneration)
            }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { if (requestGeneration == generation) authFailure(error, AuthPurpose.PICKER) }
        }
    }

    fun upload(uri: Uri, destination: Pair<String, String> = mutableState.value.pickerDestination ?: (mutableState.value.folderId to "")) {
        val access = uploadToken ?: return
        val chosenDestination = uploadDestination ?: destination
        if (mutableState.value.uploading) return
        val session = generation
        val operation = uploadOperation
        mutableState.update { it.copy(uploading = true, uploadProgress = 0L, uploadTotal = null, uploadMessage = null, error = null, pickerDestination = null) }
        uploadJob = viewModelScope.launch {
            try {
                val uploaded = repository.upload(access, chosenDestination.first, uri) { bytes, total ->
                    if (isCurrentUpload(session, operation, generation, uploadOperation, access, uploadToken)) {
                        mutableState.update { it.copy(uploadProgress = bytes, uploadTotal = total) }
                    }
                }
                if (isCurrentUpload(session, operation, generation, uploadOperation, access, uploadToken)) {
                    if (chosenDestination.first != mutableState.value.folderId) {
                        mutableState.update { it.copy(folderId = chosenDestination.first, breadcrumbs = listOf("root" to "Drive", chosenDestination), search = "") }
                    }
                    mutableState.update { it.copy(uploading = false, uploadMessage = "success:${uploaded.name}", uploadProgress = null, uploadTotal = null) }
                    loadFirstPage()
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (isCurrentUpload(session, operation, generation, uploadOperation, access, uploadToken)) {
                    mutableState.update { it.copy(uploading = false, uploadMessage = "failure:${errorMessage(error)}", uploadProgress = null, uploadTotal = null) }
                }
            } finally {
                if (isCurrentUpload(session, operation, generation, uploadOperation, access, uploadToken)) {
                    mutableState.update { it.copy(authenticating = true) }
                    clearUploadToken(access).join()
                    if (operation == uploadOperation) uploadDestination = null
                }
            }
        }
    }

    fun cancelUpload() {
        uploadOperation++
        uploadJob?.cancel()
        val selectedToken = uploadToken
        selectedToken?.let(::clearUploadToken)
        uploadToken = null; uploadDestination = null
        val cleaning = uploadCleanupJob?.isActive == true
        mutableState.update { it.copy(uploading = false, uploadProgress = null, uploadTotal = null, pickerDestination = null,
            uploadMessage = "cancelled", authenticating = cleaning) }
    }

    fun authorizationLaunchFailed() {
        val purpose = authPurpose
        authPurpose = null
        mutableState.update { it.copy(authenticating = false, loading = false,
            error = if (purpose == AuthPurpose.READ && it.connectedEmail != null) "reauth" else if (purpose == AuthPurpose.READ) "general" else null,
            uploadMessage = if (purpose == AuthPurpose.PICKER) "failure:general" else it.uploadMessage) }
    }

    fun open(file: DriveFile) {
        if (file.isFolder) { enterFolder(file); return }
        effectsChannel.trySend(VaultEffect.OpenFile(file, generation))
    }

    fun hasActiveSession(expectedSession: Int? = null) = token != null && (expectedSession == null || expectedSession == generation)

    fun disconnect() {
        val email = mutableState.value.connectedEmail
        invalidateSession()
        viewModelScope.launch(ioDispatcher) { repository.clearCache() }
        mutableState.value = VaultUiState(error = null, uploadMessage = if (email == null) null else "disconnected")
        if (email != null) {
            val disconnectedGeneration = generation
            revokeJob = viewModelScope.launch {
                runCatching { authorization.revoke(email) }.onSuccess {
                    if (disconnectedGeneration == generation && mutableState.value.connectedEmail == null) {
                        mutableState.update { it.copy(uploadMessage = "disconnected_revoked") }
                    }
                }
            }
        }
    }

    private fun invalidateSession() {
        generation++
        authJob?.cancel(); authJob = null
        listJob?.cancel(); searchJob?.cancel(); uploadJob?.cancel()
        token?.let { old -> browseCleanupJob = clearBrowseToken(old) }
        uploadOperation++
        uploadToken?.let(::clearUploadToken)
        token = null; uploadToken = null; uploadDestination = null; browseAccountPermissionId = null; authPurpose = null
    }

    private fun authFailure(error: Exception, purpose: AuthPurpose) {
        authPurpose = null
        if (purpose == AuthPurpose.READ) {
            token?.let { expired -> browseCleanupJob = clearBrowseToken(expired) }
            token = null
        } else {
            uploadToken?.let(::clearUploadToken)
            uploadToken = null; uploadDestination = null
        }
        mutableState.update { it.copy(loading = false, loadingMore = false, uploading = false, authenticating = false,
            error = if (purpose == AuthPurpose.READ) errorMessage(error) else null,
            uploadMessage = if (purpose == AuthPurpose.PICKER) "failure:${errorMessage(error)}" else it.uploadMessage) }
    }

    private fun errorMessage(error: Exception): String = when {
        error is SecurityException -> "permission"
        else -> when ((error as? DriveException)?.kind) {
            DriveErrorKind.AUTH -> "reauth"
            DriveErrorKind.PERMISSION -> "permission"
            DriveErrorKind.OFFLINE -> "offline"
            DriveErrorKind.NOT_FOUND -> "not_found"
            DriveErrorKind.QUOTA -> "quota"
            else -> "general"
        }
    }

    private fun clearUploadToken(accessToken: String): Job = viewModelScope.launch {
        val operation = uploadOperation
        runCatching { authorization.clearToken(accessToken) }
        if (uploadToken == accessToken) uploadToken = null
        if (operation == uploadOperation) mutableState.update { it.copy(authenticating = false) }
    }.also { uploadCleanupJob = it }

    private fun clearBrowseToken(accessToken: String): Job = viewModelScope.launch {
        runCatching { authorization.clearToken(accessToken) }
    }

    private enum class AuthPurpose { READ, PICKER }

    companion object {
        private const val DRIVE_READ_SCOPE = "https://www.googleapis.com/auth/drive.readonly"
        private const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
    }
}
