package id.riko.hub.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.riko.hub.data.remote.device.CompanionRepository
import id.riko.hub.data.remote.device.Laptop
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SyncViewModel(private val repository: CompanionRepository) : ViewModel() {
    private val confirmation = MutableStateFlow(false)
    private val resetConfirmation = MutableStateFlow(false)
    val state = combine(repository.state, confirmation, resetConfirmation) { companion, confirm, reset -> SyncUiState(companion, confirm, reset) }
    fun load() { viewModelScope.launch { repository.load() } }
    fun setActive(active: Boolean) = repository.setActive(active)
    fun refresh() = repository.refresh()
    fun pair(laptop: Laptop, payload: String) = repository.pair(laptop, payload)
    fun connect() = repository.connect()
    fun disconnect() = repository.disconnect()
    fun requestForget() { confirmation.value = true }
    fun cancelForget() { confirmation.value = false }
    fun forget() { confirmation.value = false; viewModelScope.launch { repository.forget() } }
    fun requestResetTrust() { resetConfirmation.value = true }
    fun cancelResetTrust() { resetConfirmation.value = false }
    fun resetLocalTrust() { resetConfirmation.value = false; viewModelScope.launch { repository.resetLocalTrust() } }
}
