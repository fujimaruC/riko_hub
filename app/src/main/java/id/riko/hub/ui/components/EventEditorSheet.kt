package id.riko.hub.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import id.riko.hub.R
import id.riko.hub.domain.model.Category
import id.riko.hub.domain.model.Event
import id.riko.hub.domain.model.EventKind
import id.riko.hub.ui.tasks.label
import id.riko.hub.ui.tasks.taskDateFormat
import id.riko.hub.ui.tasks.taskTimeFormat
import id.riko.hub.ui.theme.RikoHubTheme
import id.riko.hub.ui.theme.RikoShapes
import id.riko.hub.ui.theme.Spacing
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EventEditorSheet(
    event: Event,
    zone: ZoneId,
    busy: Boolean,
    snackbar: SnackbarHostState,
    onChange: (Event) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = { if (!busy) onDismiss() },
        shape = RikoShapes.sheet,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(Modifier.fillMaxWidth().imePadding()) {
            SnackbarHost(snackbar)
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.space5).padding(bottom = Spacing.space6),
                verticalArrangement = Arrangement.spacedBy(Spacing.space4),
            ) {
                Text(
                    stringResource(if (event.id == 0L) R.string.event_new else R.string.event_edit),
                    style = MaterialTheme.typography.titleLarge,
                )
                EventTitle(event, busy, onChange, onSave)
                TaskChoices(stringResource(R.string.event_kind), EventKind.entries, event.kind, { it.eventKindLabel() }, !busy) {
                    onChange(event.copy(kind = it))
                }
                TaskChoices(stringResource(R.string.event_category), Category.entries, event.category, { it.label() }, !busy) {
                    onChange(event.copy(category = it))
                }

                val start = Instant.ofEpochMilli(event.startAt).atZone(zone)
                EventTimeFields(
                    label = stringResource(R.string.event_start),
                    date = start.toLocalDate(),
                    time = start.toLocalTime(),
                    enabled = !busy,
                    onDateChange = { date ->
                        onChange(event.copy(startAt = date.atTime(start.toLocalTime()).atZone(zone).toInstant().toEpochMilli()))
                    },
                    onTimeChange = { time ->
                        onChange(event.copy(startAt = start.toLocalDate().atTime(time).atZone(zone).toInstant().toEpochMilli()))
                    },
                )

                if (event.endAt == null) {
                    TextButton(
                        onClick = { onChange(event.copy(endAt = start.plusHours(1).toInstant().toEpochMilli())) },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touch),
                    ) { Text(stringResource(R.string.event_add_end)) }
                } else {
                    val end = Instant.ofEpochMilli(event.endAt).atZone(zone)
                    EventTimeFields(
                        label = stringResource(R.string.event_end),
                        date = end.toLocalDate(),
                        time = end.toLocalTime(),
                        enabled = !busy,
                        onDateChange = { date ->
                            onChange(event.copy(endAt = date.atTime(end.toLocalTime()).atZone(zone).toInstant().toEpochMilli()))
                        },
                        onTimeChange = { time ->
                            onChange(event.copy(endAt = end.toLocalDate().atTime(time).atZone(zone).toInstant().toEpochMilli()))
                        },
                    )
                    if (event.endAt < event.startAt) {
                        Text(
                            stringResource(R.string.event_end_before_start),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    TextButton(
                        onClick = { onChange(event.copy(endAt = null)) },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touch),
                    ) { Text(stringResource(R.string.event_remove_end)) }
                }

                OutlinedTextField(
                    value = event.location.orEmpty(),
                    onValueChange = { onChange(event.copy(location = it)) },
                    label = { Text(stringResource(R.string.event_location)) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().testTag("event-location"),
                )
                val notesInvalid = (event.notes?.length ?: 0) > 2000
                OutlinedTextField(
                    value = event.notes.orEmpty(),
                    onValueChange = { onChange(event.copy(notes = it)) },
                    label = { Text(stringResource(R.string.event_notes)) },
                    enabled = !busy,
                    isError = notesInvalid,
                    supportingText = {
                        Text(stringResource(if (notesInvalid) R.string.event_notes_too_long else R.string.event_notes_help))
                    },
                    modifier = Modifier.fillMaxWidth().testTag("event-notes"),
                    minLines = 2,
                )
                Button(
                    onClick = onSave,
                    enabled = !busy && validEventInput(event),
                    shape = RikoShapes.pill,
                    modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touch).testTag("event-save"),
                ) {
                    Text(stringResource(if (busy) R.string.saving else R.string.save))
                }
                if (event.id != 0L) {
                    TextButton(
                        onClick = onDelete,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touch),
                    ) { Text(stringResource(R.string.event_delete)) }
                }
                TextButton(
                    onClick = onDismiss,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touch),
                ) { Text(stringResource(R.string.close)) }
            }
        }
    }
}

private fun validEventInput(event: Event) =
    event.title.trim().length in 1..120 &&
        (event.notes?.length ?: 0) <= 2000 &&
        (event.endAt == null || event.endAt >= event.startAt)

@Composable
private fun EventTitle(event: Event, busy: Boolean, onChange: (Event) -> Unit, onSave: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(event.id) { if (event.id == 0L) focus.requestFocus() }
    val titleLength = event.title.trim().length
    val titleMessage = when {
        titleLength == 0 -> R.string.event_title_required_error
        titleLength > 120 -> R.string.event_title_too_long
        else -> R.string.event_title_help
    }
    OutlinedTextField(
        value = event.title,
        onValueChange = { onChange(event.copy(title = it)) },
        enabled = !busy,
        label = { Text(stringResource(R.string.event_title)) },
        isError = titleLength !in 1..120,
        supportingText = { Text(stringResource(titleMessage)) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { if (!busy && validEventInput(event)) onSave() }),
        modifier = Modifier.fillMaxWidth().focusRequester(focus).testTag("event-title"),
    )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun EventTimeFields(
    label: String,
    date: LocalDate,
    time: LocalTime,
    enabled: Boolean,
    onDateChange: (LocalDate) -> Unit,
    onTimeChange: (LocalTime) -> Unit,
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
            TextButton(
                onClick = {
                    DatePickerDialog(context, { _, year, month, day ->
                        onDateChange(LocalDate.of(year, month + 1, day))
                    }, date.year, date.monthValue - 1, date.dayOfMonth).show()
                },
                enabled = enabled,
                modifier = Modifier.heightIn(min = Spacing.touch),
            ) { Text(stringResource(R.string.event_date_button, date.format(taskDateFormat))) }
            TextButton(
                onClick = {
                    TimePickerDialog(context, { _, hour, minute ->
                        onTimeChange(LocalTime.of(hour, minute))
                    }, time.hour, time.minute, true).show()
                },
                enabled = enabled,
                modifier = Modifier.heightIn(min = Spacing.touch),
            ) { Text(stringResource(R.string.event_time_button, time.format(taskTimeFormat))) }
        }
    }
}

fun EventKind.eventKindLabel() = when (this) {
    EventKind.CLASS -> R.string.event_kind_class
    EventKind.EXAM -> R.string.event_kind_exam
    EventKind.MEETING -> R.string.event_kind_meeting
    EventKind.ACTIVITY -> R.string.event_kind_activity
    EventKind.NOTE -> R.string.event_kind_note
    EventKind.OTHER -> R.string.event_kind_other
}

@Preview
@Composable
private fun EventEditorSheetPreview() = RikoHubTheme {
    Surface {
        EventEditorSheet(
            event = Event(title = "", startAt = Instant.parse("2026-10-06T01:00:00Z").toEpochMilli()),
            zone = ZoneId.of("Asia/Makassar"),
            busy = false,
            snackbar = remember { SnackbarHostState() },
            onChange = {}, onSave = {}, onDelete = {}, onDismiss = {},
        )
    }
}
