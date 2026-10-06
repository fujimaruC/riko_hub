package id.riko.hub.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import id.riko.hub.R
import id.riko.hub.navigation.Destination
import id.riko.hub.ui.compass.SpatialNavLayer
import id.riko.hub.ui.components.FoundationNote
import id.riko.hub.ui.theme.*

@Composable
fun HomeScreen(onNavigate: (Destination) -> Unit) {
    SpatialNavLayer(Destination.Home, onNavigate, Modifier.fillMaxSize().testTag("screen-Home"), horizontalOnly = true) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(Modifier.padding(Spacing.space5), verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.labelLarge)
                Text(stringResource(R.string.greeting), style = MaterialTheme.typography.displayLarge, modifier = Modifier.semantics { heading() })
                Text(stringResource(R.string.home_question), style = MaterialTheme.typography.bodyLarge)
                TextButton(onClick = { onNavigate(Destination.Settings) }) {
                    Text(stringResource(R.string.settings), color = MaterialTheme.colorScheme.onSurface)
                }
            }
            HomeHints(onNavigate, top = true)
            // The field owns vertical drags; the surrounding page can still scroll at large font scales.
            SpatialNavLayer(Destination.Home, onNavigate, Modifier.fillMaxWidth().testTag("home-field")) {
                Surface(
                    modifier = Modifier.padding(horizontal = Spacing.space5).fillMaxWidth(),
                    shape = RikoShapes.asymA, color = LocalRikoColors.current.personal.container,
                ) {
                    Column(Modifier.padding(Spacing.space6).heightIn(min = Spacing.field), verticalArrangement = Arrangement.spacedBy(Spacing.space4)) {
                        Text(stringResource(R.string.home_space), style = MaterialTheme.typography.displayMedium)
                        Text(stringResource(R.string.home_placeholder), style = MaterialTheme.typography.bodyLarge)
                        Text(stringResource(R.string.swipe_hint), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            HomeHints(onNavigate, top = false)
            Column(Modifier.padding(Spacing.space5), verticalArrangement = Arrangement.spacedBy(Spacing.space4)) {
                DomainLabels()
                FoundationNote()
            }
        }
    }
}

@Composable
private fun HomeHints(onNavigate: (Destination) -> Unit, top: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        val targets = if (top) listOf(Destination.Timeline) else listOf(Destination.Sync, Destination.Tasks, Destination.Vault)
        targets.forEach { target ->
            TextButton(onClick = { onNavigate(target) }) {
                Text(stringResource(when (target) {
                    Destination.Timeline -> R.string.hint_timeline
                    Destination.Sync -> R.string.hint_sync
                    Destination.Tasks -> R.string.hint_tasks
                    else -> R.string.hint_vault
                }), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DomainLabels() {
    val colors = LocalRikoColors.current
    val domains = listOf(R.string.academic to colors.academic, R.string.organization to colors.organization,
        R.string.personal to colors.personal, R.string.projects to colors.projects)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
        domains.chunked(2).forEachIndexed { index, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                row.forEachIndexed { column, (label, accent) ->
                    Surface(Modifier.weight(1f), color = accent.container,
                        shape = if (index == column) RikoShapes.pill else RikoShapes.asymB) {
                        Text(stringResource(label), Modifier.padding(Spacing.space3), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
// TODO(v0.1): Add real Home content, urgency and bubbles in Phase 4; no fake tasks here.
