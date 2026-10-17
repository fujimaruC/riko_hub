package id.riko.hub.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import id.riko.hub.R
import id.riko.hub.navigation.Destination

@Composable
fun destinationLabel(destination: Destination): String = stringResource(when (destination) {
    Destination.Home -> R.string.home
    Destination.Tasks -> R.string.tasks
    Destination.Timeline -> R.string.timeline
    Destination.Capture -> R.string.capture
    Destination.Vault -> R.string.vault
    Destination.Sync -> R.string.sync
    Destination.Settings -> R.string.settings
    Destination.Courses -> R.string.courses
    Destination.Spaces -> R.string.spaces_title
    Destination.AcademicsSpace -> R.string.spaces_academics
    Destination.ProjectsSpace -> R.string.spaces_projects
    Destination.PersonalSpace -> R.string.spaces_personal
    Destination.DevicesSpace -> R.string.spaces_devices
})
