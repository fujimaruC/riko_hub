package id.riko.hub.data.remote.drive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.json.JSONObject

class DriveApiTest {
    @Test fun queryLiteralsEscapeBackslashesAndApostrophes() {
        assertEquals("folder\\'s \\\\ archive", escapeDriveQuery("folder's \\ archive"))
    }

    @Test fun fileMetadataIdentifiesFolders() {
        val folder = DriveFile("id", "Folder", "application/vnd.google-apps.folder", canAddChildren = true)
        val document = DriveFile("id2", "Doc", "application/vnd.google-apps.document", webViewLink = "https://drive.google.com/doc")
        assertTrue(folder.isFolder)
        assertTrue(folder.canAddChildren)
        assertFalse(document.isFolder)
        assertEquals("https://drive.google.com/doc", document.webViewLink)
    }

    @Test fun errorStatusesMapToDriveKinds() {
        assertEquals(DriveErrorKind.AUTH, classifyDriveError(401, ""))
        assertEquals(DriveErrorKind.PERMISSION, classifyDriveError(403, "insufficientPermissions"))
        assertEquals(DriveErrorKind.QUOTA, classifyDriveError(403, "userRateLimitExceeded"))
        assertEquals(DriveErrorKind.QUOTA, classifyDriveError(429, ""))
        assertEquals(DriveErrorKind.NOT_FOUND, classifyDriveError(404, ""))
        assertEquals(DriveErrorKind.OTHER, classifyDriveError(500, ""))
    }

    @Test fun pageKeepsNavigationTokenEvenWithNoFiles() {
        val page = parseDrivePage(JSONObject("""{"files": [], "nextPageToken": "next"}"""))
        assertTrue(page.files.isEmpty())
        assertEquals("next", page.nextPageToken)
    }

    @Test fun fileMapperPreservesMetadataAndAllowsMissingOptionalFields() {
        val file = parseDriveFile(JSONObject("""{"id":"x","name":"Folder","mimeType":"application/vnd.google-apps.folder","capabilities":{"canAddChildren":true},"isAppAuthorized":true}"""))
        assertEquals("x", file.id)
        assertTrue(file.isFolder)
        assertTrue(file.isAppAuthorized)
        assertTrue(file.canAddChildren)
        assertEquals(null, file.size)
        assertEquals(null, file.webViewLink)
    }

    @Test fun nonblankSearchIsGlobalAndBlankSearchStaysInFolder() {
        assertEquals("trashed = false and name contains 'folder\\'s'", driveQuery("parent", "folder's"))
        assertEquals("'parent' in parents and trashed = false", driveQuery("parent", ""))
    }

    @Test fun resumableRangesHandleUnknownSizeAndEmptyContent() {
        assertEquals("bytes 0-4/*", uploadContentRange(0, 5, null, false))
        assertEquals("bytes 5-8/9", uploadContentRange(5, 4, null, true))
        assertEquals(null, uploadContentRange(0, 0, 0, true))
        assertEquals(5L, acknowledgedOffset("bytes=0-4"))
        assertEquals(null, acknowledgedOffset(null))
    }

    @Test fun cachePruningReservesTheMaximumSingleFileAllowance() {
        val mib = 1024L * 1024
        assertEquals(8 * mib, cacheBytesNeededToPrune(40 * mib, 32 * mib, 64 * mib))
        assertEquals(0L, cacheBytesNeededToPrune(16 * mib, 32 * mib, 64 * mib))
    }

    @Test fun selectedMaterialPolicyRejectsDatabasesBackupsAndNonSafUris() {
        listOf("archive.db", "archive.sqlite", "archive.sqlite3", "Riko Hub Backup.json").forEach { name ->
            try {
                validateSelectedMaterial("content", name, if (name.endsWith(".json")) "application/json" else "application/octet-stream")
                throw AssertionError("Expected $name to be rejected")
            } catch (error: DriveException) {
                assertEquals(DriveErrorKind.PERMISSION, error.kind)
            }
        }
        try {
            validateSelectedMaterial("file", "notes.pdf", "application/pdf")
            throw AssertionError("Expected non-SAF URI to be rejected")
        } catch (error: DriveException) {
            assertEquals(DriveErrorKind.PERMISSION, error.kind)
        }
    }

    @Test fun selectedMaterialPolicyAllowsOrdinaryDocumentsAndMedia() {
        listOf(
            "report.pdf" to "application/pdf",
            "proposal.docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "data.csv" to "text/csv",
            "photo.jpg" to "image/jpeg",
            "paper.epub" to "application/epub+zip",
        ).forEach { (name, mime) -> validateSelectedMaterial("content", name, mime) }
    }
}
