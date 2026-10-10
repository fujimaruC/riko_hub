package id.riko.hub.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import id.riko.hub.R
import id.riko.hub.RikoHubApp
import id.riko.hub.navigation.Destination
import id.riko.hub.ui.components.TaskEditorSheet
import id.riko.hub.ui.theme.*
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun HomeScreen(hintsShownCount: Int = 0, onBackgroundColorChanged: (Color) -> Unit = {}, navigationControl: @Composable () -> Unit = {}, onNavigate: (Destination) -> Unit) {
    val app = LocalContext.current.applicationContext as RikoHubApp
    val model: HomeViewModel = viewModel(factory = viewModelFactory {
        initializer { HomeViewModel(app.container.tasks, app.container.events, app.container.settings, app.container.clock) }
    })
    HomeRoute(model, hintsShownCount, onBackgroundColorChanged, navigationControl, onNavigate)
}

@Composable
fun HomeRoute(model: HomeViewModel, hintsShownCount: Int = 0, onBackgroundColorChanged: (Color) -> Unit = {}, navigationControl: @Composable () -> Unit = {}, onNavigate: (Destination) -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    val lifecycle by androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    LifecycleStartEffect(model) { model.setActive(true); onStopOrDispose { model.setActive(false) } }
    val reduced = rememberReducedMotion()
    val haptic = LocalHapticFeedback.current
    val snackbar = remember { SnackbarHostState() }
    HomeMessages(model, snackbar)
    val complete: (id.riko.hub.domain.model.Task) -> Unit = {
        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        model.complete(it)
    }
    Box(Modifier.fillMaxSize()) {
        HomeContent(state, reduced, lifecycle == Lifecycle.State.RESUMED, onNavigate,
            { model.edit() }, model::peek, complete, model::pin, model::retryRead, hintsShownCount, onBackgroundColorChanged,
            navigationControl)
        if (state.editor == null && state.peek == null) SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
    val snapshot = state.snapshot
    if (snapshot != null) {
        state.editor?.let { task ->
            TaskEditorSheet(task, snapshot.now, snapshot.zone, state.busy, snackbar,
                model::change, model::save, { model.delete(task) }, model::dismissEditor, quickAdd = state.quickAdd)
        }
        snapshot.ranked.firstOrNull { it.task.id == state.peek }?.let { item ->
            BubblePeekSheet(item.task, snapshot, state.busy, snackbar, { model.peek(null) },
                { complete(item.task) }, { model.start(item.task) }, { model.edit(item.task) }, { model.pin(item.task.id) })
        }
    }
}

@Composable
private fun HomeMessages(model: HomeViewModel, snackbar: SnackbarHostState) {
    val completed = stringResource(R.string.home_completed)
    val deleted = stringResource(R.string.deleted_task)
    val undo = stringResource(R.string.undo)
    val failure = stringResource(R.string.save_failed)
    val retry = stringResource(R.string.retry)
    val accessibility = LocalAccessibilityManager.current
    LaunchedEffect(model, accessibility) {
        model.effects.collect { effect ->
            snackbar.currentSnackbarData?.dismiss()
            when (effect) {
                is HomeEffect.Failed -> if (snackbar.showSnackbar(failure, retry, withDismissAction = true,
                    duration = SnackbarDuration.Indefinite) == SnackbarResult.ActionPerformed) effect.retry()
                else -> {
                    val timeout = accessibility?.calculateRecommendedTimeoutMillis(5_000, containsText = true, containsControls = true) ?: 5_000
                    val result = withTimeoutOrNull(timeout) { snackbar.showSnackbar(
                        if (effect is HomeEffect.Completed) completed else deleted, undo, withDismissAction = true, duration = SnackbarDuration.Indefinite) }
                    if (result == SnackbarResult.ActionPerformed) when (effect) {
                        is HomeEffect.Completed -> model.undo(effect.id, effect.previous)
                        is HomeEffect.Deleted -> model.restore(effect.id)
                        else -> Unit
                    }
                }
            }
        }
    }
}

// TODO(v0.1): Verify real-device drift/frame rate, touch ergonomics, TalkBack and font scales when USB is available.
