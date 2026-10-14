package id.riko.hub.ui.spaces

import id.riko.hub.data.remote.device.CompanionSnapshot
import id.riko.hub.data.remote.device.HandoffProgress
import id.riko.hub.data.remote.device.HandoffSnapshot
import id.riko.hub.data.remote.device.TrustedLaptop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DevicesBridgeStateTest {
    private val trusted = TrustedLaptop("peer", "Laptop", "pin", "token", "phone")

    @Test fun bridgeStateReflectsPairingNetworkConnectionTransferAndErrors() {
        assertEquals(DeviceBridgeState.UNPAIRED, deviceBridgeState(CompanionSnapshot(networkReady = true), HandoffSnapshot()))
        assertEquals(DeviceBridgeState.OFFLINE, deviceBridgeState(CompanionSnapshot(), HandoffSnapshot()))
        assertEquals(DeviceBridgeState.PAIRING, deviceBridgeState(CompanionSnapshot(networkReady = true, pairing = true), HandoffSnapshot()))
        assertEquals(DeviceBridgeState.OFFLINE, deviceBridgeState(CompanionSnapshot(trusted = trusted), HandoffSnapshot()))
        assertEquals(DeviceBridgeState.OFFLINE, deviceBridgeState(CompanionSnapshot(trusted = trusted, connected = true), HandoffSnapshot(connected = true)))
        assertEquals(DeviceBridgeState.PAIRING, deviceBridgeState(CompanionSnapshot(trusted = trusted, networkReady = true, pairing = true), HandoffSnapshot()))
        assertEquals(DeviceBridgeState.DISCONNECTED, deviceBridgeState(CompanionSnapshot(trusted = trusted, networkReady = true), HandoffSnapshot()))
        assertEquals(DeviceBridgeState.CONNECTED, deviceBridgeState(CompanionSnapshot(trusted = trusted, networkReady = true, connected = true), HandoffSnapshot()))
        assertEquals(DeviceBridgeState.TRANSFERRING, deviceBridgeState(
            CompanionSnapshot(trusted = trusted, networkReady = true, connected = true),
            HandoffSnapshot(connected = true, busy = true, progress = HandoffProgress(null, "file", 0, 0)),
        ))
        assertEquals(DeviceBridgeState.CONNECTED, deviceBridgeState(
            CompanionSnapshot(trusted = trusted, networkReady = true, connected = true),
            HandoffSnapshot(connected = false, busy = true, progress = HandoffProgress(null, "file", 10, 100)),
        ))
        assertEquals(DeviceBridgeState.ERROR, deviceBridgeState(CompanionSnapshot(trusted = trusted, error = "network"), HandoffSnapshot()))
    }

    @Test fun progressRatioUsesRealBytesAndClampsInvalidRanges() {
        assertEquals(.25f, deviceTransferProgress(HandoffProgress(null, "file", 25, 100))!!, .001f)
        assertEquals(0f, deviceTransferProgress(HandoffProgress(null, "file", -1, 100))!!, .001f)
        assertEquals(1f, deviceTransferProgress(HandoffProgress(null, "file", 120, 100))!!, .001f)
        assertNull(deviceTransferProgress(HandoffProgress(null, "file", 1, 0)))
        assertNull(deviceTransferProgress(null))
    }
}
