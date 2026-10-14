package id.riko.hub.ui.academic

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import id.riko.hub.data.repository.AssignmentRepository
import id.riko.hub.data.repository.CourseRepository
import id.riko.hub.data.repository.TaskRepository
import id.riko.hub.domain.logic.allowedAcademicTransitions
import id.riko.hub.domain.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AssignmentUiState(
    val assignment: AcademicAssignment? = null,
    val task: Task? = null,
    val courses: List<Course> = emptyList(),
    val loading: Boolean = true,
    val readFailed: Boolean = false,
    val busy: Boolean = false,
    val writeFailed: Boolean = false,
    val writeSucceeded: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class AssignmentViewModel(
    private val task: Task,
    private val assignments: AssignmentRepository,
    courses: CourseRepository,
    tasks: TaskRepository,
) : ViewModel() {
    private val controls = MutableStateFlow(AssignmentUiState())
    private val reload = MutableStateFlow(0)
    private val source = reload.flatMapLatest {
        combine(assignments.observeByTask(task.id), courses.observeAll(), tasks.observeById(task.id)) { assignment, allCourses, savedTask ->
            controls.value.copy(assignment = assignment, task = savedTask, courses = allCourses, loading = false, readFailed = false)
        }.onStart { emit(controls.value.copy(loading = true, readFailed = false)) }
            .catch { error ->
                if (error is CancellationException) throw error
                Log.e("AssignmentViewModel", "Assignment read failed", error)
                emit(controls.value.copy(loading = false, readFailed = true))
            }
    }
    val state = combine(source, controls) { loaded, ui -> loaded.copy(busy = ui.busy, writeFailed = ui.writeFailed,
        writeSucceeded = ui.writeSucceeded) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AssignmentUiState())

    fun retryRead() { reload.value++ }
    fun save(courseId: Long?, state: AcademicState, reference: String?) {
        if (controls.value.busy || task.id == 0L || (reference?.length ?: 0) > 2000) return
        controls.update { it.copy(busy = true, writeFailed = false, writeSucceeded = false) }
        viewModelScope.launch {
            try {
                assignments.save(task.id, courseId, state, reference)
                controls.update { it.copy(writeSucceeded = true) }
            }
            catch (error: Exception) {
                if (error is CancellationException) throw error
                Log.e("AssignmentViewModel", "Assignment save failed", error)
                controls.update { it.copy(writeFailed = true) }
            } finally { controls.update { it.copy(busy = false) } }
        }
    }

    fun transitions(current: AcademicState) = setOf(current) + allowedAcademicTransitions(current)
}
