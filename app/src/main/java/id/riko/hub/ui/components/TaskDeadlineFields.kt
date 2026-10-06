package id.riko.hub.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import id.riko.hub.R
import id.riko.hub.domain.logic.taskDeadline
import id.riko.hub.domain.model.Task
import id.riko.hub.ui.tasks.*
import id.riko.hub.ui.theme.Spacing
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskDeadlineFields(task: Task, now: Long, zone: ZoneId, enabled: Boolean, onChange: (Task) -> Unit) {
    val context = LocalContext.current
    val dateTime = Instant.ofEpochMilli(task.deadlineAt ?: now).atZone(zone)
    val date = dateTime.toLocalDate()
    val time = if (task.hasDeadlineTime) dateTime.toLocalTime().withSecond(0).withNano(0) else null
    val setDate: (LocalDate) -> Unit = { onChange(task.copy(deadlineAt = taskDeadline(it, time, zone))) }
    Column {
        Text(stringResource(R.string.deadline), style = MaterialTheme.typography.titleSmall)
        Text(deadlineLabel(task, now, zone), style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
            listOf(R.string.today to 0L, R.string.tomorrow to 1L).forEach { (label, offset) ->
                TextButton(onClick = { setDate(Instant.ofEpochMilli(now).atZone(zone).toLocalDate().plusDays(offset)) }, enabled = enabled) {
                    Text(stringResource(label))
                }
            }
            TextButton(enabled = enabled, onClick = {
                DatePickerDialog(context, { _, year, month, day -> setDate(LocalDate.of(year, month + 1, day)) },
                    date.year, date.monthValue - 1, date.dayOfMonth).show()
            }) { Text(stringResource(R.string.choose_date)) }
        }
        if (task.deadlineAt != null) FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
            TextButton(enabled = enabled, onClick = {
                TimePickerDialog(context, { _, hour, minute ->
                    onChange(task.copy(deadlineAt = taskDeadline(date, LocalTime.of(hour, minute), zone), hasDeadlineTime = true))
                }, time?.hour ?: 23, time?.minute ?: 59, true).show()
            }) { Text(stringResource(R.string.choose_time)) }
            if (task.hasDeadlineTime) TextButton(enabled = enabled, onClick = {
                onChange(task.copy(deadlineAt = taskDeadline(date, null, zone), hasDeadlineTime = false))
            }) { Text(stringResource(R.string.date_only)) }
            TextButton(enabled = enabled, onClick = { onChange(task.copy(deadlineAt = null, hasDeadlineTime = false)) }) {
                Text(stringResource(R.string.clear_deadline))
            }
        }
    }
}
