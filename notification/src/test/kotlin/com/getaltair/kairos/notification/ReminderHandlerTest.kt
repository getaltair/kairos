package com.getaltair.kairos.notification

import android.app.NotificationManager
import android.content.Context
import com.getaltair.kairos.data.dao.HabitNotificationDao
import com.getaltair.kairos.data.entity.HabitNotificationEntity
import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.entity.UserPreferences
import com.getaltair.kairos.domain.enums.AnchorType
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.HabitFrequency
import com.getaltair.kairos.domain.repository.HabitRepository
import com.getaltair.kairos.domain.repository.PreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalTime
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderHandlerTest {

    private val preferencesRepository: PreferencesRepository = mockk()
    private val habitRepository: HabitRepository = mockk()
    private val habitNotificationDao: HabitNotificationDao = mockk()
    private val habitReminderBuilder: HabitReminderBuilder = mockk(relaxed = true)
    private val notificationScheduler: NotificationScheduler = mockk(relaxed = true)
    private val quietHoursChecker: QuietHoursChecker = mockk()

    private val notifManager: NotificationManager = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true) {
        every { getSystemService(Context.NOTIFICATION_SERVICE) } returns notifManager
    }

    private lateinit var handler: ReminderHandler

    private val habitId = UUID.fromString("11111111-1111-1111-1111-111111111111")

    private val sampleHabit = Habit(
        id = habitId,
        name = "Take medication",
        anchorBehavior = "After brushing your teeth",
        anchorType = AnchorType.AfterBehavior,
        category = HabitCategory.Morning,
        frequency = HabitFrequency.Daily,
    )

    private val enabledPrefs = UserPreferences(
        notificationEnabled = true,
        quietHoursEnabled = false,
    )

    private val disabledPrefs = UserPreferences(
        notificationEnabled = false,
        quietHoursEnabled = false,
    )

    private val quietHoursPrefs = UserPreferences(
        notificationEnabled = true,
        quietHoursEnabled = true,
        quietHoursStart = LocalTime.of(22, 0),
        quietHoursEnd = LocalTime.of(7, 0),
    )

    @Before
    fun setUp() {
        handler = ReminderHandler(
            preferencesRepository = preferencesRepository,
            habitRepository = habitRepository,
            habitNotificationDao = habitNotificationDao,
            habitReminderBuilder = habitReminderBuilder,
            notificationScheduler = notificationScheduler,
            quietHoursChecker = quietHoursChecker,
        )
    }

    // -------------------------------------------------------------------------
    // 1. handle returns NotificationsDisabled when global notifications off
    // -------------------------------------------------------------------------

    @Test
    fun `handle returns NotificationsDisabled when global notifications off`() = runTest {
        coEvery { preferencesRepository.get() } returns Result.Success(disabledPrefs)

        val result = handler.handle(context, habitId, 0)

        assertEquals(ReminderResult.NotificationsDisabled, result)
        verify(exactly = 0) { notifManager.notify(any(), any()) }
    }

    // -------------------------------------------------------------------------
    // 2. handle defers to quiet hours end when in quiet hours
    // -------------------------------------------------------------------------

    @Test
    fun `handle defers to quiet hours end when in quiet hours`() = runTest {
        coEvery { preferencesRepository.get() } returns Result.Success(quietHoursPrefs)
        every {
            quietHoursChecker.isInQuietHours(any(), any(), any())
        } returns true
        every {
            quietHoursChecker.getNextDeliveryTime(any())
        } returns LocalTime.of(7, 1)

        val result = handler.handle(context, habitId, 0)

        assertEquals(ReminderResult.DeferredToQuietHoursEnd, result)
        verify { notificationScheduler.scheduleAtTime(habitId, LocalTime.of(7, 1)) }
        verify(exactly = 0) { notifManager.notify(any(), any()) }
    }

    // -------------------------------------------------------------------------
    // 3. handle posts initial reminder when not in quiet hours
    // -------------------------------------------------------------------------

    @Test
    fun `handle posts initial reminder when not in quiet hours`() = runTest {
        coEvery { preferencesRepository.get() } returns Result.Success(enabledPrefs)
        coEvery { habitRepository.getById(habitId) } returns Result.Success(sampleHabit)
        coEvery { habitNotificationDao.getForHabit(habitId) } returns null

        val result = handler.handle(context, habitId, 0)

        assertEquals(ReminderResult.NotificationPosted, result)
        val expectedNotifId = NotificationIdStrategy.reminderId(habitId)
        verify { notifManager.notify(expectedNotifId, any()) }
    }

    // -------------------------------------------------------------------------
    // 4. handle posts follow-up notification for valid follow-up number
    // -------------------------------------------------------------------------

    @Test
    fun `handle posts follow-up notification for valid follow-up number`() = runTest {
        coEvery { preferencesRepository.get() } returns Result.Success(enabledPrefs)
        coEvery { habitRepository.getById(habitId) } returns Result.Success(sampleHabit)
        coEvery { habitNotificationDao.getForHabit(habitId) } returns null

        val result = handler.handle(context, habitId, 2)

        assertEquals(ReminderResult.NotificationPosted, result)
        val expectedNotifId = NotificationIdStrategy.followUpId(habitId, 2)
        verify { notifManager.notify(expectedNotifId, any()) }
    }

    // -------------------------------------------------------------------------
    // 5. handle schedules next follow-up when persistent and within max
    // -------------------------------------------------------------------------

    @Test
    fun `handle schedules next follow-up when persistent and within max`() = runTest {
        coEvery { preferencesRepository.get() } returns Result.Success(enabledPrefs)
        coEvery { habitRepository.getById(habitId) } returns Result.Success(sampleHabit)

        val notifEntity = HabitNotificationEntity(
            habitId = habitId,
            time = LocalTime.of(9, 0),
            isEnabled = true,
            isPersistent = true,
            maxFollowUps = 3,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
        coEvery { habitNotificationDao.getForHabit(habitId) } returns notifEntity

        val result = handler.handle(context, habitId, 1)

        assertEquals(ReminderResult.NotificationPosted, result)
        // Follow-up 1 should schedule follow-up 2
        verify { notificationScheduler.scheduleFollowUp(habitId, 2) }
    }

    // -------------------------------------------------------------------------
    // 6. handle does not schedule follow-up when at max
    // -------------------------------------------------------------------------

    @Test
    fun `handle does not schedule follow-up when at max`() = runTest {
        coEvery { preferencesRepository.get() } returns Result.Success(enabledPrefs)
        coEvery { habitRepository.getById(habitId) } returns Result.Success(sampleHabit)

        val notifEntity = HabitNotificationEntity(
            habitId = habitId,
            time = LocalTime.of(9, 0),
            isEnabled = true,
            isPersistent = true,
            maxFollowUps = 3,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
        coEvery { habitNotificationDao.getForHabit(habitId) } returns notifEntity

        // Follow-up 3 is the last one (maxFollowUps = 3)
        val result = handler.handle(context, habitId, 3)

        assertEquals(ReminderResult.NotificationPosted, result)
        // Should NOT schedule follow-up 4 since 4 > maxFollowUps
        verify(exactly = 0) { notificationScheduler.scheduleFollowUp(habitId, 4) }
    }

    // -------------------------------------------------------------------------
    // 7. handle re-schedules for next day after initial reminder
    // -------------------------------------------------------------------------

    @Test
    fun `handle re-schedules for next day after initial reminder`() = runTest {
        coEvery { preferencesRepository.get() } returns Result.Success(enabledPrefs)
        coEvery { habitRepository.getById(habitId) } returns Result.Success(sampleHabit)

        val notifEntity = HabitNotificationEntity(
            habitId = habitId,
            time = LocalTime.of(9, 0),
            isEnabled = true,
            isPersistent = false,
            maxFollowUps = 3,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
        coEvery { habitNotificationDao.getForHabit(habitId) } returns notifEntity

        val result = handler.handle(context, habitId, 0)

        assertEquals(ReminderResult.NotificationPosted, result)
        // Should re-schedule for tomorrow at the same time
        verify { notificationScheduler.scheduleReminder(habitId, LocalTime.of(9, 0)) }
    }

    // -------------------------------------------------------------------------
    // 8. handle returns HabitNotFound when habit does not exist
    // -------------------------------------------------------------------------

    @Test
    fun `handle returns HabitNotFound when habit does not exist`() = runTest {
        coEvery { preferencesRepository.get() } returns Result.Success(enabledPrefs)
        coEvery { habitRepository.getById(habitId) } returns Result.Error("Habit not found")

        val result = handler.handle(context, habitId, 0)

        assertTrue(result is ReminderResult.HabitNotFound)
        assertEquals("Habit not found", (result as ReminderResult.HabitNotFound).message)
        verify(exactly = 0) { notifManager.notify(any(), any()) }
    }

    // -------------------------------------------------------------------------
    // 9. handle returns PreferencesError when preferences fail to load
    // -------------------------------------------------------------------------

    @Test
    fun `handle returns PreferencesError when preferences fail to load`() = runTest {
        coEvery { preferencesRepository.get() } returns Result.Error("DB error")

        val result = handler.handle(context, habitId, 0)

        assertTrue(result is ReminderResult.PreferencesError)
        assertEquals("DB error", (result as ReminderResult.PreferencesError).message)
        verify(exactly = 0) { notifManager.notify(any(), any()) }
    }

    // -------------------------------------------------------------------------
    // 10. handle schedules first follow-up for persistent initial reminder
    // -------------------------------------------------------------------------

    @Test
    fun `handle schedules first follow-up for persistent initial reminder`() = runTest {
        coEvery { preferencesRepository.get() } returns Result.Success(enabledPrefs)
        coEvery { habitRepository.getById(habitId) } returns Result.Success(sampleHabit)

        val notifEntity = HabitNotificationEntity(
            habitId = habitId,
            time = LocalTime.of(9, 0),
            isEnabled = true,
            isPersistent = true,
            maxFollowUps = 3,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
        coEvery { habitNotificationDao.getForHabit(habitId) } returns notifEntity

        val result = handler.handle(context, habitId, 0)

        assertEquals(ReminderResult.NotificationPosted, result)
        // Initial reminder with persistent = true should schedule follow-up 1
        verify { notificationScheduler.scheduleFollowUp(habitId, 1) }
        // And also re-schedule for tomorrow
        verify { notificationScheduler.scheduleReminder(habitId, LocalTime.of(9, 0)) }
    }
}
