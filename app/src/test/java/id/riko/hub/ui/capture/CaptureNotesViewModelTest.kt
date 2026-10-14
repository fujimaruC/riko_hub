package id.riko.hub.ui.capture

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CaptureNotesViewModelTest {
    @Test fun submission_is_single_while_busy_and_result_waits_for_consumption() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val saved = mutableListOf<String>()
            val model = CaptureNotesViewModel { saved.add(it) }
            model.save("  Idea  ")
            model.save("Duplicate")
            assertTrue(model.state.value.busy)
            advanceUntilIdle()
            assertEquals(listOf("Idea"), saved)
            assertEquals("Idea", model.state.value.savedBody)
            assertFalse(model.state.value.busy)
            model.save("Unconsumed duplicate")
            advanceUntilIdle()
            assertEquals(listOf("Idea"), saved)
            model.consumeSaved()
            assertNull(model.state.value.savedBody)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun failed_submission_can_retry_and_invalid_body_never_writes() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var fail = true
            var writes = 0
            val model = CaptureNotesViewModel { writes++; if (fail) error("Storage unavailable") }
            model.save("")
            model.save("x".repeat(10_001))
            advanceUntilIdle()
            assertEquals(0, writes)
            model.save("Keep this draft")
            advanceUntilIdle()
            assertTrue(model.state.value.failed)
            assertNull(model.state.value.savedBody)
            fail = false
            model.save("Keep this draft")
            advanceUntilIdle()
            assertEquals("Keep this draft", model.state.value.savedBody)
            assertEquals(2, writes)
        } finally { Dispatchers.resetMain() }
    }
}
