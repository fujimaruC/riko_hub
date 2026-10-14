package id.riko.hub.ui.capture

import org.junit.Assert.*
import org.junit.Test

class CaptureDraftTest {
    @Test fun short_text_is_a_title_and_multiline_text_keeps_notes() {
        val short = captureTaskDraft("  Baca bab 7  ")!!
        assertEquals("Baca bab 7", short.title)
        assertNull(short.notes)
        val multiline = captureTaskDraft("Baca referensi\nhttps://example.org\nSimpan kutipan")!!
        assertEquals("Baca referensi", multiline.title)
        assertEquals("https://example.org\nSimpan kutipan", multiline.notes)
    }

    @Test fun long_first_line_is_preserved_as_notes_without_truncation() {
        val body = "x".repeat(121)
        val task = captureTaskDraft(body)!!
        assertEquals("", task.title)
        assertEquals(body, task.notes)
        assertNotNull(captureTaskDraft("judul\n" + "x".repeat(2000)))
        assertNull(captureTaskDraft("judul\n" + "x".repeat(2001)))
        assertNull(captureTaskDraft("x".repeat(2001)))
    }

    @Test fun blank_capture_opens_an_empty_editor() {
        assertEquals("", captureTaskDraft(" \n ")!!.title)
        assertNull(captureTaskDraft("")!!.notes)
    }
}
