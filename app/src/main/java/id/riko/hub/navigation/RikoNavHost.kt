package id.riko.hub.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.*
import id.riko.hub.R
import id.riko.hub.domain.model.AppSettings
import id.riko.hub.domain.model.NavigationStyle
import id.riko.hub.ui.compass.CompassChip
import id.riko.hub.ui.compass.LocalNavigationStyle
import id.riko.hub.ui.home.HomeScreen
import id.riko.hub.ui.settings.SettingsScreen
import id.riko.hub.ui.sync.SyncScreen
import id.riko.hub.ui.tasks.TasksScreen
import id.riko.hub.ui.timeline.TimelineScreen
import id.riko.hub.ui.vault.VaultScreen
import id.riko.hub.ui.theme.*
import kotlinx.coroutines.launch

data class SettingsWriteFailure(val id: Int, val retry: () -> Unit)

@Composable
fun RikoNavHost(
    settings: AppSettings,
    onTheme: (ThemeMode) -> Unit,
    onDisplayName: (String) -> Unit = {},
    onNavigationStyle: (NavigationStyle) -> Unit = {},
    settingsWriteFailure: SettingsWriteFailure? = null,
    onSettingsFailureDismissed: (Int) -> Unit = {},
    openTaskId: Long? = null,
    onTaskOpenConsumed: () -> Unit = {},
    backupContent: @Composable () -> Unit = {},
) {
    val controller = rememberNavController()
    val entry by controller.currentBackStackEntryAsState()
    val current = destination(entry)
    val materialBackground = MaterialTheme.colorScheme.background
    val homeBackground = remember(materialBackground) { mutableStateOf(materialBackground) }
    val onHomeBackgroundChanged: (Color) -> Unit = remember(homeBackground) { { homeBackground.value = it } }
    val reduced = rememberReducedMotion()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val comingSoon = stringResource(R.string.coming_soon)
    val settingsFailureMessage = stringResource(R.string.settings_save_failed)
    val retryLabel = stringResource(R.string.retry)
    val navigate: (Destination) -> Unit = { target ->
        if (target != current) {
            if (target == Destination.Home) controller.popBackStack(Destination.Home.name, false)
            else controller.navigate(target.name) {
                launchSingleTop = true
                popUpTo(Destination.Home.name)
            }
        }
    }
    BackHandler(current != Destination.Home) { navigate(Destination.Home) }
    LaunchedEffect(openTaskId) { if (openTaskId != null) navigate(Destination.Tasks) }
    LaunchedEffect(settingsWriteFailure?.id) {
        val failure = settingsWriteFailure ?: return@LaunchedEffect
        when (snackbar.showSnackbar(settingsFailureMessage, retryLabel, withDismissAction = true,
            duration = SnackbarDuration.Indefinite)) {
            SnackbarResult.ActionPerformed -> failure.retry()
            SnackbarResult.Dismissed -> onSettingsFailureDismissed(failure.id)
        }
    }
    CompositionLocalProvider(LocalNavigationStyle provides settings.navStyle) {
      Scaffold(
          containerColor = if (current == Destination.Home) homeBackground.value else materialBackground,
          contentColor = MaterialTheme.colorScheme.onBackground,
          snackbarHost = { SnackbarHost(snackbar) },
          bottomBar = {
            if (current != Destination.Home) {
              Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = Spacing.space5, vertical = Spacing.space2), contentAlignment = Alignment.CenterEnd) {
                  CompassChip(current, reduced, navigate)
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
                    val direction = slideDirection()
                    if (reduced || direction == null) fadeIn(tween(Motion.fast))
                    else slideIntoContainer(direction, tween(Motion.medium, easing = Motion.standard))
                },
                exitTransition = {
                    val direction = slideDirection()
                    if (reduced || direction == null) fadeOut(tween(Motion.fast))
                    else slideOutOfContainer(direction, tween(Motion.medium, easing = Motion.standard))
                },
            ) {
                composable(Destination.Home.name) {
                    HomeScreen(hintsShownCount = settings.hintsShownCount,
                        onBackgroundColorChanged = onHomeBackgroundChanged,
                        navigationControl = { CompassChip(current, reduced, navigate) }, onNavigate = navigate)
                }
                composable(Destination.Tasks.name) { TasksScreen(navigate, openTaskId, onTaskOpenConsumed) }
                composable(Destination.Timeline.name) { TimelineScreen(navigate) }
                composable(Destination.Vault.name) { VaultScreen(navigate) { scope.launch { snackbar.showSnackbar(comingSoon) } } }
                composable(Destination.Sync.name) { SyncScreen(navigate) }
                composable(Destination.Settings.name) { SettingsScreen(settings, onTheme, onDisplayName, onNavigationStyle, navigate, backupContent) }
            }
        }
      }
    }
}

private fun destination(entry: NavBackStackEntry?): Destination =
    Destination.entries.firstOrNull { it.name == entry?.destination?.route } ?: Destination.Home

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideDirection(): AnimatedContentTransitionScope.SlideDirection? {
    val from = SpatialMap.position(destination(initialState)) ?: return null
    val to = SpatialMap.position(destination(targetState)) ?: return null
    return when {
        to.x > from.x -> AnimatedContentTransitionScope.SlideDirection.Left
        to.x < from.x -> AnimatedContentTransitionScope.SlideDirection.Right
        to.y > from.y -> AnimatedContentTransitionScope.SlideDirection.Up
        to.y < from.y -> AnimatedContentTransitionScope.SlideDirection.Down
        else -> null
    }
}
