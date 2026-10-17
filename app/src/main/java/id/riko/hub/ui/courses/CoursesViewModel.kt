package id.riko.hub.ui.courses

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.riko.hub.data.repository.CourseRepository
import id.riko.hub.domain.model.Course
import id.riko.hub.domain.model.normalizeAndValidateCourse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CoursesUiState(
    val courses: List<Course> = emptyList(), val loading: Boolean = true, val readFailed: Boolean = false,
    val selectedId: Long? = null, val editor: Course? = null, val busy: Boolean = false, val showArchived: Boolean = false,
)
private data class CourseLoad(val courses: List<Course> = emptyList(), val loading: Boolean = true, val failed: Boolean = false)

@OptIn(ExperimentalCoroutinesApi::class)
class CoursesViewModel(private val repository: CourseRepository) : ViewModel() {
    private val controls = MutableStateFlow(CoursesUiState())
    private val reload = MutableStateFlow(0)
    private val failures = Channel<(() -> Unit)?>(Channel.BUFFERED)
    val writeFailures = failures.receiveAsFlow()
    private val courses = reload.flatMapLatest {
        repository.observeAll().map { CourseLoad(it, loading = false) }.onStart { emit(CourseLoad()) }
            .catch { error ->
                if (error is CancellationException) throw error
                Log.e("CoursesViewModel", "Course read failed", error)
                emit(CourseLoad(loading = false, failed = true))
            }
    }
    val state = combine(courses, controls) { loaded, ui ->
        ui.copy(courses = loaded.courses, loading = loaded.loading, readFailed = loaded.failed)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CoursesUiState())

    fun retryRead() { reload.value++ }
    fun showArchived(show: Boolean) { controls.update { it.copy(showArchived = show) } }
    fun select(id: Long?) { controls.update { it.copy(selectedId = id) } }
    fun back() { if (controls.value.editor != null) dismissEditor() else select(null) }
    fun edit(course: Course? = null) {
        if (!controls.value.busy) controls.update { it.copy(editor = course ?: Course(title = "")) }
    }
    fun change(course: Course) { if (!controls.value.busy) controls.update { it.copy(editor = course) } }
    fun dismissEditor() { if (!controls.value.busy) controls.update { it.copy(editor = null) } }
    fun save() {
        val course = controls.value.editor ?: return
        if (!validCourse(course)) return
        write(null) {
            val id = repository.upsert(course.copy(title = course.title.trim()))
            controls.update { it.copy(editor = null, selectedId = id) }
        }
    }
    fun archive(course: Course, archived: Boolean): Unit = write({ archive(course, archived) }) { repository.setArchived(course.id, archived) }
    fun move(course: Course, direction: Int): Unit = write({ move(course, direction) }) { repository.move(course.id, direction) }

    private fun write(retry: (() -> Unit)?, operation: suspend () -> Unit) {
        if (controls.value.busy) return
        controls.update { it.copy(busy = true) }
        viewModelScope.launch {
            try { operation() }
            catch (error: Exception) {
                if (error is CancellationException) throw error
                Log.e("CoursesViewModel", "Course action failed", error)
                failures.send(retry)
            } finally { controls.update { it.copy(busy = false) } }
        }
    }
}

fun validCourse(course: Course): Boolean = runCatching { normalizeAndValidateCourse(course) }.isSuccess
