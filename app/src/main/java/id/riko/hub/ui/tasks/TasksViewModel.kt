package id.riko.hub.ui.tasks

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.riko.hub.core.time.Clock
import id.riko.hub.data.repository.TaskRepository
import id.riko.hub.domain.logic.*
import id.riko.hub.domain.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.time.Instant
import java.time.ZoneId

data class TasksUiState(
    val tasks: List<Task> = emptyList(),
    val buckets: Map<TaskBucket, List<Task>> = emptyMap(),
    val filter: Category? = null,
    val query: String = "",
    val doneExpanded: Boolean = false,
    val loading: Boolean = true,
    val readFailed: Boolean = false,
    val editor: Task? = null,
    val busy: Boolean = false,
    val now: Long = 0,
    val zone: ZoneId = ZoneId.of("UTC"),
    val todayCount: Int = 0,
)

sealed interface TaskEffect {
    data class Deleted(val id: Long) : TaskEffect
    data class Failed(val retry: () -> Unit) : TaskEffect
}

private data class TaskLoad(val tasks: List<Task> = emptyList(), val loading: Boolean = true, val failed: Boolean = false)

@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModel(private val repository: TaskRepository, private val clock: Clock) : ViewModel() {
    private val initial = TasksUiState(now = clock.now(), zone = clock.zone())
    private val controls = MutableStateFlow(initial)
    private val reload = MutableStateFlow(0)
    private val messages = Channel<TaskEffect>(Channel.BUFFERED)
    val effects = messages.receiveAsFlow()
    private var lastCategory = Category.PERSONAL
    private val data = reload.flatMapLatest {
        repository.observeAll().map { TaskLoad(it, loading = false) }
            .onStart { emit(TaskLoad()) }
            .catch { error ->
                if (error is CancellationException) throw error
                Log.e("TasksViewModel", "Task read failed", error)
                emit(TaskLoad(loading = false, failed = true))
            }
    }
    private val ticks = flow {
        while (currentCoroutineContext().isActive) {
            emit(clock.now() to clock.zone())
            delay(30_000)
        }
    }
    val state: StateFlow<TasksUiState> = combine(data, controls, ticks) { loaded, ui, time ->
        val (now, zone) = time
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        ui.copy(tasks = loaded.tasks, buckets = groupTasks(loaded.tasks, ui.filter, now, zone, ui.query),
            loading = loaded.loading, readFailed = loaded.failed, now = now, zone = zone,
            todayCount = loaded.tasks.count { task -> task.deadlineAt?.let {
                Instant.ofEpochMilli(it).atZone(zone).toLocalDate() == today
            } == true })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    fun retryRead() { reload.value++ }
    fun filter(category: Category?) { controls.update { it.copy(filter = category) } }
    fun search(query: String) { controls.update { it.copy(query = query) } }
    fun clearFilters() { controls.update { it.copy(query = "", filter = null) } }
    fun toggleDone() { controls.update { it.copy(doneExpanded = !it.doneExpanded) } }
    fun edit(task: Task? = null) {
        if (!controls.value.busy) controls.update { it.copy(editor = task ?: Task(title = "", category = lastCategory)) }
    }
    fun change(task: Task) { if (!controls.value.busy) controls.update { it.copy(editor = task) } }
    fun dismissEditor() { if (!controls.value.busy) controls.update { it.copy(editor = null) } }
    fun save() {
        val task = controls.value.editor ?: return
        if (task.title.trim().length !in 1..120 || (task.notes?.length ?: 0) > 2000) return
        write(::save) {
            repository.upsert(task)
            lastCategory = task.category
            controls.update { it.copy(editor = null) }
        }
    }
    fun complete(task: Task, done: Boolean = task.status != TaskStatus.DONE): Unit = write({ complete(task, done) }) {
        repository.setStatus(task.id, if (done) TaskStatus.DONE else TaskStatus.NOT_STARTED)
    }
    fun delete(task: Task): Unit = write({ delete(task) }) {
        repository.softDelete(task.id)
        controls.update { it.copy(editor = null) }
        messages.send(TaskEffect.Deleted(task.id))
    }
    fun restore(id: Long): Unit = write({ restore(id) }) { repository.restore(id) }

    private fun write(retry: () -> Unit, operation: suspend () -> Unit) {
        if (controls.value.busy) return
        controls.update { it.copy(busy = true) }
        viewModelScope.launch {
            try { operation() }
            catch (error: Exception) {
                if (error is CancellationException) throw error
                Log.e("TasksViewModel", "Task action failed", error)
                messages.send(TaskEffect.Failed(retry))
            } finally { controls.update { it.copy(busy = false) } }
        }
    }
}
