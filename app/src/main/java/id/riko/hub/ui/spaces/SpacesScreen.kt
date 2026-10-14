package id.riko.hub.ui.spaces

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import id.riko.hub.R
import id.riko.hub.navigation.Destination
import id.riko.hub.ui.theme.Spacing
import id.riko.hub.ui.theme.SpacesTokens

@Composable
fun SpacesScreen(onNavigate: (Destination) -> Unit) {
    SpacesTheme {
        val twoColumns = LocalConfiguration.current.screenWidthDp >= 600 && LocalConfiguration.current.fontScale < 1.3f
        val portalAtlas = ImageBitmap.imageResource(R.drawable.illustration_space_portals)
        Column(
            Modifier.fillMaxSize().testTag("screen-Spaces").background(SpacesTokens.canvas).verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.space5, vertical = Spacing.space4),
            verticalArrangement = Arrangement.spacedBy(Spacing.space5),
        ) {
            SpacesPageHeader(
                title = stringResource(R.string.spaces_title),
                subtitle = stringResource(R.string.spaces_subtitle),
                onBack = { onNavigate(Destination.Home) },
            )
            if (twoColumns) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                        SpacePortal("space-portal-AcademicsSpace", R.string.spaces_academics, R.string.spaces_academics_hint, R.string.spaces_academics_accessibility,
                            SpacesTokens.ultramarine, PortalArt.Orbit, portalAtlas, true, Modifier.weight(1f)) { onNavigate(Destination.AcademicsSpace) }
                        SpacePortal("space-portal-ProjectsSpace", R.string.spaces_projects, R.string.spaces_projects_hint, R.string.spaces_projects_accessibility,
                            SpacesTokens.amber, PortalArt.Bench, portalAtlas, true, Modifier.weight(1f)) { onNavigate(Destination.ProjectsSpace) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                        SpacePortal("space-portal-PersonalSpace", R.string.spaces_personal, R.string.spaces_personal_hint, R.string.spaces_personal_accessibility,
                            SpacesTokens.magenta, PortalArt.Moon, portalAtlas, true, Modifier.weight(1f)) { onNavigate(Destination.PersonalSpace) }
                        SpacePortal("space-portal-DevicesSpace", R.string.spaces_devices, R.string.spaces_devices_hint, R.string.spaces_devices_accessibility,
                            SpacesTokens.ultramarine, PortalArt.Bridge, portalAtlas, true, Modifier.weight(1f)) { onNavigate(Destination.DevicesSpace) }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
                    SpacePortal("space-portal-AcademicsSpace", R.string.spaces_academics, R.string.spaces_academics_hint, R.string.spaces_academics_accessibility,
                        SpacesTokens.ultramarine, PortalArt.Orbit, portalAtlas) { onNavigate(Destination.AcademicsSpace) }
                    SpacePortal("space-portal-ProjectsSpace", R.string.spaces_projects, R.string.spaces_projects_hint, R.string.spaces_projects_accessibility,
                        SpacesTokens.amber, PortalArt.Bench, portalAtlas) { onNavigate(Destination.ProjectsSpace) }
                    SpacePortal("space-portal-PersonalSpace", R.string.spaces_personal, R.string.spaces_personal_hint, R.string.spaces_personal_accessibility,
                        SpacesTokens.magenta, PortalArt.Moon, portalAtlas) { onNavigate(Destination.PersonalSpace) }
                    SpacePortal("space-portal-DevicesSpace", R.string.spaces_devices, R.string.spaces_devices_hint, R.string.spaces_devices_accessibility,
                        SpacesTokens.ultramarine, PortalArt.Bridge, portalAtlas) { onNavigate(Destination.DevicesSpace) }
                }
            }
        }
    }
}

private enum class PortalArt { Orbit, Bench, Moon, Bridge }

@Composable
private fun SpacePortal(tag: String, title: Int, hint: Int, accessibility: Int, accent: androidx.compose.ui.graphics.Color,
                        art: PortalArt, atlas: ImageBitmap, tile: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val spokenLabel = stringResource(accessibility)
    Box(
        modifier.fillMaxWidth().testTag(tag).heightIn(min = if (tile) 190.dp else 124.dp).clip(RoundedCornerShape(24.dp))
            .background(accent.copy(alpha = .08f)).border(1.dp, accent.copy(alpha = .42f), RoundedCornerShape(24.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(Spacing.space4).semantics { contentDescription = spokenLabel },
    ) {
        if (tile) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                PortalIllustration(art, atlas, Modifier.fillMaxWidth().height(92.dp))
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium, color = SpacesTokens.primary)
                Text(stringResource(hint), style = MaterialTheme.typography.bodyMedium, color = SpacesTokens.secondary)
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.space1)) {
                    Text(stringResource(title), style = MaterialTheme.typography.titleMedium, color = SpacesTokens.primary)
                    Text(stringResource(hint), style = MaterialTheme.typography.bodyMedium, color = SpacesTokens.secondary)
                }
                Spacer(Modifier.width(Spacing.space3))
                PortalIllustration(art, atlas, Modifier.size(width = 148.dp, height = 92.dp))
            }
        }
        Icon(painter = androidx.compose.ui.res.painterResource(R.drawable.ic_chevron_right), contentDescription = null,
            tint = accent, modifier = Modifier.align(Alignment.TopEnd).size(Spacing.space4))
    }
}

@Composable
private fun PortalIllustration(art: PortalArt, atlas: ImageBitmap, modifier: Modifier) {
    Canvas(modifier) {
        val quadrantWidth = atlas.width / 2
        val quadrantHeight = atlas.height / 2
        val (column, row) = when (art) {
            PortalArt.Orbit -> 0 to 0
            PortalArt.Bench -> 1 to 0
            PortalArt.Moon -> 0 to 1
            PortalArt.Bridge -> 1 to 1
        }
        val sourceAspect = quadrantWidth.toFloat() / quadrantHeight
        val targetAspect = size.width / size.height
        val cropWidth = if (sourceAspect > targetAspect) (quadrantHeight * targetAspect).toInt() else quadrantWidth
        val cropHeight = if (sourceAspect > targetAspect) quadrantHeight else (quadrantWidth / targetAspect).toInt()
        drawImage(
            image = atlas,
            srcOffset = IntOffset(column * quadrantWidth + (quadrantWidth - cropWidth) / 2,
                row * quadrantHeight + (quadrantHeight - cropHeight) / 2),
            srcSize = IntSize(cropWidth, cropHeight),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
            filterQuality = FilterQuality.Medium,
        )
    }
}
