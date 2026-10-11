package id.riko.hub.ui.vault

import id.riko.hub.data.remote.drive.DriveFile
import id.riko.hub.data.remote.drive.DrivePage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultPageTest {
    @Test fun pagingStopsAtFiveHundredItems() {
        val existing = (1..490).map { file(it) }
        val incoming = DrivePage((491..510).map(::file), "next")

        val (files, limited) = appendVaultPage(existing, incoming, reset = false)

        assertEquals(500, files.size)
        assertEquals("f500", files.last().id)
        assertTrue(limited)
    }

    @Test fun refreshReplacesPreviousPage() {
        val previous = listOf(file(1), file(2))
        val refreshed = DrivePage(listOf(file(3)), "next")

        val (files, limited) = appendVaultPage(previous, refreshed, reset = true)

        assertEquals(listOf("f3"), files.map(DriveFile::id))
        assertFalse(limited)
    }

    @Test fun staleUploadCannotOwnANewGrantOrSession() {
        assertTrue(isCurrentUpload(4, 7, 4, 7, "old-token", "old-token"))
        assertFalse(isCurrentUpload(4, 7, 4, 8, "old-token", "new-token"))
        assertFalse(isCurrentUpload(4, 7, 5, 7, "old-token", "old-token"))
    }

    private fun file(id: Int) = DriveFile(id = "f$id", name = "File $id", mimeType = "text/plain")
}
