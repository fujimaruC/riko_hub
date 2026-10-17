package id.riko.hub.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import id.riko.hub.domain.model.Course
import id.riko.hub.domain.model.CourseWeeklySlot
import id.riko.hub.domain.model.normalizeAndValidateCourse

@Entity(tableName = "courses", indices = [
    Index(value = ["syncId"], unique = true, name = "index_courses_syncId"),
    Index(value = ["archivedAt", "sortOrder"], name = "index_courses_archivedAt_sortOrder"),
])
data class CourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val syncId: String,
    val title: String,
    val code: String?,
    val instructor: String?,
    val academicPeriod: String?,
    val color: String?,
    val icon: String?,
    val sortOrder: Long,
    val archivedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "course_weekly_slots", foreignKeys = [ForeignKey(
    entity = CourseEntity::class,
    parentColumns = ["id"],
    childColumns = ["courseId"],
    onDelete = ForeignKey.CASCADE,
)], indices = [Index(value = ["courseId"], name = "index_course_weekly_slots_courseId")])
data class CourseWeeklySlotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val dayOfWeek: Int,
    val startMinute: Int,
    val endMinute: Int,
    val location: String?,
)

data class CourseWithWeeklySlots(
    @androidx.room.Embedded val course: CourseEntity,
    @Relation(parentColumn = "id", entityColumn = "courseId") val weeklySlots: List<CourseWeeklySlotEntity>,
)

fun CourseWithWeeklySlots.toDomain() = normalizeAndValidateCourse(Course(
    id = course.id,
    syncId = course.syncId,
    title = course.title,
    code = course.code,
    instructor = course.instructor,
    academicPeriod = course.academicPeriod,
    color = course.color,
    icon = course.icon,
    sortOrder = course.sortOrder,
    archivedAt = course.archivedAt,
    createdAt = course.createdAt,
    updatedAt = course.updatedAt,
    weeklySlots = weeklySlots.sortedWith(compareBy<CourseWeeklySlotEntity> { it.dayOfWeek }.thenBy { it.startMinute }.thenBy { it.id })
        .map { CourseWeeklySlot(it.dayOfWeek, it.startMinute, it.endMinute, it.location) },
))

fun Course.toEntity() = normalizeAndValidateCourse(this).let { course ->
    CourseEntity(course.id, course.syncId, course.title, course.code, course.instructor, course.academicPeriod,
        course.color, course.icon, course.sortOrder, course.archivedAt, course.createdAt, course.updatedAt)
}

fun Course.toSlotEntities(courseId: Long) = normalizeAndValidateCourse(this).weeklySlots.map {
    CourseWeeklySlotEntity(courseId = courseId, dayOfWeek = it.dayOfWeek, startMinute = it.startMinute,
        endMinute = it.endMinute, location = it.location)
}
