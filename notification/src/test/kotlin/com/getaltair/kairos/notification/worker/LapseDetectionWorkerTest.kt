package com.getaltair.kairos.notification.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.getaltair.kairos.domain.common.Result as DomainResult
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.enums.AnchorType
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.HabitFrequency
import com.getaltair.kairos.domain.enums.HabitPhase
import com.getaltair.kairos.domain.repository.HabitRepository
import com.getaltair.kairos.domain.usecase.DetectLapsesUseCase
import com.getaltair.kairos.notification.RecoveryNotificationBuilder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest

/**
 * Unit tests for [LapseDetectionWorker].
 *
 * Verifies the worker calls [DetectLapsesUseCase], loads habit details,
 * and posts the correct notifications for lapse/relapse phases.
 */
class LapseDetectionWorkerTest : KoinTest {

    private val mockContext: Context = mockk(relaxed = true)
    private val mockParams: WorkerParameters = mockk(relaxed = true) {
        every { runAttemptCount } returns 0
    }
    private val mockDetectLapses: DetectLapsesUseCase = mockk()
    private val mockHabitRepository: HabitRepository = mockk()
    private val mockNotificationBuilder: RecoveryNotificationBuilder = mockk(relaxed = true)

    private val habitId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")
    private val lapsedHabit = Habit(
        id = habitId,
        name = "Take medication",
        anchorBehavior = "After breakfast",
        anchorType = AnchorType.AfterBehavior,
        category = HabitCategory.Morning,
        frequency = HabitFrequency.Daily,
        phase = HabitPhase.LAPSED
    )

    private fun setupKoin() {
        stopKoin()
        startKoin {
            modules(
                module {
                    single { mockDetectLapses }
                    single { mockHabitRepository }
                    single { mockNotificationBuilder }
                }
            )
        }
    }

    @Test
    fun `doWork calls DetectLapsesUseCase and returns success`() = runTest {
        setupKoin()
        coEvery { mockDetectLapses.invoke() } returns DomainResult.Success(emptyList())

        val worker = LapseDetectionWorker(mockContext, mockParams)
        val result = worker.doWork()

        coVerify(exactly = 1) { mockDetectLapses.invoke() }
        assertEquals(ListenableWorker.Result.success(), result)
        stopKoin()
    }

    @Test
    fun `doWork posts lapse notification for LAPSED habit`() = runTest {
        setupKoin()
        coEvery { mockDetectLapses.invoke() } returns DomainResult.Success(listOf(habitId))
        coEvery { mockHabitRepository.getById(habitId) } returns DomainResult.Success(lapsedHabit)

        val worker = LapseDetectionWorker(mockContext, mockParams)
        val result = worker.doWork()

        verify(exactly = 1) {
            mockNotificationBuilder.postLapseNotification(
                habitId = habitId.toString(),
                habitName = "Take medication",
                missedDays = 3
            )
        }
        assertEquals(ListenableWorker.Result.success(), result)
        stopKoin()
    }

    @Test
    fun `doWork posts relapse notification for RELAPSED habit`() = runTest {
        val relapsedHabit = lapsedHabit.copy(phase = HabitPhase.RELAPSED)
        setupKoin()
        coEvery { mockDetectLapses.invoke() } returns DomainResult.Success(listOf(habitId))
        coEvery { mockHabitRepository.getById(habitId) } returns DomainResult.Success(relapsedHabit)

        val worker = LapseDetectionWorker(mockContext, mockParams)
        val result = worker.doWork()

        verify(exactly = 1) {
            mockNotificationBuilder.postRelapseNotification(
                habitId = habitId.toString(),
                habitName = "Take medication"
            )
        }
        assertEquals(ListenableWorker.Result.success(), result)
        stopKoin()
    }

    @Test
    fun `doWork returns retry on error with retries remaining`() = runTest {
        setupKoin()
        coEvery { mockDetectLapses.invoke() } returns DomainResult.Error("db error")

        val worker = LapseDetectionWorker(mockContext, mockParams)
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.retry(), result)
        stopKoin()
    }
}
