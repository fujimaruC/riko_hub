package id.riko.hub.data.remote.drive

import android.accounts.Account
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.ClearTokenRequest
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class DriveAuthorizationResult(
    val accessToken: String? = null,
    val resolution: PendingIntent? = null,
    val pickedFileIds: List<String> = emptyList(),
    val grantedScopes: List<String> = emptyList(),
)

interface DriveAuthorizer {
    suspend fun authorizeRead(accountEmail: String? = null): DriveAuthorizationResult
    suspend fun authorizePicker(accountEmail: String): DriveAuthorizationResult
    fun resultFromIntent(intent: Intent): DriveAuthorizationResult
    suspend fun clearToken(token: String)
    suspend fun revoke(accountEmail: String)
}

/** Keeps Google OAuth tokens in memory; UI owns resolution launch and consumption. */
class GoogleDriveAuthorization(context: Context) : DriveAuthorizer {
    private val client = Identity.getAuthorizationClient(context.applicationContext)

    override suspend fun authorizeRead(accountEmail: String?): DriveAuthorizationResult = authorize(
        request(scopes = listOf(DRIVE_READ), accountEmail = accountEmail, prompt =
            if (accountEmail == null) AuthorizationRequest.Prompt.SELECT_ACCOUNT else AuthorizationRequest.Prompt.NOT_SET),
    )

    override suspend fun authorizePicker(accountEmail: String): DriveAuthorizationResult = authorize(
        request(
            scopes = listOf(DRIVE_FILE), accountEmail = accountEmail,
            prompt = AuthorizationRequest.Prompt.CONSENT,
            picker = true,
        ),
    )

    override fun resultFromIntent(intent: Intent): DriveAuthorizationResult = intentResult(client.getAuthorizationResultFromIntent(intent))

    override suspend fun clearToken(token: String) {
        client.clearToken(ClearTokenRequest.builder().setToken(token).build()).awaitTask()
    }

    override suspend fun revoke(accountEmail: String) {
        client.revokeAccess(RevokeAccessRequest.builder().setScopes(listOf(DRIVE_READ, DRIVE_FILE))
            .setAccount(Account(accountEmail, "com.google")).build()).awaitTask()
    }

    private suspend fun authorize(request: AuthorizationRequest) = intentResult(client.authorize(request).awaitTask())

    private fun request(scopes: List<Scope>, accountEmail: String?, prompt: Int, picker: Boolean = false): AuthorizationRequest {
        val builder = AuthorizationRequest.builder().setRequestedScopes(scopes).setOptOutIncludingGrantedScopes(true).setPrompt(prompt)
        accountEmail?.let { builder.setAccount(Account(it, "com.google")) }
        if (picker) {
            builder.setOptOutIncludingGrantedScopes(true)
                .addResourceParameter(AuthorizationRequest.ResourceParameter.PICKER_OAUTH_TRIGGER, "true")
                .addResourceParameter(AuthorizationRequest.ResourceParameter.PICKER_ALLOW_FOLDER_SELECTION, "true")
                .addResourceParameter(AuthorizationRequest.ResourceParameter.PICKER_MIMETYPES, "application/vnd.google-apps.folder")
        }
        return builder.build()
    }

    private fun intentResult(result: AuthorizationResult) = DriveAuthorizationResult(
        accessToken = result.accessToken,
        resolution = result.pendingIntent.takeIf { result.hasResolution() },
        pickedFileIds = result.tokenResponseParams?.getString("picked_file_ids")?.split(',')?.filter(String::isNotBlank).orEmpty(),
        grantedScopes = result.grantedScopes.orEmpty(),
    )

    private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
        addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
        addOnCanceledListener { continuation.cancel() }
    }

    companion object {
        private val DRIVE_READ = Scope("https://www.googleapis.com/auth/drive.readonly")
        private val DRIVE_FILE = Scope("https://www.googleapis.com/auth/drive.file")
    }
}
