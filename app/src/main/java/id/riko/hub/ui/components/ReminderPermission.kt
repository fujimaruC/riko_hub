package id.riko.hub.ui.components

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import id.riko.hub.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import id.riko.hub.work.TaskReminders
import id.riko.hub.ui.theme.Spacing

enum class ReminderPermissionStatus { ENABLED, PERMISSION, APP_DISABLED, CHANNEL_DISABLED }

@Stable
class ReminderPermissionState internal constructor(
    val status: ReminderPermissionStatus,
    private val request: () -> Unit,
    private val openSettings: () -> Unit,
) {
    fun request() = request.invoke()
    fun openSettings() = openSettings.invoke()
}

@Composable
fun rememberReminderPermissionState(onEnabled: () -> Unit = {}): ReminderPermissionState {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var refresh by remember { mutableIntStateOf(0) }
    val refreshStatus = { refresh++ }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        refreshStatus()
        if (granted) onEnabled()
    }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refreshStatus() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val status = remember(refresh) {
        val manager = context.getSystemService(NotificationManager::class.java)
        when {
            Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED -> ReminderPermissionStatus.PERMISSION
            !manager.areNotificationsEnabled() -> ReminderPermissionStatus.APP_DISABLED
            Build.VERSION.SDK_INT >= 26 && manager.getNotificationChannel(TaskReminders.CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_NONE -> ReminderPermissionStatus.CHANNEL_DISABLED
            else -> ReminderPermissionStatus.ENABLED
        }
    }
    return remember(status, launcher) {
        ReminderPermissionState(status,
            request = {
                if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                else if (status == ReminderPermissionStatus.ENABLED) onEnabled()
                else context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            },
            openSettings = {
                val intent = if (Build.VERSION.SDK_INT >= 26 && status == ReminderPermissionStatus.CHANNEL_DISABLED)
                    Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        .putExtra(Settings.EXTRA_CHANNEL_ID, TaskReminders.CHANNEL_ID)
                else Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            })
    }
}

@Composable
fun ReminderPermissionNotice(state: ReminderPermissionState) {
    val message = stringResource(when (state.status) {
        ReminderPermissionStatus.ENABLED -> R.string.reminder_permission_on
        ReminderPermissionStatus.PERMISSION -> R.string.reminder_permission_denied
        ReminderPermissionStatus.APP_DISABLED -> R.string.reminder_app_notifications_off
        ReminderPermissionStatus.CHANNEL_DISABLED -> R.string.reminder_channel_notifications_off
    })
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space1)) {
        Text(message, style = MaterialTheme.typography.bodySmall)
        if (state.status == ReminderPermissionStatus.PERMISSION)
            TextButton(onClick = state::request, contentPadding = PaddingValues(0.dp)) {
                Text(stringResource(R.string.reminder_enable_notifications))
            }
        if (state.status != ReminderPermissionStatus.ENABLED)
            TextButton(onClick = state::openSettings, contentPadding = PaddingValues(0.dp)) {
                Text(stringResource(R.string.reminder_open_settings))
            }
    }
}
