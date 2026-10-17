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
import id.riko.hub.ui.theme.Spacing

@Composable
fun ScreenFrame(destination: Destination, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().testTag("screen-${destination.name}")) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.space5),
            verticalArrangement = Arrangement.spacedBy(Spacing.space6),
        ) {
            content()
            Spacer(Modifier.height(Spacing.space6))
        }
    }
}

@Composable
fun FoundationNote() {
    Text(stringResource(R.string.foundation_note), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
