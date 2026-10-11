package id.riko.hub.ui.vault

import android.content.Intent
import android.net.Uri
import id.riko.hub.data.remote.drive.DriveAccount
import id.riko.hub.data.remote.drive.DriveAuthorizer
import id.riko.hub.data.remote.drive.DriveAuthorizationResult
import id.riko.hub.data.remote.drive.DriveErrorKind
import id.riko.hub.data.remote.drive.DriveException
import id.riko.hub.data.remote.drive.DriveFile
import id.riko.hub.data.remote.drive.DrivePage
import id.riko.hub.data.remote.drive.MaterialsRepository
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultViewModelTest {
    @After fun resetMainDispatcher() { Dispatchers.resetMain() }

    @Test fun lateAuthorizationCannotReconnectAfterDisconnect() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val pending = CompletableDeferred<DriveAuthorizationResult>()
        val auth = FakeAuthorizer(read = { withContext(NonCancellable) { pending.await() } })
        val repo = FakeRepository()
        val model = newModel(repo, auth)

        model.connect()
        runCurrent()
        model.disconnect()
        pending.complete(DriveAuthorizationResult(accessToken = "late", grantedScopes = listOf(DRIVE_READ_SCOPE)))
        advanceUntilIdle()

        assertNull(model.state.value.connectedEmail)
        assertEquals(0, repo.accountCalls)
        assertNull(model.state.value.error)
    }

    @Test fun lateAboutResponseCannotReconnectAfterDisconnect() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val account = CompletableDeferred<DriveAccount>()
        val repo = FakeRepository().apply {
            accountResult = { withContext(NonCancellable) { account.await() } }
        }
        val model = newModel(repo, FakeAuthorizer(read = {
            DriveAuthorizationResult("read", grantedScopes = listOf(DRIVE_READ_SCOPE))
        }))

        model.connect()
        runCurrent()
        model.disconnect()
        account.complete(DriveAccount("mine", "Saya", "person@example.com"))
        advanceUntilIdle()

        assertNull(model.state.value.connectedEmail)
        assertTrue(repo.listRequests.isEmpty())
    }

    @Test fun lateFolderValidationCannotStartUploadAfterDisconnect() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val folder = CompletableDeferred<DriveFile>()
        val repo = FakeRepository().apply { fileResult = { withContext(NonCancellable) { folder.await() } } }
        val auth = FakeAuthorizer(
            read = { DriveAuthorizationResult("read", grantedScopes = listOf(DRIVE_READ_SCOPE)) },
            picker = { DriveAuthorizationResult("write", pickedFileIds = listOf("folder"), grantedScopes = listOf(DRIVE_FILE_SCOPE)) },
        )
        val model = newModel(repo, auth)

        model.connect()
        advanceUntilIdle()
        model.startUpload()
        runCurrent()
        assertEquals(1, repo.getFileCalls)
        model.disconnect()
        folder.complete(DriveFile("folder", "Folder", "application/vnd.google-apps.folder", canAddChildren = true))
        advanceUntilIdle()

        assertNull(model.state.value.connectedEmail)
        assertNull(model.state.value.pickerDestination)
        assertEquals(false, model.state.value.uploading)
    }

    @Test fun pickerRejectsFolderAuthorizationFromAnotherAccount() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val auth = FakeAuthorizer(
            read = { DriveAuthorizationResult("read", grantedScopes = listOf(DRIVE_READ_SCOPE)) },
            picker = { DriveAuthorizationResult("write", pickedFileIds = listOf("folder"), grantedScopes = listOf(DRIVE_FILE_SCOPE)) },
        )
        val repo = FakeRepository().apply { accounts["write"] = DriveAccount("other", "Other", "other@example.com") }
        val model = newModel(repo, auth)

        model.connect()
        advanceUntilIdle()
        model.startUpload()
        advanceUntilIdle()

        assertNull(model.state.value.error)
        assertEquals("failure:permission", model.state.value.uploadMessage)
        assertEquals(0, repo.getFileCalls)
        assertTrue(auth.clearedTokens.contains("write"))
        assertEquals("person@example.com", model.state.value.connectedEmail)
    }

    @Test fun cancellingAtDocumentSelectionClearsTheUploadGrantAndProgress() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val auth = FakeAuthorizer(
            read = { DriveAuthorizationResult("read", grantedScopes = listOf(DRIVE_READ_SCOPE)) },
            picker = { DriveAuthorizationResult("write", pickedFileIds = listOf("root"), grantedScopes = listOf(DRIVE_FILE_SCOPE)) },
        )
        val model = newModel(FakeRepository(), auth)

        model.connect()
        advanceUntilIdle()
        model.startUpload()
        advanceUntilIdle()
        model.cancelUpload()
        advanceUntilIdle()

        assertEquals(false, model.state.value.uploading)
        assertNull(model.state.value.uploadProgress)
        assertEquals("cancelled", model.state.value.uploadMessage)
        assertTrue(auth.clearedTokens.contains("write"))
    }

    @Test fun folderNavigationAndDebouncedSearchUseCurrentPath() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeRepository()
        val model = newModel(repo, FakeAuthorizer(read = {
            DriveAuthorizationResult("read", grantedScopes = listOf(DRIVE_READ_SCOPE))
        }))

        model.connect()
        advanceUntilIdle()
        val folder = DriveFile("folder", "Kelas", "application/vnd.google-apps.folder", canAddChildren = true)
        model.enterFolder(folder)
        runCurrent()
        model.search("rencana")
        advanceTimeBy(351)
        advanceUntilIdle()
        assertEquals("folder", repo.listRequests.last().first)
        assertEquals("rencana", repo.listRequests.last().second)
        assertTrue(model.backFolder())
        advanceUntilIdle()
        assertEquals("root", repo.listRequests.last().first)
    }

    @Test fun readFailureReturnsToDisconnectedRetryableState() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val model = newModel(FakeRepository(), FakeAuthorizer(read = { throw DriveException(DriveErrorKind.OFFLINE, "offline") }))

        model.connect()
        advanceUntilIdle()

        assertEquals("offline", model.state.value.error)
        assertEquals(false, model.state.value.authenticating)
        assertNull(model.state.value.connectedEmail)
    }

    @Test fun failedReconnectDoesNotLeaveRefreshWithAnInvalidToken() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        var reads = 0
        val auth = FakeAuthorizer(read = {
            if (++reads == 1) DriveAuthorizationResult("read", grantedScopes = listOf(DRIVE_READ_SCOPE))
            else throw DriveException(DriveErrorKind.OFFLINE, "offline")
        })
        val model = newModel(FakeRepository(), auth)

        model.connect()
        advanceUntilIdle()
        assertTrue(model.hasActiveSession())
        model.reconnect()
        advanceUntilIdle()

        assertEquals("person@example.com", model.state.value.connectedEmail)
        assertEquals("offline", model.state.value.error)
        assertTrue(!model.hasActiveSession())
    }

    private class FakeAuthorizer(
        private val read: suspend () -> DriveAuthorizationResult = { DriveAuthorizationResult() },
        private val picker: suspend () -> DriveAuthorizationResult = { DriveAuthorizationResult() },
    ) : DriveAuthorizer {
        val clearedTokens = mutableListOf<String>()
        override suspend fun authorizeRead(accountEmail: String?) = read()
        override suspend fun authorizePicker(accountEmail: String) = picker()
        override fun resultFromIntent(intent: Intent) = DriveAuthorizationResult()
        override suspend fun clearToken(token: String) { clearedTokens += token }
        override suspend fun revoke(accountEmail: String) = Unit
    }

    private fun newModel(repository: MaterialsRepository, authorization: DriveAuthorizer) =
        VaultViewModel(repository, authorization, Dispatchers.Main)

    private class FakeRepository : MaterialsRepository {
        val accounts = mutableMapOf(
            "read" to DriveAccount("mine", "Saya", "person@example.com"),
            "write" to DriveAccount("mine", "Saya", "person@example.com"),
        )
        val listRequests = mutableListOf<Triple<String, String, String?>>()
        var accountResult: suspend (String) -> DriveAccount = { accounts.getValue(it) }
        var fileResult: suspend (String) -> DriveFile = { DriveFile(it, "Folder", "application/vnd.google-apps.folder", canAddChildren = true) }
        var accountCalls = 0
        var getFileCalls = 0
        override suspend fun account(token: String): DriveAccount { accountCalls++; return accountResult(token) }
        override suspend fun getFile(token: String, id: String): DriveFile { getFileCalls++; return fileResult(id) }
        override suspend fun list(token: String, folderId: String, search: String, pageToken: String?): DrivePage {
            listRequests += Triple(folderId, search, pageToken)
            return DrivePage(emptyList(), null)
        }
        override suspend fun upload(token: String, parentId: String, uri: Uri, onProgress: (Long, Long?) -> Unit) = DriveFile("new", "New", "text/plain")
        override suspend fun downloadForOpen(token: String, file: DriveFile): File = error("Not used in this test")
        override fun clearCache() = Unit
    }

    companion object {
        private const val DRIVE_READ_SCOPE = "https://www.googleapis.com/auth/drive.readonly"
        private const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
    }
}
