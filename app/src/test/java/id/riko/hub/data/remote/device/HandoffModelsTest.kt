package id.riko.hub.data.remote.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HandoffModelsTest {
    private fun transfer(id: String, direction: String = "phone_to_laptop") = HandoffTransfer(
        id, direction, "failed", "name", "application/octet-stream", 0, "0".repeat(64),
        0, 0, null, false, interrupted = true
    )

    @Test fun uploadRetryIsBoundToTheSinglePendingOutgoingTransfer() {
        val pending = "123e4567-e89b-12d3-a456-426614174000"
        val olderFailure = "123e4567-e89b-12d3-a456-426614174001"
        assertTrue(canRetryHandoffUpload(transfer(pending), pending))
        assertFalse(canRetryHandoffUpload(transfer(olderFailure), pending))
        assertFalse(canRetryHandoffUpload(transfer(pending, "laptop_to_phone"), pending))
        assertFalse(canRetryHandoffUpload(transfer(pending), null))
    }

    @Test fun namesTruncateOnUtf8BoundaryAndSanitizeControlsAndUnsafeSeparators() {
        val result = handoffSafeName("a".repeat(126) + "éz")
        assertEquals(128, result.toByteArray(Charsets.UTF_8).size)
        assertEquals("_x__", handoffSafeName("\u00a0x/\u001b"))
        val emojiBoundary = handoffSafeName("a".repeat(127) + "😀")
        assertEquals(127, emojiBoundary.length)
        assertEquals(127, emojiBoundary.toByteArray(Charsets.UTF_8).size)
    }
}
