package id.riko.hub.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.riko.hub.R
import id.riko.hub.navigation.Destination
import id.riko.hub.navigation.isSpaces
import id.riko.hub.ui.theme.ChefCanvas
import id.riko.hub.ui.theme.ChefGround
import id.riko.hub.ui.theme.ChefLine
import id.riko.hub.ui.theme.ChefMuted
import id.riko.hub.ui.theme.ChefSage

private val destinations = listOf(
    Destination.Home, Destination.Tasks, Destination.Timeline, Destination.ProjectsSpace, Destination.Spaces,
)
private val icons = mapOf(
    Destination.Home to R.drawable.ic_dock_today,
    Destination.Tasks to R.drawable.ic_dock_tasks,
    Destination.Timeline to R.drawable.ic_dock_plan,
    Destination.ProjectsSpace to R.drawable.ic_dock_projects,
    Destination.Spaces to R.drawable.ic_dock_spaces,
)

@Composable
fun RikoDock(
    current: Destination,
    reducedMotion: Boolean,
    onNavigate: (Destination) -> Unit,
) {
    val tab = when {
        current == Destination.ProjectsSpace -> Destination.ProjectsSpace
        current.isSpaces || current == Destination.Courses || current == Destination.Sync || current == Destination.Vault -> Destination.Spaces
        current == Destination.Tasks -> Destination.Tasks
        current == Destination.Timeline -> Destination.Timeline
        else -> Destination.Home
    }
    val label = { destination: Destination ->
        when (destination) {
            Destination.Home -> "Hari ini"
            Destination.Tasks -> "Tugas"
            Destination.Timeline -> "Jadwal"
            Destination.ProjectsSpace -> "Proyek"
            Destination.Spaces -> "Ruangku"
            else -> destination.name
        }
    }
    Surface(
        color = ChefGround,
        border = BorderStroke(1.dp, ChefLine),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .testTag("dock")
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            destinations.forEach { destination ->
                val selected = destination == tab
                val name = label(destination)
                val itemBg = if (selected) ChefSage else Color.Transparent
                val contentColor = if (selected) ChefGround else ChefMuted
                Column(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .selectable(
                            selected = selected,
                            role = Role.Tab,
                            onClick = { onNavigate(destination) },
                        )
                        .semantics { contentDescription = name }
                        .testTag("dock-${destination.name}")
                        .background(itemBg, RoundedCornerShape(16.dp))
                        .padding(horizontal = 2.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        painter = painterResource(icons[destination] ?: R.drawable.ic_dock_today),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = contentColor,
                    )
                    Text(
                        text = name,
                        color = contentColor,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
