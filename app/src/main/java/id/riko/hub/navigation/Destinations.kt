package id.riko.hub.navigation

enum class Destination { Home, Tasks, Timeline, Vault, Sync, Settings }

val compassDestinations = listOf(
    Destination.Timeline, Destination.Sync, Destination.Home, Destination.Vault, Destination.Tasks,
)
