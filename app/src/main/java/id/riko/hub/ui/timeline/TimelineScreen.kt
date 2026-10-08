package id.riko.hub.ui.timeline

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import id.riko.hub.R
import id.riko.hub.RikoHubApp
import id.riko.hub.navigation.Destination
import id.riko.hub.ui.components.*
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.collectLatest

@Composable
fun TimelineScreen(onNavigate: (Destination) -> Unit) {
    val app = LocalContext.current.applicationContext as RikoHubApp
    val model: TimelineViewModel = viewModel(factory = viewModelFactory {
        initializer { TimelineViewModel(app.container.tasks, app.container.events, app.container.clock) }
    })
    TimelineRoute(model, onNavigate)
}

@Composable
fun TimelineRoute(model: TimelineViewModel, onNavigate: (Destination) -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    LifecycleStartEffect(model) { model.setActive(true); onStopOrDispose { model.setActive(false) } }
    val snackbar = remember { SnackbarHostState() }
    TimelineMessages(model, snackbar)
    Box(Modifier.fillMaxSize()) {
        TimelineContent(state, onNavigate, model::moveWeek, model::currentWeek, model::toggleDayMode,
            model::open, model::addEvent, model::retryRead)
        if (state.eventEditor == null && state.taskEditor == null) SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
    state.eventEditor?.let { event ->
        EventEditorSheet(event, state.zone, state.busy, snackbar, model::changeEvent, model::saveEvent, model::deleteEvent, model::dismiss)
    }
    state.taskEditor?.let { task ->
        TaskEditorSheet(task, state.now, state.zone, state.busy, snackbar,
            model::changeTask, model::saveTask, model::deleteTask, model::dismiss)
    }
}

@Composable
private fun TimelineMessages(model: TimelineViewModel, snackbar: SnackbarHostState) {
    val deletedEvent = stringResource(R.string.timeline_deleted_event)
    val deletedTask = stringResource(R.string.deleted_task)
    val failure = stringResource(R.string.save_failed)
    val undo = stringResource(R.string.undo)
    val retry = stringResource(R.string.retry)
    val accessibility = LocalAccessibilityManager.current
    LaunchedEffect(model, accessibility) {
        model.effects.collectLatest { effect ->
            when (effect) {
                TimelineEffect.Clear -> snackbar.currentSnackbarData?.dismiss()
                is TimelineEffect.Deleted -> {
                    val timeout = accessibility?.calculateRecommendedTimeoutMillis(5_000, containsText = true, containsControls = true) ?: 5_000
                    val result = withTimeoutOrNull(timeout) { snackbar.showSnackbar(
                        if (effect.event) deletedEvent else deletedTask, undo, withDismissAction = true, duration = SnackbarDuration.Indefinite) }
                    if (result == SnackbarResult.ActionPerformed) model.restore(effect.id, effect.event)
                }
                is TimelineEffect.Failed -> if (snackbar.showSnackbar(failure, retry, withDismissAction = true,
                    duration = SnackbarDuration.Indefinite) == SnackbarResult.ActionPerformed) effect.retry()
            }
        }
    }
}
