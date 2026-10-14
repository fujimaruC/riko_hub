package id.riko.hub.ui.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class CaptureNoteState(val busy: Boolean = false, val failed: Boolean = false, val savedBody: String? = null)

/** A note submission survives configuration changes, just like the task editor's save. */
internal class CaptureNotesViewModel(private val saveIdea: suspend (String) -> Unit) : ViewModel() {
    private val mutableState = MutableStateFlow(CaptureNoteState())
    val state = mutableState.asStateFlow()

    fun save(raw: String) {
        val body = raw.trim()
        if (mutableState.value.busy || mutableState.value.savedBody != null || body.isEmpty() || body.length > 10_000) return
        mutableState.value = CaptureNoteState(busy = true)
        viewModelScope.launch {
            try {
                saveIdea(body)
                mutableState.value = CaptureNoteState(savedBody = body)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                mutableState.value = CaptureNoteState(failed = true)
            } finally { mutableState.update { it.copy(busy = false) } }
        }
    }

    fun consumeSaved() { mutableState.update { it.copy(savedBody = null) } }
}
