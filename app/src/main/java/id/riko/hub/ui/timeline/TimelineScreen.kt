package id.riko.hub.ui.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import id.riko.hub.R
import id.riko.hub.navigation.Destination
import id.riko.hub.ui.components.*
import id.riko.hub.ui.theme.*

@Composable
fun TimelineScreen(onNavigate: (Destination) -> Unit) {
    ScreenFrame(Destination.Timeline, onNavigate) {
        Text(stringResource(R.string.timeline_intro), style = MaterialTheme.typography.bodyLarge)
        val ink = LocalRikoColors.current.organization.color
        Canvas(Modifier.fillMaxWidth().height(Spacing.illustration)) {
            val path = Path().apply {
                moveTo(size.width * 0.15f, 0f)
                cubicTo(size.width * 0.9f, size.height * 0.25f, size.width * 0.1f, size.height * 0.7f, size.width * 0.8f, size.height)
            }
            drawPath(path, ink, style = Stroke(Spacing.pathStroke.toPx()))
        }
        Surface(shape = RikoShapes.wide, color = MaterialTheme.colorScheme.surfaceVariant) {
            Column(Modifier.padding(Spacing.space5), verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                Text(stringResource(R.string.timeline_empty), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.timeline_placeholder))
            }
        }
        FoundationNote()
    }
}
// TODO(v0.1): Add real events, week navigation and merged task deadlines in Phase 5.
