package id.riko.hub.work

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.room.withTransaction
import androidx.work.WorkInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import id.riko.hub.core.time.Clock
import id.riko.hub.MainActivity
import id.riko.hub.R
import id.riko.hub.data.local.RikoDatabase
import id.riko.hub.data.mapper.toDomain
import id.riko.hub.domain.logic.TaskReminderSchedule
import id.riko.hub.domain.logic.matches
import id.riko.hub.domain.logic.taskReminderSchedule
import id.riko.hub.domain.logic.taskReminderToken
import id.riko.hub.domain.logic.taskReminderWorkAction
import id.riko.hub.domain.logic.ReminderWorkAction
import id.riko.hub.domain.model.Task
import id.riko.hub.RikoHubApp
import java.util.concurrent.TimeUnit
import java.util.concurrent.Executor

class TaskReminders(private val context: Context) {
    fun requestReconciliation() {
        try {
            val operation = WorkManager.getInstance(context).enqueueUniqueWork(
                RECONCILIATION_WORK,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                OneTimeWorkRequestBuilder<ReminderReconciliationWorker>().build(),
            )
            operation.result.addListener({
                try { operation.result.get() }
                catch (error: Exception) { Log.e("TaskReminders", "Could not queue reconciliation work", error) }
            }, Executor { it.run() })
        } catch (error: Exception) {
            Log.e("TaskReminders", "Could not request reminder reconciliation", error)
        }
    }

    companion object {
        const val CHANNEL_ID = "task_deadline_reminders"
        val CHANNEL_NAME_RES get() = R.string.reminder_channel_name
        internal const val RECONCILIATION_WORK = "task_reminder_reconciliation"
        internal fun reminderWorkName(taskId: Long) = "task_reminder_$taskId"
    }
}

class ReminderReconciliationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) { reconcile() }

    private suspend fun reconcile(): Result {
        val app = applicationContext as? RikoHubApp ?: return Result.failure()
        val container = app.container
        reconcileTaskReminders(container.database, container.clock, WorkManager.getInstance(applicationContext)) {
            cancelReminder(applicationContext, it)
        }
        return Result.success()
    }
}

internal suspend fun reconcileTaskReminders(
    database: RikoDatabase,
    clock: Clock,
    work: WorkManager,
    cancelNotification: (Long) -> Unit,
) {
    for (row in database.tasks().adminFindAllForReminders()) {
        val task = row.toDomain()
        val name = TaskReminders.reminderWorkName(task.id)
        val now = clock.now()
        val token = taskReminderToken(task)
        val infos = work.getWorkInfosForUniqueWork(name).get()
        val isUnfinished: (WorkInfo) -> Boolean = { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.BLOCKED }
        val unfinished = infos.firstOrNull { isUnfinished(it) && token != null && TaskReminderWorker.scheduleFromTags(it.tags) == token }
            ?: infos.firstOrNull(isUnfinished)
        val matchingSucceeded = infos.firstOrNull { token != null && it.state == WorkInfo.State.SUCCEEDED && TaskReminderWorker.scheduleFromTags(it.tags) == token }
        val previous = unfinished ?: matchingSucceeded
        val existingToken = previous?.let { TaskReminderWorker.scheduleFromTags(it.tags) }
        val keepExisting = previous != null && previous.state != WorkInfo.State.FAILED && previous.state != WorkInfo.State.CANCELLED
        when (taskReminderWorkAction(task, now, existingToken, keepExisting)) {
            ReminderWorkAction.KEEP -> continue
            ReminderWorkAction.CANCEL -> {
                work.cancelUniqueWork(name).result.get()
                cancelNotification(task.id)
            }
            ReminderWorkAction.ENQUEUE -> {
                if (existingToken != token) cancelNotification(task.id)
                val schedule = requireNotNull(taskReminderSchedule(task, now))
                val request = OneTimeWorkRequestBuilder<TaskReminderWorker>()
                    .setInputData(TaskReminderWorker.input(schedule.taskId, schedule.deadlineAt, schedule.offsetMinutes))
                    .addTag(TaskReminderWorker.scheduleTag(schedule))
                    .setInitialDelay(schedule.reminderAt - now, TimeUnit.MILLISECONDS)
                    .build()
                work.enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE, request).result.get()
            }
        }
    }
}

class TaskReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) { deliver() }

    private suspend fun deliver(): Result {
        val taskId = inputData.getLong(TASK_ID_EXTRA, 0L)
        val deadline = inputData.getLong(DEADLINE_EXTRA, Long.MIN_VALUE)
        val offset = inputData.getInt(OFFSET_EXTRA, -1)
        if (taskId <= 0 || deadline == Long.MIN_VALUE || offset !in 0..10080) return Result.failure()
        val reminderAt = try { Math.subtractExact(deadline, offset * 60_000L) } catch (_: ArithmeticException) { return Result.failure() }
        val app = applicationContext as? RikoHubApp ?: return Result.failure()
        val delivery = deliverTaskReminder(app.container.database, app.container.clock, TaskReminderSchedule(taskId, deadline, offset, reminderAt)) {
            postReminder(applicationContext, it)
        }
        return when (delivery) {
            ReminderDelivery.TOO_EARLY -> Result.retry()
            ReminderDelivery.DELIVERED, ReminderDelivery.STALE, ReminderDelivery.NOTIFICATIONS_DISABLED -> Result.success()
        }
    }

    companion object {
        const val TASK_ID_EXTRA = "task_id"
        private const val DEADLINE_EXTRA = "deadline_at"
        private const val OFFSET_EXTRA = "offset_minutes"
        internal fun input(taskId: Long, deadlineAt: Long, offsetMinutes: Int) = androidx.work.Data.Builder()
            .putLong(TASK_ID_EXTRA, taskId).putLong(DEADLINE_EXTRA, deadlineAt).putInt(OFFSET_EXTRA, offsetMinutes).build()
        internal fun scheduleTag(schedule: TaskReminderSchedule) =
            "task-reminder:${schedule.taskId}:${schedule.deadlineAt}:${schedule.offsetMinutes}"
        internal fun scheduleFromTags(tags: Set<String>): TaskReminderSchedule? {
            val values = tags.firstOrNull { it.startsWith("task-reminder:") }?.split(':') ?: return null
            if (values.size != 4 || values[0] != "task-reminder") return null
            val id = values[1].toLongOrNull() ?: return null
            val deadline = values[2].toLongOrNull() ?: return null
            val offset = values[3].toIntOrNull() ?: return null
            if (id <= 0 || deadline == Long.MIN_VALUE || offset !in 0..10080) return null
            val at = try { Math.subtractExact(deadline, offset * 60_000L) } catch (_: ArithmeticException) { return null }
            return TaskReminderSchedule(id, deadline, offset, at)
        }
    }
}

internal enum class ReminderDelivery { DELIVERED, STALE, TOO_EARLY, NOTIFICATIONS_DISABLED }

internal suspend fun deliverTaskReminder(
    database: RikoDatabase,
    clock: id.riko.hub.core.time.Clock,
    schedule: TaskReminderSchedule,
    post: (Task) -> Boolean,
): ReminderDelivery =
    database.withTransaction {
        val task = database.tasks().adminFindForWrite(schedule.taskId)?.toDomain() ?: return@withTransaction ReminderDelivery.STALE
        if (!schedule.matches(task)) return@withTransaction ReminderDelivery.STALE
        if (clock.now() < schedule.reminderAt) return@withTransaction ReminderDelivery.TOO_EARLY
        if (post(task)) ReminderDelivery.DELIVERED else ReminderDelivery.NOTIFICATIONS_DISABLED
    }

internal fun postReminder(context: Context, task: Task): Boolean {
    return try {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        val manager = context.getSystemService(NotificationManager::class.java)
        if (!manager.areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(TaskReminders.CHANNEL_ID, context.getString(TaskReminders.CHANNEL_NAME_RES), NotificationManager.IMPORTANCE_DEFAULT)
            )
            if (manager.getNotificationChannel(TaskReminders.CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_NONE) return false
        }
        val intent = Intent(context, MainActivity::class.java).putExtra(TaskReminderWorker.TASK_ID_EXTRA, task.id)
            .setData(Uri.parse("riko://task/${task.id}"))
        val pendingIntent = PendingIntent.getActivity(context, task.id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(context, TaskReminders.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_reminder)
            .setContentTitle(context.getString(R.string.reminder_notification_title))
            .setContentText(task.title)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
        manager.notify("task:${task.id}", 0, notification)
        true
    } catch (_: SecurityException) {
        false
    }
}

private fun cancelReminder(context: Context, taskId: Long) {
    context.getSystemService(NotificationManager::class.java).cancel("task:$taskId", 0)
}
