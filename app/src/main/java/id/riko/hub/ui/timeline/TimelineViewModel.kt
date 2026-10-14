package id.riko.hub.ui.timeline

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.riko.hub.core.time.Clock
import id.riko.hub.data.repository.*
import id.riko.hub.domain.logic.*
import id.riko.hub.domain.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.time.*

data class TimelineUiState(
    val weekStart: LocalDate = LocalDate.of(1970, 1, 5),
    val selectedDay: LocalDate? = null,
    val dayMode: Boolean = false,
    val today: LocalDate = LocalDate.of(1970, 1, 1),
    val now: Long = 0, val zone: ZoneId = ZoneId.of("UTC"),
    val days: List<TimelineDay> = emptyList(), val loading: Boolean = true,
    val readFailed: Boolean = false, val busy: Boolean = false,
    val eventEditor: Event? = null, val taskEditor: Task? = null,
)
sealed interface TimelineEffect {
    data object Clear : TimelineEffect
    data class Deleted(val id: Long, val event: Boolean) : TimelineEffect
    data class Failed(val retry: () -> Unit) : TimelineEffect
}

@OptIn(ExperimentalCoroutinesApi::class)
class TimelineViewModel(private val tasks: TaskRepository, private val events: EventRepository,
                        private val clock: Clock) : ViewModel() {
    private val initial = clock.now().let { now ->
        val zone = clock.zone()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        TimelineUiState(weekStart = timelineWeek(today), today = today, now = now, zone = zone)
    }
    private val controls = MutableStateFlow(initial)
    private val selectedDate = MutableStateFlow<LocalDate?>(null)
    private val dayMode = MutableStateFlow(false)
    private val active = MutableStateFlow(false)
    private val reload = MutableStateFlow(0)
    private val messages = Channel<TimelineEffect>(Channel.BUFFERED)
    val effects = messages.receiveAsFlow()
    private val data = combine(active, reload) { started, _ -> started }.flatMapLatest { started ->
        if (!started) emptyFlow() else flow {
            while (currentCoroutineContext().isActive) {
                emit(clock.now() to clock.zone())
                val midnight = Instant.ofEpochMilli(clock.now()).atZone(clock.zone()).toLocalDate().plusDays(1)
                    .atStartOfDay(clock.zone()).toInstant().toEpochMilli()
                delay(minOf(60_000L, midnight - clock.now()).coerceAtLeast(1))
            }
        }.combine(selectedDate) { time, chosen -> time to chosen }
            .combine(dayMode) { (time, chosen), isDay -> Triple(time.first, time.second, isDay) to chosen }
            .flatMapLatest { (timeAndMode, chosen) ->
                val (now, zone, isDay) = timeAndMode
                val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
                val cursor = chosen ?: today
                val week = timelineWeek(cursor)
                val initial = TimelineUiState(weekStart = week, selectedDay = if (isDay) cursor else null,
                    dayMode = isDay, today = today, now = now, zone = zone)
                combine(tasks.observeAll(), events.observeBetween(week.atStartOfDay(zone).toInstant().toEpochMilli(),
                    week.plusWeeks(1).atStartOfDay(zone).toInstant().toEpochMilli())) { taskList, eventList ->
                    val days = buildTimeline(taskList, eventList, week, zone)
                    initial.copy(days = if (isDay) days.filter { it.date == cursor } else days, loading = false)
                }.catch { error ->
                    if (error is CancellationException) throw error
                    Log.e("TimelineViewModel", "Timeline read failed", error)
                    emit(initial.copy(loading = false, readFailed = true))
                }
            }
    }
    val state = combine(data, controls) { data, ui ->
        data.copy(busy = ui.busy, eventEditor = ui.eventEditor, taskEditor = ui.taskEditor)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    fun setActive(value: Boolean) { active.value = value }
    fun retryRead() { reload.value++ }
    fun moveWeek(direction: Long) {
        val date = selectedDate.value ?: Instant.ofEpochMilli(clock.now()).atZone(clock.zone()).toLocalDate()
        selectedDate.value = if (dayMode.value) date.plusDays(direction) else timelineWeek(date).plusWeeks(direction)
    }
    fun selectDay(date: LocalDate) {
        selectedDate.value = date
        dayMode.value = true
    }
    fun toggleDayMode() {
        if (!dayMode.value && selectedDate.value == null) {
            selectedDate.value = Instant.ofEpochMilli(clock.now()).atZone(clock.zone()).toLocalDate()
        }
        dayMode.value = !dayMode.value
    }
    fun currentWeek() { selectedDate.value = null; reload.value++ }
    fun open(stop: TimelineStop) {
        if (!controls.value.busy) controls.update { it.copy(eventEditor = stop.event, taskEditor = stop.task) }
    }
    fun addEvent() {
        if (controls.value.busy) return
        val zone = clock.zone()
        val current = Instant.ofEpochMilli(clock.now()).atZone(zone).withSecond(0).withNano(0)
        val date = state.value.selectedDay ?: state.value.weekStart
        val startDate = if (!state.value.dayMode && current.toLocalDate() in state.value.weekStart..state.value.weekStart.plusDays(6))
            current.toLocalDate() else date
        val start = if (current.toLocalDate() == startDate) current else startDate.atTime(9, 0).atZone(zone)
        controls.update { it.copy(eventEditor = Event(title = "", startAt = start.toInstant().toEpochMilli()), taskEditor = null) }
    }
    fun changeEvent(event: Event) { if (!controls.value.busy) controls.update { it.copy(eventEditor = event) } }
    fun changeTask(task: Task) { if (!controls.value.busy) controls.update { it.copy(taskEditor = task) } }
    fun dismiss() { if (!controls.value.busy) controls.update { it.copy(eventEditor = null, taskEditor = null) } }
    fun saveEvent() { controls.value.eventEditor?.let(::saveEventDraft) }
    private fun saveEventDraft(event: Event) {
        if (event.title.trim().length !in 1..120 || (event.notes?.length ?: 0) > 2000 ||
            (event.endAt != null && event.endAt < event.startAt)) return
        write({ saveEventDraft(event) }) {
            events.upsert(event)
            controls.update { if (it.eventEditor == event) it.copy(eventEditor = null) else it }
        }
    }
    fun saveTask() { controls.value.taskEditor?.let(::saveTaskDraft) }
    private fun saveTaskDraft(task: Task) {
        if (task.title.trim().length !in 1..120 || (task.notes?.length ?: 0) > 2000) return
        write({ saveTaskDraft(task) }) {
            tasks.upsert(task)
            controls.update { if (it.taskEditor == task) it.copy(taskEditor = null) else it }
        }
    }
    fun deleteEvent() { controls.value.eventEditor?.let { delete(it.id, true) } }
    fun deleteTask() { controls.value.taskEditor?.let { delete(it.id, false) } }
    private fun delete(id: Long, event: Boolean): Unit = write({ delete(id, event) }) {
        if (event) events.softDelete(id) else tasks.softDelete(id)
        controls.update { ui -> if (event && ui.eventEditor?.id == id) ui.copy(eventEditor = null)
            else if (!event && ui.taskEditor?.id == id) ui.copy(taskEditor = null) else ui }
        messages.send(TimelineEffect.Deleted(id, event))
    }
    fun restore(id: Long, event: Boolean): Unit = write({ restore(id, event) }) {
        if (event) events.restore(id) else tasks.restore(id)
    }
    private var writeGeneration = 0L
    private fun write(retry: () -> Unit, action: suspend () -> Unit) {
        if (controls.value.busy) return
        val generation = ++writeGeneration
        messages.trySend(TimelineEffect.Clear)
        controls.update { it.copy(busy = true) }
        viewModelScope.launch {
            try { action() }
            catch (error: Exception) {
                if (error is CancellationException) throw error
                Log.e("TimelineViewModel", "Timeline action failed", error)
                messages.send(TimelineEffect.Failed { if (generation == writeGeneration) retry() })
            } finally { controls.update { it.copy(busy = false) } }
        }
    }
}
