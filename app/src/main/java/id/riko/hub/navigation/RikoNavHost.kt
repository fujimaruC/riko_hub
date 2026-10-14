package id.riko.hub.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import id.riko.hub.R
import id.riko.hub.RikoHubApp
import id.riko.hub.domain.model.AppSettings
import id.riko.hub.ui.capture.CaptureScreen
import id.riko.hub.ui.home.HomeScreen
import id.riko.hub.ui.navigation.RenaissanceDock
import id.riko.hub.ui.courses.CoursesScreen
import id.riko.hub.ui.settings.SettingsScreen
import id.riko.hub.ui.spaces.AcademicsSpaceScreen
import id.riko.hub.ui.spaces.DevicesSpaceScreen
import id.riko.hub.ui.spaces.ProjectsSpaceScreen
import id.riko.hub.ui.spaces.SpacesScreen
import id.riko.hub.ui.spaces.personal.PersonalScreen
import id.riko.hub.ui.sync.SyncScreen
import id.riko.hub.ui.tasks.TasksScreen
import id.riko.hub.ui.timeline.TimelineScreen
import id.riko.hub.ui.vault.VaultScreen
import id.riko.hub.ui.vault.VaultViewModel
import id.riko.hub.ui.theme.*
import kotlinx.coroutines.launch

data class SettingsWriteFailure(val id: Int, val retry: () -> Unit)

@Composable
fun RikoNavHost(
    settings: AppSettings,
    onTheme: (ThemeMode) -> Unit,
    onDisplayName: (String) -> Unit = {},
    settingsWriteFailure: SettingsWriteFailure? = null,
    onSettingsFailureDismissed: (Int) -> Unit = {},
    openTaskId: Long? = null,
    onTaskOpenConsumed: () -> Unit = {},
    backupContent: @Composable () -> Unit = {},
    onSpacesVisibilityChanged: (Boolean) -> Unit = {},
) {
    val controller = rememberNavController()
    val app = LocalContext.current.applicationContext as RikoHubApp
    val sharedVaultModel: VaultViewModel = viewModel(key = "navigation-vault", factory = viewModelFactory {
        initializer { VaultViewModel(app.container.materials, app.container.driveAuthorization) }
    })
    val sharedVaultState by sharedVaultModel.state.collectAsStateWithLifecycle()
    val entry by controller.currentBackStackEntryAsState()
    val current = destination(entry)
    SideEffect { onSpacesVisibilityChanged(current.isSpaces) }
    val materialBackground = MaterialTheme.colorScheme.background
    val homeBackground = remember(materialBackground) { mutableStateOf(materialBackground) }
    val onHomeBackgroundChanged: (Color) -> Unit = remember(homeBackground) { { homeBackground.value = it } }
    val reduced = rememberReducedMotion()
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val comingSoon = stringResource(R.string.coming_soon)
    val settingsFailureMessage = stringResource(R.string.settings_save_failed)
    val retryLabel = stringResource(R.string.retry)
    val navigate: (Destination) -> Unit = { target ->
        if (target != current) {
            when {
                target == Destination.Home -> controller.popBackStack(Destination.Home.name, false)
                target == Destination.Spaces -> if (current.isSpaces) {
                    controller.popBackStack(Destination.Spaces.name, false)
                } else controller.navigate(target.name) {
                    launchSingleTop = true
                    popUpTo(Destination.Home.name) { saveState = true }
                }
                target.isSpaces && target != Destination.Spaces -> {
                    if (!current.isSpaces) controller.navigate(Destination.Spaces.name) {
                        launchSingleTop = true
                        popUpTo(Destination.Home.name) { saveState = true }
                    }
                    controller.navigate(target.name) {
                        launchSingleTop = true
                    }
                }
                target in setOf(Destination.Timeline, Destination.Capture, Destination.Vault) ->
                    controller.navigate(target.name) {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(Destination.Home.name) { saveState = true }
                    }
                else -> controller.navigate(target.name) { launchSingleTop = true }
            }
        }
    }
    BackHandler(current != Destination.Home) {
        if (!controller.popBackStack()) navigate(Destination.Home)
    }
    LaunchedEffect(openTaskId) { if (openTaskId != null) navigate(Destination.Tasks) }
    LaunchedEffect(settingsWriteFailure?.id) {
        val failure = settingsWriteFailure ?: return@LaunchedEffect
        when (snackbar.showSnackbar(settingsFailureMessage, retryLabel, withDismissAction = true,
            duration = SnackbarDuration.Indefinite)) {
            SnackbarResult.ActionPerformed -> failure.retry()
            SnackbarResult.Dismissed -> onSettingsFailureDismissed(failure.id)
        }
    }
      Scaffold(
          containerColor = when {
              current.isSpaces -> SpacesTokens.canvas
              current == Destination.Home -> homeBackground.value
              else -> materialBackground
          },
          contentColor = if (current.isSpaces) SpacesTokens.primary else MaterialTheme.colorScheme.onBackground,
          snackbarHost = { SnackbarHost(snackbar) },
          bottomBar = {
            if (!imeVisible) {
                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = Spacing.screenGutter, vertical = Spacing.space2)) {
                    RenaissanceDock(current, reduced, navigate)
                }
            }
          },
      ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            NavHost(
                navController = controller,
                startDestination = Destination.Home.name,
                modifier = Modifier.widthIn(max = Spacing.maxContentWidth).fillMaxSize(),
                enterTransition = {
                    fadeIn(tween(if (reduced) Motion.fast else Motion.medium))
                },
                exitTransition = {
                    fadeOut(tween(if (reduced) Motion.fast else Motion.medium))
                },
            ) {
                composable(Destination.Home.name) {
                    HomeScreen(hintsShownCount = settings.hintsShownCount,
                        onBackgroundColorChanged = onHomeBackgroundChanged, onNavigate = navigate)
                }
                composable(Destination.Tasks.name) { TasksScreen(navigate, openTaskId, onTaskOpenConsumed) }
                composable(Destination.Capture.name) { CaptureScreen(navigate) }
                composable(Destination.Courses.name) { CoursesScreen(navigate) }
                composable(Destination.Spaces.name) { SpacesScreen(navigate) }
                composable(Destination.AcademicsSpace.name) {
                    AcademicsSpaceScreen(
                        onNavigate = navigate,
                        vaultState = sharedVaultState,
                        onOpenMaterial = { file ->
                            navigate(Destination.Vault)
                            sharedVaultModel.open(file)
                        },
                    )
                }
                composable(Destination.ProjectsSpace.name) { ProjectsSpaceScreen(navigate) }
                composable(Destination.PersonalSpace.name) { PersonalScreen(navigate) }
                composable(Destination.DevicesSpace.name) { DevicesSpaceScreen(navigate) }
                composable(Destination.Timeline.name) { TimelineScreen() }
                composable(Destination.Vault.name) {
                    VaultScreen(navigate, { scope.launch { snackbar.showSnackbar(comingSoon) } }, sharedVaultModel)
                }
                composable(Destination.Sync.name) { SyncScreen(navigate) }
                composable(Destination.Settings.name) { SettingsScreen(settings, onTheme, onDisplayName, backupContent) }
            }
        }
      }
}

private fun destination(entry: NavBackStackEntry?): Destination =
    Destination.entries.firstOrNull { it.name == entry?.destination?.route } ?: Destination.Home
