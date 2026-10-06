package id.riko.hub.ui.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import id.riko.hub.R
import id.riko.hub.navigation.Destination
import id.riko.hub.ui.components.*
import id.riko.hub.ui.theme.*

@Composable
fun TasksScreen(onNavigate: (Destination) -> Unit) {
    ScreenFrame(Destination.Tasks, onNavigate) {
        Text(stringResource(R.string.tasks_intro), style = MaterialTheme.typography.bodyLarge)
        listOf(R.string.today, R.string.tomorrow, R.string.later).forEachIndexed { index, label ->
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                Text(stringResource(label), style = MaterialTheme.typography.titleSmall)
                if (index == 0) Surface(shape = RikoShapes.pill, color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(stringResource(R.string.tasks_placeholder), Modifier.fillMaxWidth().padding(Spacing.space4))
                } else HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
        }
        FoundationNote()
    }
}
// TODO(v0.1): Add task CRUD, category filters and time buckets in Phase 3.
