package id.riko.hub.ui.timeline

import android.graphics.Bitmap
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import id.riko.hub.R
import id.riko.hub.domain.logic.buildTimeline
import id.riko.hub.domain.logic.timelineWeek
import id.riko.hub.domain.model.*
import id.riko.hub.ui.theme.RikoHubTheme
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

class TimelineLayoutTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun normal_dense_large_text_and_empty_layouts_remain_readable() {
        val zone = ZoneId.of("Asia/Makassar")
        val today = LocalDate.of(2026, 10, 7)
        val now = today.atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        fun state(count: Int): TimelineUiState {
            val events = (1..count).map { index -> Event(id = index.toLong(),
                title = if (index == 1) "Diskusi kelompok untuk persiapan presentasi" else "Rencana kampus $index",
                startAt = now + index * 3_600_000L, endAt = now + index * 3_600_000L + 1_800_000,
                kind = EventKind.MEETING, category = Category.ACADEMIC,
                location = if (index == 1) "Perpustakaan kampus" else null) }
            return TimelineUiState(weekStart = timelineWeek(today), today = today, now = now, zone = zone,
                days = buildTimeline(emptyList(), events, timelineWeek(today), zone), loading = false)
        }
        val content = mutableStateOf(state(3))
        val scale = mutableFloatStateOf(1f)
        val dark = mutableStateOf(true)
        rule.runOnUiThread { rule.activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, scale.floatValue)) {
                RikoHubTheme(dark.value) { Surface(Modifier.fillMaxSize()) {
                    TimelineContent(content.value, {}, {}, {}, {}, {}, {}, {})
                } }
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("day-$today").assertIsDisplayed()
        inspect("normal-dark")
        rule.runOnIdle { content.value = state(7); dark.value = false; scale.floatValue = 2f }
        rule.onNodeWithTag("timeline-add").assertIsDisplayed().assertHasClickAction()
        val last = content.value.days.flatMap { it.stops }.last().key
        rule.onNodeWithTag("timeline-list").performScrollToNode(hasTestTag("timeline-stop-$last"))
        rule.onNodeWithTag("timeline-stop-$last").assertIsDisplayed()
        inspect("dense-large-light")
        rule.runOnIdle { content.value = state(0) }
        rule.onNodeWithText(rule.activity.getString(R.string.timeline_empty)).assertExists()
        rule.onNodeWithTag("timeline-add").assertIsDisplayed()
        inspect("empty-large-light")
    }

    private fun inspect(name: String) {
        rule.waitForIdle()
        rule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult), useUnmergedTree = true)
            .fetchSemanticsNodes().forEach { node ->
                val layouts = mutableListOf<TextLayoutResult>()
                node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
                layouts.forEach { text ->
                    // Layout sizes round to integer pixels; allow the same 1px tolerance as Tasks.
                    assertFalse("Text overflow: ${text.layoutInput.text}", text.multiParagraph.didExceedMaxLines ||
                        (0 until text.lineCount).any { text.getLineRight(it) > text.size.width + 1f ||
                            text.getLineBottom(it) > text.size.height + 1f })
                }
            }
        // Explicit opt-in only; runner pulls and removes these private cache files after review.
        if (InstrumentationRegistry.getArguments().getString("timelineScreenshots") == "true") {
            val directory = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
                "timeline-review").apply { mkdirs() }
            File(directory, "$name.png").outputStream().use {
                rule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }
}
