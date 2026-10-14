package id.riko.hub.navigation

enum class Destination {
    Home, Tasks, Timeline, Capture, Vault, Sync, Settings, Courses,
    Spaces, AcademicsSpace, ProjectsSpace, PersonalSpace, DevicesSpace,
}

val Destination.isSpaces: Boolean
    get() = this == Destination.Spaces || this == Destination.AcademicsSpace ||
        this == Destination.ProjectsSpace || this == Destination.PersonalSpace ||
        this == Destination.DevicesSpace
