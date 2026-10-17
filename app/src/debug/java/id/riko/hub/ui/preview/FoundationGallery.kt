package id.riko.hub.ui.preview

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import id.riko.hub.R
import id.riko.hub.navigation.Destination
import id.riko.hub.ui.navigation.RikoDock
import id.riko.hub.ui.home.HomePreview
import id.riko.hub.ui.theme.*

@Preview(name = "Home / light", widthDp = 393, heightDp = 852)
@Composable
private fun HomeLightPreview() { RikoHubTheme(false) { Surface { HomePreview() } } }

@Preview(name = "Home / dark", widthDp = 393, heightDp = 852, uiMode = 0x20)
@Composable
private fun HomeDarkPreview() { RikoHubTheme(true) { Surface { HomePreview() } } }

@Preview(name = "Components / light", widthDp = 393)
@Preview(name = "Components / dark", widthDp = 393, uiMode = 0x20)
@Preview(name = "Components / large type", widthDp = 360, fontScale = 2f)
@Composable
private fun ComponentGallery() {
    RikoHubTheme(androidx.compose.foundation.isSystemInDarkTheme()) {
        Surface {
            Column(Modifier.padding(Spacing.space5), verticalArrangement = Arrangement.spacedBy(Spacing.space4)) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displayMedium)
                listOf(RikoShapes.asymA, RikoShapes.wide, RikoShapes.pill).forEach { shape ->
                    Surface(shape = shape, color = MaterialTheme.colorScheme.primaryContainer) {
                        Text(stringResource(R.string.academic), Modifier.padding(Spacing.space4))
                    }
                }
                RikoDock(Destination.Home, reducedMotion = false, onNavigate = {})
            }
        }
    }
}

@Preview(name = "Tasks / light", widthDp = 393, heightDp = 852)
@Preview(name = "Tasks / dark", widthDp = 393, heightDp = 852, uiMode = 0x20)
@Composable
private fun TasksPreview() {
    RikoHubTheme(androidx.compose.foundation.isSystemInDarkTheme()) { Surface { id.riko.hub.ui.tasks.TasksScreen(onNavigate = {}) } }
}
