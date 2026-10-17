package id.riko.hub.ui.spaces

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.riko.hub.R
import id.riko.hub.ui.theme.ChefGround
import id.riko.hub.ui.theme.ChefInk
import id.riko.hub.ui.theme.ChefLime
import id.riko.hub.ui.theme.ChefLine
import id.riko.hub.ui.theme.ChefMuted
import id.riko.hub.ui.theme.ChefPanel
import id.riko.hub.ui.theme.NotoCondensedBlack
import id.riko.hub.ui.theme.Spacing

@Composable
fun SpacesTheme(content: @Composable () -> Unit) {
    content()
}

@Composable
fun SpacesPageHeader(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(ChefPanel)
                    .border(1.dp, ChefLine, CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) {
                Text("←", color = ChefInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Column {
                subtitle?.let {
                    Text(it.uppercase(), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = ChefMuted)
                }
                Text(
                    title.uppercase(),
                    fontFamily = NotoCondensedBlack,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    color = ChefInk,
                    modifier = Modifier.semantics { heading() }
                )
            }
        }
        trailing?.let { Row { it() } }
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
                    Icon(
                        painter = painterResource(icon), contentDescription = null,
                        tint = ChefMuted, modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(Spacing.space2))
                    Text(title, Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.titleSmall,
                        color = ChefInk, fontWeight = FontWeight.Bold)
                }
                if (actionLabel != null && onAction != null) {
                    TextButton(onClick = onAction, modifier = Modifier.align(Alignment.End).heightIn(min = Spacing.touch)) {
                        Text(actionLabel, color = ChefLime, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Row(Modifier.fillMaxWidth().heightIn(min = Spacing.touch), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(icon), contentDescription = null,
                    tint = ChefMuted, modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(Spacing.space2))
                Text(title, Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.titleSmall,
                    color = ChefInk, fontWeight = FontWeight.Bold)
                if (actionLabel != null && onAction != null) TextButton(onClick = onAction) {
                    Text(actionLabel, color = ChefLime, fontWeight = FontWeight.Bold)
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
        Icon(painterResource(icon), contentDescription, tint = ChefInk)
    }
}

@Composable
fun SpacesBadge(text: String, tint: Color = Color.Unspecified) {
    val badgeTint = tint.takeUnless { it == Color.Unspecified } ?: ChefLime
    Text(text, Modifier.clip(CircleShape).background(badgeTint.copy(alpha = 0.15f))
        .border(1.dp, badgeTint.copy(alpha = 0.35f), CircleShape).padding(horizontal = 12.dp, vertical = 5.dp),
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = badgeTint)
}
