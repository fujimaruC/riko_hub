package id.riko.hub.ui.sync

import id.riko.hub.data.remote.device.CompanionSnapshot
import id.riko.hub.data.remote.device.HandoffSnapshot

data class SyncUiState(
    val companion: CompanionSnapshot = CompanionSnapshot(),
    val forgetConfirmation: Boolean = false,
    val resetConfirmation: Boolean = false,
    val handoff: HandoffSnapshot = HandoffSnapshot(),
    val clearHandoffConfirmation: Boolean = false
)
