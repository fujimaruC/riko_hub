package id.riko.hub.ui.sync

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.riko.hub.data.remote.device.CompanionRepository
import id.riko.hub.data.remote.device.HandoffRepository
import id.riko.hub.data.remote.device.Laptop
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withTimeout

class SyncViewModel(private val repository: CompanionRepository, private val handoffRepository: HandoffRepository) : ViewModel() {
    private val confirmation = MutableStateFlow(false)
    private val resetConfirmation = MutableStateFlow(false)
    private val clearHandoffConfirmation = MutableStateFlow(false)
    private val operationMessage = MutableStateFlow<String?>(null)
    private val handoffBusy = MutableStateFlow(false)
    private var handoffJob: Job? = null
    private val combined = combine(repository.state, confirmation, resetConfirmation, handoffRepository.state) { companion, confirm, reset, handoff ->
        SyncUiState(companion, confirm, reset, handoff)
    }
    val state = combine(combined, clearHandoffConfirmation, operationMessage, handoffBusy) { current, clear, message, busy ->
        current.copy(handoff = current.handoff.copy(busy = current.handoff.busy || busy,
            error = message ?: current.handoff.error), clearHandoffConfirmation = clear)
    }

    fun load() { viewModelScope.launch { repository.load() } }
    fun setActive(active: Boolean) {
        if (!active) cancelTransfer()
        repository.setActive(active)
        handoffRepository.setActive(active)
    }
    fun refresh() = repository.refresh()
    fun refreshHandoff() = handoff { handoffRepository.refresh() }
    fun pair(laptop: Laptop, payload: String) = repository.pair(laptop, payload)
    fun connect() = repository.connect()
    fun disconnect() = repository.disconnect()
    fun requestForget() { confirmation.value = true }
    fun cancelForget() { confirmation.value = false }
    fun forget() { confirmation.value = false; viewModelScope.launch { repository.forget() } }
    fun requestResetTrust() { resetConfirmation.value = true }
    fun cancelResetTrust() { resetConfirmation.value = false }
    fun resetLocalTrust() { resetConfirmation.value = false; viewModelScope.launch { repository.resetLocalTrust() } }

    fun prepareSend(uri: Uri) = handoff { handoffRepository.prepareSend(uri) }
    fun sendPrepared() = handoff { handoffRepository.sendPrepared() }
    fun receive(transferId: String, uri: Uri, expectedPeerId: String) = handoff {
        var completed = false
        var destinationHandled = false
        var receiveStarted = false
        try {
            try { withTimeout(15_000) { repository.state.first { it.connected } } }
            catch (_: TimeoutCancellationException) {
                val removed = handoffRepository.discardFreshDestination(uri)
                destinationHandled = true
                throw IllegalStateException(if (removed) "The laptop did not reconnect; the new destination was removed. Retry after reconnecting."
                    else "The laptop did not reconnect; remove the empty destination before retrying.")
            }
            check(repository.state.value.trusted?.id == expectedPeerId) { "The trusted laptop changed. Refresh offers before receiving." }
            receiveStarted = true
            handoffRepository.retryReceive(transferId, uri)
            completed = true
        } finally {
            if (!completed && !destinationHandled && !receiveStarted && !handoffRepository.discardFreshDestination(uri))
                operationMessage.value = "The new destination could not be removed. Delete the empty file before retrying."
        }
    }
    fun sendText(text: String, onSent: () -> Unit) = handoff(onSuccess = onSent) { handoffRepository.sendText(text) }
    fun previewText(id: String) = handoff { handoffRepository.previewText(id) }
    fun consumeText(id: String) = handoff { handoffRepository.consumeText(id) }
    fun retryUpload(transferId: String) = handoff { handoffRepository.retry(transferId) }
    fun cancelTransfer() { handoffJob?.cancel(); handoffRepository.cancel() }
    fun removeFreshDestination(uri: Uri) { viewModelScope.launch { handoffRepository.discardFreshDestination(uri) } }
    fun discardPrepared() = handoff { handoffRepository.discardPrepared() }
    fun clearHistory() = handoffRepository.clearHistory()
    fun requestClearHandoff() { clearHandoffConfirmation.value = true }
    fun cancelClearHandoff() { clearHandoffConfirmation.value = false }
    fun clearHandoffStorage() {
        clearHandoffConfirmation.value = false
        handoff { handoffRepository.clearLocalStorage() }
    }

    private fun handoff(onSuccess: () -> Unit = {}, block: suspend () -> Unit) {
        if (handoffJob?.isActive == true) return
        operationMessage.value = null
        handoffBusy.value = true
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            val thisJob = currentCoroutineContext()[Job]
            try { block(); onSuccess() }
            catch (error: TimeoutCancellationException) { operationMessage.value = error.message ?: "The laptop did not reconnect in time. Retry when it is available." }
            catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                val message = error.message ?: "Handoff failed. Retry when the laptop is connected."
                operationMessage.value = operationMessage.value?.let { "$message $it" } ?: message
            }
            finally {
                if (handoffJob === thisJob) { handoffBusy.value = false; handoffJob = null }
            }
        }
        handoffJob = job
        job.start()
    }
}
