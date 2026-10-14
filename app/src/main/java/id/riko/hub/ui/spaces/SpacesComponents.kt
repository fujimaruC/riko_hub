package id.riko.hub.ui.spaces

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import id.riko.hub.R
import id.riko.hub.ui.theme.RikoTypography
import id.riko.hub.ui.theme.Spacing
import id.riko.hub.ui.theme.SpacesTokens

@Composable
fun SpacesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = SpacesTokens.canvas,
            surface = SpacesTokens.surface,
            surfaceVariant = SpacesTokens.row,
            onBackground = SpacesTokens.primary,
            onSurface = SpacesTokens.primary,
            onSurfaceVariant = SpacesTokens.secondary,
            inverseSurface = SpacesTokens.row,
            inverseOnSurface = SpacesTokens.primary,
            inversePrimary = SpacesTokens.tangerine,
            secondary = SpacesTokens.jade,
            onSecondary = SpacesTokens.canvas,
            secondaryContainer = SpacesTokens.row,
            onSecondaryContainer = SpacesTokens.primary,
            primary = SpacesTokens.tangerine,
            onPrimary = SpacesTokens.canvas,
            primaryContainer = SpacesTokens.tangerineDim,
            onPrimaryContainer = SpacesTokens.primary,
            tertiary = SpacesTokens.cream,
            onTertiary = SpacesTokens.canvas,
            tertiaryContainer = SpacesTokens.row,
            onTertiaryContainer = SpacesTokens.primary,
            surfaceDim = SpacesTokens.canvas,
            surfaceBright = SpacesTokens.pressed,
            surfaceContainerLowest = SpacesTokens.canvas,
            surfaceContainerLow = SpacesTokens.surface,
            surfaceContainer = SpacesTokens.row,
            surfaceContainerHigh = SpacesTokens.pressed,
            surfaceContainerHighest = SpacesTokens.edge,
            outline = SpacesTokens.edge,
            outlineVariant = SpacesTokens.pressed,
            error = Color(0xFFFF9F91),
            onError = SpacesTokens.canvas,
            errorContainer = Color(0xFF512020),
            onErrorContainer = SpacesTokens.primary,
        ),
        typography = RikoTypography,
        content = content,
    )
}

@Composable
fun SpacesPageHeader(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SpacesIconButton(R.drawable.ic_chevron_left, stringResource(R.string.spaces_back), onBack)
            Spacer(Modifier.width(Spacing.space3))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.displayMedium, color = SpacesTokens.primary,
                    modifier = Modifier.semantics { heading() })
                subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = SpacesTokens.secondary) }
            }
            trailing?.invoke(this)
        }
    }
}

@Composable
fun SpacesSectionHeader(
    title: String,
    icon: Int,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stacked = maxWidth < 360.dp || LocalDensity.current.fontScale >= 1.3f
        if (stacked) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.space1)) {
                Row(Modifier.fillMaxWidth().heightIn(min = Spacing.touch), verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Icon(
                        painter = androidx.compose.ui.res.painterResource(icon), contentDescription = null,
                        tint = SpacesTokens.tangerine, modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(Spacing.space2))
                    Text(title, Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.titleSmall,
                        color = SpacesTokens.primary)
                }
                if (actionLabel != null && onAction != null) {
                    TextButton(onClick = onAction, modifier = Modifier.align(Alignment.End).heightIn(min = Spacing.touch)) {
                        Text(actionLabel, color = SpacesTokens.tangerine)
                    }
                }
            }
        } else {
            Row(Modifier.fillMaxWidth().heightIn(min = Spacing.touch), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Icon(
                    painter = androidx.compose.ui.res.painterResource(icon), contentDescription = null,
                    tint = SpacesTokens.tangerine, modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(Spacing.space2))
                Text(title, Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.titleSmall,
                    color = SpacesTokens.primary)
                if (actionLabel != null && onAction != null) TextButton(onClick = onAction) {
                    Text(actionLabel, color = SpacesTokens.tangerine)
                }
            }
        }
    }
}

@Composable
fun SpacesIconButton(
    icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.sizeIn(minWidth = Spacing.touch, minHeight = Spacing.touch)) {
        Icon(androidx.compose.ui.res.painterResource(icon), contentDescription)
    }
}

@Composable
fun SpacesBadge(text: String, tint: Color = SpacesTokens.tangerine) {
    Text(text, Modifier.clip(CircleShape).background(tint.copy(alpha = .13f))
        .border(1.dp, tint.copy(alpha = .35f), CircleShape).padding(horizontal = 12.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelMedium, color = tint)
}
