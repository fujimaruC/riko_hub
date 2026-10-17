package id.riko.hub.ui.tasks

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import id.riko.hub.R
import id.riko.hub.RikoHubApp
import id.riko.hub.domain.logic.TaskBucket
import id.riko.hub.domain.model.*
import id.riko.hub.navigation.Destination
import id.riko.hub.ui.components.*
import id.riko.hub.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun TasksScreen(onNavigate: (Destination) -> Unit, openTaskId: Long? = null, onTaskOpenConsumed: () -> Unit = {}) {
    val app = LocalContext.current.applicationContext as RikoHubApp
    val model: TasksViewModel = viewModel(factory = viewModelFactory {
        initializer { TasksViewModel(app.container.tasks, app.container.clock) }
    })
    TasksRoute(model, onNavigate, openTaskId, onTaskOpenConsumed)
}

@Composable
fun TasksRoute(model: TasksViewModel, onNavigate: (Destination) -> Unit, openTaskId: Long? = null, onTaskOpenConsumed: () -> Unit = {}) {
    val app = LocalContext.current.applicationContext as RikoHubApp
    val reminderPermission = rememberReminderPermissionState { app.container.reminders.requestReconciliation() }
    val state by model.state.collectAsStateWithLifecycle()
    LaunchedEffect(openTaskId, state.loading, state.readFailed, state.busy, state.editor, state.tasks) {
        val id = openTaskId ?: return@LaunchedEffect
        if (!state.loading && !state.readFailed && !state.busy && state.editor == null) {
            state.tasks.firstOrNull { it.id == id }?.let(model::edit)
            onTaskOpenConsumed()
        }
    }
    val snackbar = remember { SnackbarHostState() }
    val deleted = stringResource(R.string.deleted_task)
    val undo = stringResource(R.string.undo)
    val failed = stringResource(R.string.save_failed)
    val retry = stringResource(R.string.retry)
    val accessibility = LocalAccessibilityManager.current
    LaunchedEffect(model, accessibility) {
        model.effects.collect { effect ->
            when (effect) {
                is TaskEffect.Deleted -> {
                    val timeout = accessibility?.calculateRecommendedTimeoutMillis(5_000, containsText = true, containsControls = true) ?: 5_000
                    val result = withTimeoutOrNull(timeout) {
                        snackbar.showSnackbar(deleted, undo, withDismissAction = true, duration = SnackbarDuration.Indefinite)
                    }
                    if (result == SnackbarResult.ActionPerformed) model.restore(effect.id)
                }
                is TaskEffect.Failed -> {
                    if (snackbar.showSnackbar(failed, retry, withDismissAction = true, duration = SnackbarDuration.Indefinite)
                        == SnackbarResult.ActionPerformed) effect.retry()
                }
            }
        }
    }
    Box(Modifier.fillMaxSize()) {
        TasksContent(state, onNavigate, model::filter, model::search, model::clearFilters, model::toggleDone, { model.edit() }, model::edit,
            { task, done -> model.complete(task, done) }, model::delete, model::retryRead)
        if (state.editor == null) SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
    state.editor?.let { task ->
        TaskEditorSheet(task, state.now, state.zone, state.busy, snackbar, model::change, model::save,
            { model.delete(task) }, model::dismissEditor, reminderPermission = reminderPermission)
    }
}

@Composable
fun TasksContent(state: TasksUiState, onNavigate: (Destination) -> Unit, onFilter: (Category?) -> Unit, onSearch: (String) -> Unit,
                 onClearFilters: () -> Unit,
                 onExpand: () -> Unit, onAdd: () -> Unit, onEdit: (Task) -> Unit,
                 onComplete: (Task, Boolean) -> Unit, onDelete: (Task) -> Unit, onRetry: () -> Unit) {
    var showSkeleton by remember { mutableStateOf(false) }
    val reducedMotion = rememberReducedMotion()
    val listState = rememberLazyListState()
    LaunchedEffect(state.loading) {
        showSkeleton = false
        if (state.loading) { delay(150); showSkeleton = true }
    }
    Column(Modifier.fillMaxSize().testTag("screen-Tasks")) {
        LazyColumn(state = listState, modifier = Modifier.weight(1f).testTag("task-list"), contentPadding = PaddingValues(horizontal = Spacing.space5, vertical = Spacing.space3),
            verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
            item {
                Column(Modifier.fillMaxWidth().padding(top = Spacing.space4).testTag("task-workspace-header"),
                    verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                    Text("RUANG 02 / DAFTAR KERJA", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("TUGAS\nTERKUMPUL.", style = MaterialTheme.typography.displayLarge,
                        modifier = Modifier.semantics { heading() })
                    Text(pluralStringResource(R.plurals.tasks_count, state.todayCount, state.todayCount, state.tasks.size),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item { CategoryFilters(state.filter, onFilter) }
            item {
                OutlinedTextField(value = state.query, onValueChange = onSearch, singleLine = true,
                    leadingIcon = { Text("⌕") }, placeholder = { Text(stringResource(R.string.task_search)) },
                    modifier = Modifier.fillMaxWidth().testTag("task-search"), shape = RikoShapes.pill)
            }
            when {
                state.readFailed -> item {
                    Surface(shape = RikoShapes.asymA, color = MaterialTheme.colorScheme.surfaceVariant) {
                        Column(Modifier.fillMaxWidth().padding(Spacing.space4)) {
                            Text(stringResource(R.string.read_failed))
                            TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
                        }
                    }
                }
                state.loading && showSkeleton -> items(4) {
                    Surface(Modifier.fillMaxWidth().height(Spacing.row), shape = RikoShapes.pill, color = MaterialTheme.colorScheme.surfaceVariant) {}
                }
                !state.loading && state.buckets.isEmpty() -> item {
                    TasksEmpty(state.filter != null || state.query.isNotBlank(), onClearFilters)
                }
                !state.loading -> state.buckets.forEach { (bucket, tasks) ->
                    item(key = bucket.name) { BucketHeader(bucket, tasks.size, state.doneExpanded, onExpand) }
                    if (bucket != TaskBucket.DONE || state.doneExpanded) itemsIndexed(tasks, key = { _, task -> task.id }) { index, task ->
                        TaskRow(task, index, state.filter == null, state.now, state.zone, !state.busy,
                            { onEdit(task) }, { onComplete(task, it) }, { onDelete(task) })
                    }
                }
            }
        }
        Button(onClick = onAdd, enabled = !state.busy, shape = RikoShapes.pill,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.space5, vertical = Spacing.space2).heightIn(min = Spacing.touch)) {
            Text(stringResource(R.string.add_task))
        }
    }
}

@Composable
private fun CategoryFilters(selected: Category?, onFilter: (Category?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
        items(listOf(null) + Category.entries) { category ->
            FilterChip(shape = RikoShapes.pill, selected = selected == category, onClick = { onFilter(category) },
                label = { Text(stringResource(category?.label() ?: R.string.all_tasks)) },
                modifier = Modifier.heightIn(min = Spacing.touch).testTag("filter-${category?.name ?: "ALL"}"))
        }
    }
}

@Composable
private fun BucketHeader(bucket: TaskBucket, count: Int, expanded: Boolean, onExpand: () -> Unit) {
    val label = stringResource(R.string.bucket_count, stringResource(bucket.label()), count)
    val description = stringResource(if (expanded) R.string.expanded else R.string.collapsed)
    if (bucket == TaskBucket.DONE) TextButton(onClick = onExpand,
        modifier = Modifier.fillMaxWidth().testTag("done-bucket").semantics { heading(); stateDescription = description }) {
        Text(label, style = MaterialTheme.typography.titleSmall)
    } else Surface(shape = RikoShapes.pill,
        color = if (bucket == TaskBucket.OVERDUE) LocalRikoColors.current.urgent.container else MaterialTheme.colorScheme.background) {
        Text(label, style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = Spacing.space3, vertical = Spacing.space2).semantics { heading() })
    }
}

@Composable
private fun TasksEmpty(filtered: Boolean, onClear: () -> Unit) {
    val accent = LocalRikoColors.current.personal.color
    Surface(shape = RikoShapes.asymA, color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.fillMaxWidth().padding(Spacing.space6), verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
            Canvas(Modifier.size(Spacing.space12)) {
                drawLine(accent, androidx.compose.ui.geometry.Offset(size.width * .5f, size.height),
                    androidx.compose.ui.geometry.Offset(size.width * .5f, size.height * .3f), strokeWidth = size.width * .07f)
                drawOval(accent, topLeft = androidx.compose.ui.geometry.Offset(0f, size.height * .15f),
                    size = androidx.compose.ui.geometry.Size(size.width * .5f, size.height * .3f))
                drawOval(accent, topLeft = androidx.compose.ui.geometry.Offset(size.width * .5f, 0f),
                    size = androidx.compose.ui.geometry.Size(size.width * .5f, size.height * .3f))
            }
            Text(stringResource(R.string.tasks_empty), style = MaterialTheme.typography.titleLarge)
            if (filtered) TextButton(onClick = onClear) { Text(stringResource(R.string.clear_filter)) }
            else Text(stringResource(R.string.tasks_first))
        }
    }
}

@Preview @Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, fontScale = 2f)
@Composable
private fun TasksPreview() = RikoHubTheme {
    Surface { TasksContent(TasksUiState(loading = false), {}, {}, {}, {}, {}, {}, {}, { _, _ -> }, {}, {}) }
}
