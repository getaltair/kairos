package com.getaltair.kairos.notification

import android.app.AlarmManager
import android.content.Context
import com.getaltair.kairos.data.dao.HabitNotificationDao
import io.mockk.every
import io.mockk.mockk
import java.time.LocalTime
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [NotificationScheduler] focusing on permission-check and validation logic.
 *
 * Tests that exercise the "Scheduled" happy path (alarm is actually set) require
 * PendingIntent.getBroadcast and Intent construction which are Android framework
 * calls not available in JVM unit tests. Those scenarios belong in instrumented tests.
 */
class NotificationSchedulerTest {

    private val alarmManager: AlarmManager = mockk(relaxed = true)
    private val dao: HabitNotificationDao = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true) {
        every { getSystemService(Context.ALARM_SERVICE) } returns alarmManager
    }

    private lateinit var scheduler: NotificationScheduler

    private val habitId = UUID.fromString("22222222-2222-2222-2222-222222222222")

    @Before
    fun setUp() {
        scheduler = NotificationScheduler(context, dao)
    }

    // -------------------------------------------------------------------------
    // scheduleReminder -- permission check
    // -------------------------------------------------------------------------

    @Test
    fun `scheduleReminder returns ExactAlarmDenied when canScheduleExactAlarms is false`() {
        every { alarmManager.canScheduleExactAlarms() } returns false

        val result = scheduler.scheduleReminder(habitId, LocalTime.of(9, 0))

        assertEquals(ScheduleResult.ExactAlarmDenied, result)
    }

    // -------------------------------------------------------------------------
    // scheduleSnooze -- validation and permission check
    // -------------------------------------------------------------------------

    @Test(expected = IllegalArgumentException::class)
    fun `scheduleSnooze throws on zero delay`() {
        scheduler.scheduleSnooze(habitId, 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `scheduleSnooze throws on negative delay`() {
        scheduler.scheduleSnooze(habitId, -5)
    }

    @Test
    fun `scheduleSnooze returns ExactAlarmDenied when canScheduleExactAlarms is false`() {
        every { alarmManager.canScheduleExactAlarms() } returns false

        val result = scheduler.scheduleSnooze(habitId, 10)

        assertEquals(ScheduleResult.ExactAlarmDenied, result)
    }

    // -------------------------------------------------------------------------
    // scheduleFollowUp -- validation and permission check
    // -------------------------------------------------------------------------

    @Test(expected = IllegalArgumentException::class)
    fun `scheduleFollowUp throws for follow-up number 0`() {
        scheduler.scheduleFollowUp(habitId, 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `scheduleFollowUp throws for follow-up number exceeding max`() {
        scheduler.scheduleFollowUp(habitId, NotificationConstants.MAX_FOLLOW_UPS + 1)
    }

    @Test
    fun `scheduleFollowUp returns ExactAlarmDenied when canScheduleExactAlarms is false`() {
        every { alarmManager.canScheduleExactAlarms() } returns false

        val result = scheduler.scheduleFollowUp(habitId, 1)

        assertEquals(ScheduleResult.ExactAlarmDenied, result)
    }

    // -------------------------------------------------------------------------
    // scheduleAtTime -- permission check
    // -------------------------------------------------------------------------

    @Test
    fun `scheduleAtTime returns ExactAlarmDenied when canScheduleExactAlarms is false`() {
        every { alarmManager.canScheduleExactAlarms() } returns false

        val result = scheduler.scheduleAtTime(habitId, LocalTime.of(7, 1))

        assertEquals(ScheduleResult.ExactAlarmDenied, result)
    }
}
