package id.riko.hub.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import id.riko.hub.MainActivity
import id.riko.hub.R
import id.riko.hub.RikoHubApp
import id.riko.hub.core.time.Clock
import id.riko.hub.domain.logic.UrgencyInfo
import id.riko.hub.domain.logic.nextDateRefreshAt
import id.riko.hub.domain.logic.nextWidgetRefreshAt
import id.riko.hub.domain.logic.selectWidgetTasks
import id.riko.hub.domain.logic.widgetTaskCapacity
import id.riko.hub.domain.model.Priority
import id.riko.hub.domain.model.Task
import id.riko.hub.work.TaskReminderWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class TaskWidgets(private val context: Context) {
    fun requestUpdate() {
        try {
            if (widgetIds(context).isEmpty()) return
            val operation = WorkManager.getInstance(context).enqueueUniqueWork(
                RENDER_WORK, ExistingWorkPolicy.APPEND_OR_REPLACE,
                OneTimeWorkRequestBuilder<TaskWidgetRenderWorker>().build(),
            )
            operation.result.addListener({
                try { operation.result.get() }
                catch (error: Exception) { Log.e(TAG, "Could not queue widget update", error) }
            }, Executor { it.run() })
        } catch (error: Exception) {
            Log.e(TAG, "Could not request widget update", error)
        }
    }

    companion object {
        internal const val RENDER_WORK = "task_widget_render"
        internal const val BOUNDARY_WORK = "task_widget_boundary"
    }
}

class TaskWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = update(context)
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: android.os.Bundle) = update(context)
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action in SYSTEM_REFRESH_ACTIONS) update(context)
    }
    override fun onDisabled(context: Context) {
        try { observeOperation(WorkManager.getInstance(context).cancelUniqueWork(TaskWidgets.BOUNDARY_WORK), "Could not cancel widget boundary") }
        catch (error: Exception) { Log.e(TAG, "Could not cancel widget boundary", error) }
    }

    private fun update(context: Context) {
        try { widgetIds(context).forEach { renderState(context, it, WidgetState.LOADING) } }
        catch (error: Exception) { Log.e(TAG, "Could not show widget loading state", error) }
        TaskWidgets(context).requestUpdate()
    }

    companion object {
        private val SYSTEM_REFRESH_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_DATE_CHANGED, Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_LOCALE_CHANGED, Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}

class TaskWidgetRenderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val ids = widgetIds(applicationContext)
        if (ids.isEmpty()) return@withContext Result.success()
        val app = applicationContext as? RikoHubApp ?: return@withContext Result.failure()
        try {
            val clock = app.container.clock
            val now = clock.now()
            val zone = clock.zone()
            val snapshotClock = object : Clock {
                override fun now() = now
                override fun zone() = zone
            }
            val tasks = app.container.tasks.observeActive().first()
            val selected = selectWidgetTasks(tasks, snapshotClock)
            ids.forEach { renderTasks(applicationContext, it, selected, snapshotClock) }
            scheduleBoundary(applicationContext, tasks, snapshotClock)
            Result.success()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e(TAG, "Could not render task widgets", error)
            try { ids.forEach { renderState(applicationContext, it, WidgetState.UNAVAILABLE) } }
            catch (renderError: Exception) { Log.e(TAG, "Could not show widget error state", renderError) }
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }
}

class TaskWidgetBoundaryWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        TaskWidgets(applicationContext).requestUpdate()
        return Result.success()
    }
}

private enum class WidgetState { LOADING, UNAVAILABLE }

private fun widgetIds(context: Context) = AppWidgetManager.getInstance(context)
    .getAppWidgetIds(ComponentName(context, TaskWidgetProvider::class.java))

private fun renderTasks(context: Context, widgetId: Int, tasks: List<UrgencyInfo>, clock: Clock) {
    val views = RemoteViews(context.packageName, R.layout.task_widget)
    setHeaderIntent(context, views)
    setDate(views, clock)
    views.setImageViewResource(R.id.widget_miyu, MIYU_IMAGES.random())
    val rows = widgetRows(context, widgetId)
    views.setTextViewText(R.id.widget_state, context.getString(R.string.widget_empty))
    views.setViewVisibility(R.id.widget_state, if (tasks.isEmpty()) View.VISIBLE else View.GONE)
    val now = clock.now()
    tasks.take(rows).forEachIndexed { index, info ->
        val ids = ROW_IDS[index]
        views.setViewVisibility(ids.root, View.VISIBLE)
        views.setTextViewText(ids.title, info.task.title)
        views.setTextViewText(ids.meta, metadata(context, info, now, clock))
        views.setViewVisibility(ids.meta, if (rows == 1) View.GONE else View.VISIBLE)
        views.setImageViewResource(ids.marker, if (info.isOverdue || info.task.priority == Priority.HIGH) R.drawable.widget_coral_dot else R.drawable.widget_lavender_dot)
        views.setOnClickPendingIntent(ids.root, taskPendingIntent(context, info.task.id))
    }
    for (index in minOf(rows, tasks.size) until ROW_IDS.size) {
        val ids = ROW_IDS[index]
        views.setViewVisibility(ids.root, View.GONE)
        views.setOnClickPendingIntent(ids.root, null)
        views.setTextViewText(ids.title, "")
        views.setTextViewText(ids.meta, "")
    }
    AppWidgetManager.getInstance(context).updateAppWidget(widgetId, views)
}

private fun renderState(context: Context, widgetId: Int, state: WidgetState) {
    val views = RemoteViews(context.packageName, R.layout.task_widget)
    setHeaderIntent(context, views)
    setDate(views, (context.applicationContext as? RikoHubApp)?.container?.clock)
    views.setImageViewResource(R.id.widget_miyu, MIYU_IMAGES.first())
    views.setTextViewText(R.id.widget_state, context.getString(if (state == WidgetState.LOADING) R.string.widget_loading else R.string.widget_unavailable))
    views.setViewVisibility(R.id.widget_state, View.VISIBLE)
    ROW_IDS.forEach { row ->
        views.setViewVisibility(row.root, View.GONE)
        views.setOnClickPendingIntent(row.root, null)
    }
    AppWidgetManager.getInstance(context).updateAppWidget(widgetId, views)
}

private fun setDate(views: RemoteViews, clock: Clock?) {
    val local = Instant.ofEpochMilli(clock?.now() ?: System.currentTimeMillis()).atZone(clock?.zone() ?: java.time.ZoneId.systemDefault())
    views.setTextViewText(R.id.widget_weekday, local.format(DateTimeFormatter.ofPattern("EEEE", Locale.forLanguageTag("id-ID"))))
    views.setTextViewText(R.id.widget_date, local.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("id-ID"))))
}

private fun widgetRows(context: Context, widgetId: Int): Int {
    val manager = AppWidgetManager.getInstance(context)
    val height = manager.getAppWidgetOptions(widgetId).getInt(OPTION_APPWIDGET_MIN_HEIGHT, 272)
    val scale = context.resources.configuration.fontScale.coerceAtLeast(1f)
    return widgetTaskCapacity(height, scale)
}

private fun setHeaderIntent(context: Context, views: RemoteViews) {
    val intent = Intent(context, MainActivity::class.java).setData(Uri.parse("riko://widget/home"))
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    val pendingIntent = PendingIntent.getActivity(
        context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    listOf(R.id.widget_header, R.id.widget_clock, R.id.widget_weekday, R.id.widget_date).forEach {
        views.setOnClickPendingIntent(it, pendingIntent)
    }
}

private fun taskPendingIntent(context: Context, taskId: Long): PendingIntent {
    val intent = Intent(context, MainActivity::class.java)
        .putExtra(TaskReminderWorker.TASK_ID_EXTRA, taskId)
        .setData(Uri.parse("riko://widget/task/$taskId"))
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    return PendingIntent.getActivity(context, taskId.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

private fun metadata(context: Context, info: UrgencyInfo, now: Long, clock: Clock): String {
    val task = info.task
    val due = task.deadlineAt?.let {
        if (info.isOverdue) context.getString(R.string.widget_overdue)
        else {
            val date = Instant.ofEpochMilli(it).atZone(clock.zone())
            val today = Instant.ofEpochMilli(now).atZone(clock.zone()).toLocalDate()
            when (date.toLocalDate()) {
                today -> context.getString(R.string.today)
                today.plusDays(1) -> context.getString(R.string.tomorrow)
                else -> date.format(DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("id-ID")))
            } + if (task.hasDeadlineTime) " · " + date.format(DateTimeFormatter.ofPattern("HH.mm", Locale.forLanguageTag("id-ID"))) else ""
        }
    } ?: context.getString(R.string.no_deadline)
    val priority = context.getString(when (task.priority) {
        Priority.LOW -> R.string.priority_low
        Priority.MEDIUM -> R.string.priority_medium
        Priority.HIGH -> R.string.priority_high
    })
    return "$due · $priority"
}

private fun scheduleBoundary(context: Context, tasks: List<Task>, clock: Clock) {
    val work = WorkManager.getInstance(context)
    val ids = widgetIds(context)
    val next = nextWidgetRefreshAt(tasks, clock) ?: nextDateRefreshAt(clock.now(), clock.zone())
    if (ids.isEmpty()) {
        observeOperation(work.cancelUniqueWork(TaskWidgets.BOUNDARY_WORK), "Could not cancel widget boundary")
        return
    }
    observeOperation(work.enqueueUniqueWork(
        TaskWidgets.BOUNDARY_WORK, ExistingWorkPolicy.REPLACE,
        OneTimeWorkRequestBuilder<TaskWidgetBoundaryWorker>().setInitialDelay((next - clock.now()).coerceAtLeast(0), TimeUnit.MILLISECONDS).build(),
    ), "Could not schedule widget boundary")
}

private fun observeOperation(operation: Operation, message: String) {
    operation.result.addListener({
        try { operation.result.get() }
        catch (error: Exception) { Log.e(TAG, message, error) }
    }, Executor { it.run() })
}

private data class RowIds(val root: Int, val marker: Int, val title: Int, val meta: Int)
private val ROW_IDS = listOf(
    RowIds(R.id.widget_row_1, R.id.widget_marker_1, R.id.widget_title_1, R.id.widget_meta_1),
    RowIds(R.id.widget_row_2, R.id.widget_marker_2, R.id.widget_title_2, R.id.widget_meta_2),
    RowIds(R.id.widget_row_3, R.id.widget_marker_3, R.id.widget_title_3, R.id.widget_meta_3),
)

private const val TAG = "TaskWidgets"
private val MIYU_IMAGES = intArrayOf(
    R.drawable.widget_miyu_01, R.drawable.widget_miyu_02, R.drawable.widget_miyu_03, R.drawable.widget_miyu_04,
    R.drawable.widget_miyu_05, R.drawable.widget_miyu_06, R.drawable.widget_miyu_07, R.drawable.widget_miyu_08,
)
