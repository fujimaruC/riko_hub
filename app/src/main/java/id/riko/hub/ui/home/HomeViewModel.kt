package id.riko.hub.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.riko.hub.core.time.Clock
import id.riko.hub.data.prefs.SettingsDataStore
import id.riko.hub.data.repository.*
import id.riko.hub.domain.logic.*
import id.riko.hub.domain.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.time.Instant

data class HomeUiState(
    val snapshot: HomeSnapshot? = null, val name: String = "", val loading: Boolean = true,
    val readFailed: Boolean = false, val editor: Task? = null, val quickAdd: Boolean = false,
    val peek: Long? = null, val busy: Boolean = false,
)
sealed interface HomeEffect {
    data class Completed(val id: Long, val previous: TaskStatus) : HomeEffect
    data class Deleted(val id: Long) : HomeEffect
    data class Failed(val retry: () -> Unit) : HomeEffect
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(private val tasks: TaskRepository, private val events: EventRepository,
                    private val preferences: SettingsDataStore, private val clock: Clock) : ViewModel() {
    private val controls = MutableStateFlow(HomeUiState())
    private val active = MutableStateFlow(false)
    private val reload = MutableStateFlow(0)
    private val messages = Channel<HomeEffect>(Channel.BUFFERED)
    val effects = messages.receiveAsFlow()
    private var lastCategory = Category.PERSONAL
    private val data = combine(active, reload) { running, _ -> running }.flatMapLatest { running ->
        if (!running) emptyFlow() else flow {
            while (currentCoroutineContext().isActive) {
                emit(clock.now())
                delay(nextHomeTickDelay(clock))
            }
        }.flatMapLatest { now ->
            combine(tasks.observeAll(), events.observeBetween(now, now + 86_400_001), preferences.settings) { list, agenda, settings ->
                val snapshot = homeSnapshot(list, agenda, settings, clock)
                if (settings.pinnedFocusTaskId != null && snapshot.validPin == null) {
                    try { preferences.clearPinnedFocusIfMatches(settings.pinnedFocusTaskId, settings.pinnedFocusDate) }
                    catch (error: Exception) {
                        if (error is CancellationException) throw error
                        Log.e("HomeViewModel", "Cannot clear expired pin", error)
                        messages.send(HomeEffect.Failed(::retryRead))
                    }
                }
                HomeUiState(snapshot = snapshot, name = settings.displayName, loading = false)
            }
        }.catch { error ->
            if (error is CancellationException) throw error
            Log.e("HomeViewModel", "Home read failed", error)
            emit(HomeUiState(loading = false, readFailed = true))
        }
    }
    val state = combine(data, controls) { data, ui ->
        data.copy(editor = ui.editor, quickAdd = ui.quickAdd, peek = ui.peek?.takeIf { id -> data.snapshot?.ranked?.any { it.task.id == id } == true }, busy = ui.busy)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun setActive(value: Boolean) { active.value = value }
    fun retryRead() { reload.value++ }
    fun peek(id: Long?) { controls.update { it.copy(peek = id) } }
    fun edit(task: Task? = null) {
        if (!controls.value.busy) controls.update { it.copy(editor = task ?: Task(title = "", category = lastCategory), quickAdd = task == null, peek = null) }
    }
    fun change(task: Task) { if (!controls.value.busy) controls.update { it.copy(editor = task) } }
    fun dismissEditor() { if (!controls.value.busy) controls.update { it.copy(editor = null) } }
    fun save() {
        val task = controls.value.editor ?: return
        if (task.title.trim().length !in 1..120 || (task.notes?.length ?: 0) > 2000) return
        write(::save) {
            tasks.upsert(task)
            lastCategory = task.category
            controls.update { it.copy(editor = null) }
        }
    }
    fun complete(task: Task): Unit = write({ complete(task) }) {
        tasks.setStatus(task.id, TaskStatus.DONE)
        controls.update { it.copy(peek = null) }
        messages.send(HomeEffect.Completed(task.id, task.status))
    }
    fun start(task: Task): Unit = write({ start(task) }) { tasks.setStatus(task.id, TaskStatus.IN_PROGRESS); peek(null) }
    fun undo(id: Long, status: TaskStatus): Unit = write({ undo(id, status) }) { tasks.setStatus(id, status) }
    fun delete(task: Task): Unit = write({ delete(task) }) {
        tasks.softDelete(task.id); controls.update { it.copy(editor = null, peek = null) }; messages.send(HomeEffect.Deleted(task.id))
    }
    fun restore(id: Long): Unit = write({ restore(id) }) { tasks.restore(id) }
    fun pin(id: Long): Unit = write({ pin(id) }) {
        val snapshot = state.value.snapshot ?: return@write
        if (snapshot.validPin == id) preferences.setPinnedFocus(null, null)
        else preferences.setPinnedFocus(id, Instant.ofEpochMilli(clock.now()).atZone(clock.zone()).toLocalDate())
    }
    private fun write(retry: () -> Unit, action: suspend () -> Unit) {
        if (controls.value.busy) return
        controls.update { it.copy(busy = true) }
        viewModelScope.launch {
            try { action() }
            catch (error: Exception) {
                if (error is CancellationException) throw error
                Log.e("HomeViewModel", "Home action failed", error)
                messages.send(HomeEffect.Failed(retry))
            } finally { controls.update { it.copy(busy = false) } }
        }
    }
}
