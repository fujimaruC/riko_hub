package id.riko.hub.ui.sync

import id.riko.hub.data.remote.device.CompanionSnapshot

data class SyncUiState(val companion: CompanionSnapshot = CompanionSnapshot(), val forgetConfirmation: Boolean = false, val resetConfirmation: Boolean = false)
