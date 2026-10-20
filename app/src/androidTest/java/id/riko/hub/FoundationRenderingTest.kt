package id.riko.hub

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import id.riko.hub.ui.RikoHub
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import java.io.File

class FoundationRenderingTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun both_themes_keep_text_readable_at_all_font_scales() {
        val fontScale = mutableFloatStateOf(1f)
        rule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale.floatValue)) {
                Box(Modifier.fillMaxSize()) { RikoHub() }
            }
        }
        listOf(1f, 1.3f, 2f).forEach { scale ->
            rule.runOnIdle { fontScale.floatValue = scale }
            listOf("Terang", "Gelap").forEach { theme ->
                rule.onNodeWithText("Pengaturan").performScrollTo().performClick()
                rule.onNodeWithText(theme).performClick().assertIsSelected()
                assertNoTextOverflow()
                snapshot("$scale-$theme-Settings")
                rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
                assertNoTextOverflow()
                snapshot("$scale-$theme-Home")
                listOf("Tasks", "Timeline", "ProjectsSpace", "Spaces").forEach { label ->
                    rule.onNodeWithTag("dock-$label").assertHasClickAction()
                    rule.onNodeWithTag("dock-$label").performClick()
                    assertNoTextOverflow()
                    snapshot("$scale-$theme-$label")
                    rule.onNodeWithTag("dock-Home").performClick()
                }
            }
        }
    }

    private fun assertNoTextOverflow() {
        rule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult), useUnmergedTree = true)
            .fetchSemanticsNodes().forEach { node ->
                val layouts = mutableListOf<TextLayoutResult>()
                node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
                layouts.forEach { layout ->
                    // TextLayoutResult rounds its integer size; tolerate only that one-pixel rounding.
                    val clipped = (0 until layout.lineCount).any { line ->
                            layout.getLineLeft(line) < -1f ||
                                layout.getLineRight(line) > layout.size.width + 1f ||
                                layout.getLineBottom(line) > layout.size.height + 1f
                        } || layout.multiParagraph.didExceedMaxLines &&
                        (layout.layoutInput.maxLines != 1 || layout.layoutInput.density.fontScale >= 1.3f ||
                            layout.layoutInput.overflow != TextOverflow.Ellipsis || !layout.isLineEllipsized(layout.lineCount - 1))
                    assertFalse("Text clipped: ${layout.layoutInput.text}; fontScale=" +
                        layout.layoutInput.density.fontScale, clipped)
                }
            }
    }

    private fun snapshot(name: String) {
        val directory = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "phase1")
        directory.mkdirs()
        File(directory, "$name.png").outputStream().use {
            rule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
