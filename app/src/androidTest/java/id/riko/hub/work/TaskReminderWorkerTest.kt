package id.riko.hub.work

import android.Manifest
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkInfo
import androidx.work.WorkManager
import id.riko.hub.R
import id.riko.hub.core.time.Clock
import id.riko.hub.data.local.RikoDatabase
import id.riko.hub.data.mapper.toDomain
import id.riko.hub.data.mapper.toEntity
import id.riko.hub.data.repository.TaskRepository
import id.riko.hub.domain.logic.TaskReminderSchedule
import id.riko.hub.domain.model.Recurrence
import id.riko.hub.domain.model.Task
import id.riko.hub.domain.model.TaskStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.time.ZoneId

class TaskReminderWorkerTest {
    private class TestClock(var time: Long) : Clock {
        override fun now() = time
        override fun zone() = ZoneId.of("UTC")
    }

    @Test fun deliveryRechecksPersistedTaskBeforePosting() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, RikoDatabase::class.java).build()
        try {
            val clock = TestClock(System.currentTimeMillis())
            val deadline = clock.now() + 60_000
            val tasks = TaskRepository(database, clock)
            val id = tasks.upsert(Task(title = "Original", deadlineAt = deadline, reminderMinutesBefore = 0))
            val token = TaskReminderSchedule(id, deadline, 0, deadline)
            assertEquals(token, TaskReminderWorker.scheduleFromTags(setOf(TaskReminderWorker.scheduleTag(token))))
            var postedTitle: String? = null
            assertEquals(ReminderDelivery.TOO_EARLY, deliverTaskReminder(database, clock, token) { postedTitle = it.title; true })
            assertNull(postedTitle)
            clock.time = deadline
            val saved = requireNotNull(database.tasks().adminFindForWrite(id)).toDomain()
            tasks.upsert(saved.copy(title = "Latest title"))
            assertEquals(ReminderDelivery.DELIVERED, deliverTaskReminder(database, clock, token) { postedTitle = it.title; true })
            assertEquals("Latest title", postedTitle)
            assertEquals(ReminderDelivery.NOTIFICATIONS_DISABLED, deliverTaskReminder(database, clock, token) { false })

            tasks.upsert(requireNotNull(database.tasks().adminFindForWrite(id)).toDomain().copy(deadlineAt = deadline + 60_000, reminderMinutesBefore = 1))
            val editedToken = TaskReminderSchedule(id, deadline + 60_000, 1, deadline)
            assertEquals(ReminderDelivery.STALE, deliverTaskReminder(database, clock, token) { postedTitle = "stale"; true })
            tasks.setStatus(id, TaskStatus.DONE)
            assertEquals(ReminderDelivery.STALE, deliverTaskReminder(database, clock, editedToken) { postedTitle = "stale"; true })
            tasks.setStatus(id, TaskStatus.NOT_STARTED)
            tasks.softDelete(id)
            assertEquals(ReminderDelivery.STALE, deliverTaskReminder(database, clock, editedToken) { postedTitle = "stale"; true })
            tasks.restore(id)
            assertEquals(ReminderDelivery.DELIVERED, deliverTaskReminder(database, clock, editedToken) { postedTitle = it.title; true })
            assertEquals("Latest title", postedTitle)
        } finally { database.close() }
    }

    @Test fun reconciliationKeepsReplacesCancelsAndRestoresOnlyFixtureWork() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, RikoDatabase::class.java).build()
        val work = WorkManager.getInstance(context)
        val id = fixtureId()
        val name = TaskReminders.reminderWorkName(id)
        val clock = TestClock(System.currentTimeMillis())
        val cancelled = mutableListOf<Long>()
        try {
            assertTrue(work.getWorkInfosForUniqueWork(name).get().isEmpty())
            val tasks = TaskRepository(database, clock)
            database.tasks().insert(Task(id = id, title = "Fixture", deadlineAt = clock.now() + 2 * DAY,
                createdAt = clock.now(), updatedAt = clock.now(), reminderMinutesBefore = 0).toEntity())
            reconcileTaskReminders(database, clock, work) { cancelled.add(it); Unit }
            val first = activeInfo(work, id)
            reconcileTaskReminders(database, clock, work) { cancelled.add(it); Unit }
            assertEquals(first.id, activeInfo(work, id).id)

            tasks.upsert(requireNotNull(database.tasks().adminFindForWrite(id)).toDomain().copy(
                deadlineAt = clock.now() + 3 * DAY, reminderMinutesBefore = 10,
            ))
            reconcileTaskReminders(database, clock, work) { cancelled.add(it); Unit }
            assertNotEquals(first.id, activeInfo(work, id).id)

            tasks.setStatus(id, TaskStatus.DONE)
            reconcileTaskReminders(database, clock, work) { cancelled.add(it); Unit }
            assertTrue(activeInfos(work, id).isEmpty())
            assertTrue(id in cancelled)
            tasks.setStatus(id, TaskStatus.NOT_STARTED)
            reconcileTaskReminders(database, clock, work) { cancelled.add(it); Unit }
            assertNotEquals(first.id, activeInfo(work, id).id)

            tasks.softDelete(id)
            reconcileTaskReminders(database, clock, work) { cancelled.add(it); Unit }
            assertTrue(activeInfos(work, id).isEmpty())
            tasks.restore(id)
            reconcileTaskReminders(database, clock, work) { cancelled.add(it); Unit }
            assertTrue(activeInfo(work, id).state == WorkInfo.State.ENQUEUED)
        } finally {
            work.cancelUniqueWork(name).result.get()
            context.getSystemService(NotificationManager::class.java).cancel("task:$id", 0)
            database.close()
        }
    }

    @Test fun completionSchedulesRecurringChildIndependently() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, RikoDatabase::class.java).build()
        val work = WorkManager.getInstance(context)
        val parentId = fixtureId()
        val ids = mutableListOf(parentId)
        val clock = TestClock(System.currentTimeMillis())
        try {
            val tasks = TaskRepository(database, clock)
            val parent = Task(id = parentId, title = "Recurring fixture", deadlineAt = clock.now() + 2 * DAY,
                createdAt = clock.now(), updatedAt = clock.now(), reminderMinutesBefore = 0, recurrence = Recurrence.DAILY)
            val parentName = TaskReminders.reminderWorkName(parentId)
            assertTrue(work.getWorkInfosForUniqueWork(parentName).get().isEmpty())
            database.tasks().insert(parent.toEntity())
            tasks.setStatus(parentId, TaskStatus.DONE)
            val childId = requireNotNull(database.tasks().findGeneratedSuccessor(parentId)).id
            ids += childId
            assertTrue(work.getWorkInfosForUniqueWork(TaskReminders.reminderWorkName(childId)).get().isEmpty())
            val cancelled = mutableListOf<Long>()
            reconcileTaskReminders(database, clock, work) { cancelled.add(it); Unit }
            assertTrue(cancelled.contains(parentId))
            assertEquals(WorkInfo.State.ENQUEUED, activeInfo(work, childId).state)
        } finally {
            ids.forEach { work.cancelUniqueWork(TaskReminders.reminderWorkName(it)).result.get() }
            database.close()
        }
    }

    @Test fun notificationUsesTaskSpecificTagTitleAndTapIntentWhenEnabled() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = context.getSystemService(NotificationManager::class.java)
        assumeTrue(manager.areNotificationsEnabled())
        assumeTrue(Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
        assumeTrue(Build.VERSION.SDK_INT < 26 || manager.getNotificationChannel(TaskReminders.CHANNEL_ID)?.importance != NotificationManager.IMPORTANCE_NONE)
        val id = fixtureId()
        try {
            assertTrue(postReminder(context, Task(id = id, title = "Reminder fixture", deadlineAt = System.currentTimeMillis())))
            val posted = manager.activeNotifications.single { it.tag == "task:$id" && it.id == 0 }.notification
            assertEquals(TaskReminders.CHANNEL_ID, posted.channelId)
            assertEquals(context.getString(R.string.reminder_notification_title), posted.extras.getString(android.app.Notification.EXTRA_TITLE))
            assertEquals("Reminder fixture", posted.extras.getString(android.app.Notification.EXTRA_TEXT))
            assertNotNull(posted.contentIntent)
        } finally { manager.cancel("task:$id", 0) }
    }

    private fun activeInfo(work: WorkManager, taskId: Long) = requireNotNull(activeInfos(work, taskId).singleOrNull())
    private fun activeInfos(work: WorkManager, taskId: Long) = work.getWorkInfosForUniqueWork(TaskReminders.reminderWorkName(taskId)).get()
        .filter { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.BLOCKED }
    private fun fixtureId() = 8_000_000_000_000_000L + System.nanoTime()

    companion object { private const val DAY = 86_400_000L }
}
