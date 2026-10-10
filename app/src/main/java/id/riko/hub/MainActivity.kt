package id.riko.hub

import android.os.Bundle
import android.util.Log
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import id.riko.hub.ui.RikoHub
import id.riko.hub.work.TaskReminderWorker
import kotlinx.coroutines.launch
import java.io.IOException
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    private var taskToOpen by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        taskToOpen = intent.taskIdExtra()
        if (savedInstanceState == null) {
            lifecycleScope.launch {
                try { (application as RikoHubApp).container.settings.recordAppLaunch() }
                catch (error: IOException) { Log.e("MainActivity", "Cannot record app launch", error) }
            }
        }
        enableEdgeToEdge()
        setContent {
            RikoHub(openTaskId = taskToOpen, onTaskOpenConsumed = ::consumeTaskOpen) { dark ->
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { dark },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        taskToOpen = intent.taskIdExtra()
    }

    override fun onResume() {
        super.onResume()
        try { (application as RikoHubApp).container.reminders.requestReconciliation() }
        catch (error: Exception) { Log.e("MainActivity", "Cannot reconcile task reminders", error) }
        try { (application as RikoHubApp).container.widgets.requestUpdate() }
        catch (error: Exception) { Log.e("MainActivity", "Cannot update task widgets", error) }
    }

    private fun consumeTaskOpen() {
        taskToOpen = null
        intent.removeExtra(TaskReminderWorker.TASK_ID_EXTRA)
        intent.data = null
    }
}

private fun Intent.taskIdExtra(): Long? = (extras?.get(TaskReminderWorker.TASK_ID_EXTRA) as? Long).takeIf { it != null && it > 0L }
