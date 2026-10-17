package id.riko.hub.ui.courses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import id.riko.hub.R
import id.riko.hub.domain.model.Course
import id.riko.hub.domain.model.CourseWeeklySlot
import id.riko.hub.ui.components.rikoFormColorScheme
import id.riko.hub.ui.components.rikoFormFieldColors
import id.riko.hub.ui.theme.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CourseEditorSheet(course: Course, busy: Boolean, snackbar: SnackbarHostState, onChange: (Course) -> Unit, onSave: () -> Unit, onDismiss: () -> Unit) {
    val timeDrafts = remember(course.id) { mutableStateMapOf<String, String>() }
    val slots = course.weeklySlots
    val timeValid = slots.indices.all { i ->
        val startDraft = timeDrafts["$i-start"]
        val endDraft = timeDrafts["$i-end"]
        val start = if (startDraft == null) slots[i].startMinute else parseTime(startDraft) ?: return@all false
        val end = if (endDraft == null) slots[i].endMinute else parseTime(endDraft) ?: return@all false
        start in 0..1439 && end in 1..1440 && end > start
    }
    val allowed = validCourse(course) && timeValid
    ModalBottomSheet(onDismissRequest = { if (!busy) onDismiss() }, shape = RikoShapes.sheet, containerColor = Color(0xFF222320),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
      MaterialTheme(colorScheme = rikoFormColorScheme()) {
        Box(Modifier.fillMaxWidth().imePadding()) {
          Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
              .padding(horizontal = Spacing.space5).padding(bottom = Spacing.space6),
              verticalArrangement = Arrangement.spacedBy(Spacing.space3)) {
            Surface(shape = RikoShapes.wide, color = MaterialTheme.colorScheme.surface) {
                Text(stringResource(if (course.id == 0L) R.string.course_new else R.string.course_edit),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth().padding(Spacing.space4))
            }
            CourseTextField(course.title, R.string.course_title, 120, busy, "course-title") { onChange(course.copy(title = it)) }
            CourseTextField(course.code.orEmpty(), R.string.course_code, 40, busy, "course-code") { onChange(course.copy(code = it.ifBlank { null })) }
            CourseTextField(course.instructor.orEmpty(), R.string.course_instructor, 120, busy, "course-instructor") { onChange(course.copy(instructor = it.ifBlank { null })) }
            CourseTextField(course.academicPeriod.orEmpty(), R.string.course_period, 120, busy, "course-period") { onChange(course.copy(academicPeriod = it.ifBlank { null })) }
            CourseTextField(course.icon.orEmpty(), R.string.course_icon, 8, busy, "course-icon") { onChange(course.copy(icon = it.ifBlank { null })) }
            Text(stringResource(R.string.course_color), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                listOf(null, "BLUE", "GREEN", "PURPLE", "ORANGE").forEach { color ->
                    FilterChip(selected = course.color == color, enabled = !busy, onClick = { onChange(course.copy(color = color)) },
                        label = { Text(stringResource(when (color) {
                            null -> R.string.course_color_none; "BLUE" -> R.string.course_color_blue
                            "GREEN" -> R.string.course_color_green; "PURPLE" -> R.string.course_color_purple
                            else -> R.string.course_color_orange
                        })) }, modifier = Modifier.heightIn(min = Spacing.touch))
                }
            }
            Text(stringResource(R.string.course_timetable), style = MaterialTheme.typography.titleMedium)
            slots.forEachIndexed { index, slot ->
                CourseSlotEditor(slot, index, timeDrafts, busy, { updated ->
                    onChange(course.copy(weeklySlots = slots.toMutableList().also { it[index] = updated }))
                }, { field, raw ->
                    timeDrafts["$index-$field"] = raw
                    parseTime(raw)?.let { parsed ->
                        val updated = if (field == "start") slot.copy(startMinute = parsed) else slot.copy(endMinute = parsed)
                        onChange(course.copy(weeklySlots = slots.toMutableList().also { it[index] = updated }))
                    }
                }, {
                    val shifted = timeDrafts.toMap().mapNotNull { (key, value) ->
                        val split = key.split('-', limit = 2)
                        val slotIndex = split[0].toIntOrNull() ?: return@mapNotNull null
                        when {
                            slotIndex == index -> null
                            slotIndex > index -> "${slotIndex - 1}-${split[1]}" to value
                            else -> key to value
                        }
                    }
                    timeDrafts.clear(); timeDrafts.putAll(shifted)
                    onChange(course.copy(weeklySlots = slots.toMutableList().also { it.removeAt(index) }))
                })
            }
            TextButton(onClick = {
                if (slots.size < 28) onChange(course.copy(weeklySlots = slots + CourseWeeklySlot(1, 9 * 60, 10 * 60)))
            }, enabled = !busy && slots.size < 28, modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touch)) {
                Text(stringResource(R.string.course_add_slot))
            }
            if (!allowed) Text(stringResource(R.string.course_validation), color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            Button(onClick = onSave, enabled = !busy && allowed, shape = RikoShapes.pill,
                modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touch).testTag("course-save")) {
                Text(stringResource(if (busy) R.string.saving else R.string.save))
            }
            TextButton(onClick = onDismiss, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touch)) {
                Text(stringResource(R.string.close))
            }
          }
          SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
        }
      }
    }
}

@Composable
private fun CourseTextField(value: String, label: Int, max: Int, busy: Boolean, tag: String, change: (String) -> Unit) {
    OutlinedTextField(value, change, enabled = !busy, label = { Text(stringResource(label)) }, singleLine = true,
        colors = rikoFormFieldColors(),
        isError = value.length > max, supportingText = { if (value.length > max) Text(stringResource(R.string.course_too_long, max)) },
        modifier = Modifier.fillMaxWidth().testTag(tag))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CourseSlotEditor(slot: CourseWeeklySlot, index: Int, drafts: MutableMap<String, String>,
                             busy: Boolean, changeSlot: (CourseWeeklySlot) -> Unit,
                             changeTime: (String, String) -> Unit, remove: () -> Unit) {
    Surface(shape = RikoShapes.asymB, color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.fillMaxWidth().padding(Spacing.space3), verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
            Text(stringResource(R.string.course_slot_number, index + 1), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.space1)) {
                (1..7).forEach { day ->
                    FilterChip(selected = slot.dayOfWeek == day, enabled = !busy, onClick = { changeSlot(slot.copy(dayOfWeek = day)) },
                        label = { Text(stringResource(day.dayLabel())) }, modifier = Modifier.heightIn(min = Spacing.touch))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                listOf("start" to R.string.course_start, "end" to R.string.course_end).forEach { (key, label) ->
                    OutlinedTextField(value = drafts["$index-$key"] ?: formatTime(if (key == "start") slot.startMinute else slot.endMinute),
                        onValueChange = { changeTime(key, it) }, enabled = !busy, label = { Text(stringResource(label)) },
                        colors = rikoFormFieldColors(),
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                        modifier = Modifier.fillMaxWidth().testTag("course-slot-$index-$key"))
                }
            }
            CourseTextField(slot.location.orEmpty(), R.string.course_location, 120, busy, "course-slot-$index-location") {
                changeSlot(slot.copy(location = it.ifBlank { null }))
            }
            TextButton(onClick = remove, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touch)) {
                Text(stringResource(R.string.course_remove_slot))
            }
        }
    }
}
