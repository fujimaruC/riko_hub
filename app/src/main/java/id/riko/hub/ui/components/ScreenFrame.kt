package id.riko.hub.ui.components

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
import id.riko.hub.ui.theme.Spacing

@Composable
fun ScreenFrame(
    destination: Destination, onNavigate: (Destination) -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().testTag("screen-${destination.name}")) {
        ReturnHomeHandle(destination, onNavigate)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.space5),
            verticalArrangement = Arrangement.spacedBy(Spacing.space6),
        ) {
            Text(destinationLabel(destination), style = MaterialTheme.typography.displayMedium, modifier = Modifier.semantics { heading() })
            content()
            Spacer(Modifier.height(Spacing.space6))
        }
    }
}

@Composable
fun FoundationNote() {
    Text(stringResource(R.string.foundation_note), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun ReturnHomeHandle(destination: Destination, onNavigate: (Destination) -> Unit) {
    // Return swipes are confined to this handle; scrolling content never navigates.
    SpatialNavLayer(destination, onNavigate, Modifier.fillMaxWidth().testTag("return-handle")) {
        TextButton(onClick = { onNavigate(Destination.Home) }, modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.returnHandle)) {
            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                Text(stringResource(R.string.back_home), color = MaterialTheme.colorScheme.onSurface)
                val hint = when (destination) {
                    Destination.Tasks -> R.string.return_down
                    Destination.Timeline -> R.string.return_up
                    Destination.Vault -> R.string.return_right
                    Destination.Sync -> R.string.return_left
                    else -> null
                }
                hint?.let {
                    Text(stringResource(it), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
