package id.riko.hub.data.remote.device

import org.junit.Assert.*
import org.junit.Test
import java.net.InetAddress
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest

class CompanionProtocolTest {
    @Test fun pairingPayloadRequiresExactVersionUuidAndLowercaseHex() {
        val id = "123e4567-e89b-12d3-a456-426614174000"
        val hex = "a".repeat(64)
        assertEquals(PairingPayload(id, hex, hex), parsePairingPayload("riko1:$id:$hex:$hex"))
        assertNull(parsePairingPayload("riko2:$id:$hex:$hex"))
        assertNull(parsePairingPayload("riko1:$id:${"A".repeat(64)}:$hex"))
        assertNull(parsePairingPayload("riko1:not-a-uuid:$hex:$hex"))
    }

    @Test fun onlyRfc1918Ipv4AddressesAreAccepted() {
        listOf("10.1.2.3", "172.16.0.1", "172.31.255.1", "192.168.1.4").forEach {
            assertTrue(it, isPrivateIpv4(InetAddress.getByName(it)))
        }
        listOf("172.32.0.1", "192.0.2.1", "8.8.8.8", "127.0.0.1").forEach {
            assertFalse(it, isPrivateIpv4(InetAddress.getByName(it)))
        }
        assertFalse(isPrivateIpv4(InetAddress.getByName("fd00::1")))
    }

    @Test fun retryScheduleCapsAtSixtySeconds() {
        assertEquals(2L, retryDelaySeconds(0))
        assertEquals(4L, retryDelaySeconds(2))
        assertEquals(60L, retryDelaySeconds(6))
        assertEquals(60L, retryDelaySeconds(100))
    }

    @Test fun pairingInvalidatesAnOlderInFlightTrustRead() = runTest {
        val epoch = CompanionTrustEpoch()
        val readResult = CompletableDeferred<String?>()
        var displayedTrust: String? = null
        val loading = launch(start = CoroutineStart.UNDISPATCHED) {
            val readEpoch = epoch.snapshot()
            val result = readResult.await()
            if (epoch.isCurrent(readEpoch)) displayedTrust = result
        }

        epoch.invalidate() // Pairing begins; an older null read must no longer publish.
        displayedTrust = "validated-laptop"
        readResult.complete(null)
        loading.join()

        assertEquals("validated-laptop", displayedTrust)
    }

    @Test fun trustRemovalCompletionInvalidatesReadsStartedDuringRemoval() = runTest {
        val epoch = CompanionTrustEpoch()
        epoch.invalidate() // reset or forget has begun
        val readResult = CompletableDeferred<String?>()
        var displayedTrust: String? = null
        val loading = launch(start = CoroutineStart.UNDISPATCHED) {
            val readEpoch = epoch.snapshot()
            val result = readResult.await()
            if (epoch.isCurrent(readEpoch)) displayedTrust = result
        }

        epoch.invalidate() // Disk removal completed; old bytes must no longer publish.
        displayedTrust = null
        readResult.complete("removed-laptop")
        loading.join()

        assertNull(displayedTrust)
    }
}
