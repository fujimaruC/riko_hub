package id.riko.hub.domain.model

import java.util.UUID

data class Course(
    val id: Long = 0,
    val syncId: String = UUID.randomUUID().toString(),
    val title: String,
    val code: String? = null,
    val instructor: String? = null,
    val academicPeriod: String? = null,
    val color: String? = null,
    val icon: String? = null,
    val sortOrder: Long = 0,
    val archivedAt: Long? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val weeklySlots: List<CourseWeeklySlot> = emptyList(),
)

data class CourseWeeklySlot(
    val dayOfWeek: Int,
    val startMinute: Int,
    val endMinute: Int,
    val location: String? = null,
)

fun normalizeAndValidateCourse(course: Course): Course {
    val title = course.title.trim()
    require(title.length in 1..120) { "Course title must contain 1 to 120 characters" }
    fun optional(value: String?, maxLength: Int, name: String): String? = value?.trim()?.takeIf { it.isNotEmpty() }?.also {
        require(it.length <= maxLength) { "$name must contain at most $maxLength characters" }
    }
    require(course.id >= 0) { "Course id cannot be negative" }
    require(course.syncId.isNotBlank() && course.syncId.length <= 120) { "Course syncId must contain 1 to 120 characters" }
    require(course.sortOrder >= 0) { "Course sortOrder cannot be negative" }
    require(course.createdAt in 0..253402300799999L && course.updatedAt in course.createdAt..253402300799999L) { "Invalid course timestamps" }
    require(course.archivedAt == null || course.archivedAt in 0..course.updatedAt) { "Invalid archive timestamp" }
    require(course.color == null || course.color in setOf("BLUE", "GREEN", "PURPLE", "ORANGE")) { "Unsupported course color" }
    require(course.weeklySlots.size <= 28) { "A course can have at most 28 weekly slots" }
    val slots = course.weeklySlots.map { slot ->
        require(slot.dayOfWeek in 1..7) { "Slot dayOfWeek must be 1 to 7" }
        require(slot.startMinute in 0..1439) { "Slot startMinute must be 0 to 1439" }
        require(slot.endMinute in 1..1440 && slot.endMinute > slot.startMinute) { "Slot endMinute must follow startMinute and be at most 1440" }
        slot.copy(location = optional(slot.location, 120, "Slot location"))
    }
    return course.copy(
        title = title,
        code = optional(course.code, 40, "Course code"),
        instructor = optional(course.instructor, 120, "Instructor"),
        academicPeriod = optional(course.academicPeriod, 120, "Academic period"),
        icon = optional(course.icon, 8, "Course icon"),
        weeklySlots = slots,
    )
}
