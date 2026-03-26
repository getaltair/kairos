package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.enums.AnchorType
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.HabitFrequency
import com.getaltair.kairos.domain.enums.HabitStatus
import com.getaltair.kairos.domain.repository.HabitRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ResumeHabitUseCaseTest {

    private lateinit var habitRepository: HabitRepository
    private lateinit var useCase: ResumeHabitUseCase

    private fun pausedHabit() = Habit(
        name = "Meditate",
        anchorBehavior = "After brushing teeth",
        anchorType = AnchorType.AfterBehavior,
        category = HabitCategory.Morning,
        frequency = HabitFrequency.Daily,
        status = HabitStatus.Paused,
        pausedAt = Instant.parse("2025-01-10T00:00:00Z"),
        createdAt = Instant.parse("2025-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2025-01-10T00:00:00Z")
    )

    @Before
    fun setup() {
        habitRepository = mockk()
        useCase = ResumeHabitUseCase(habitRepository)
    }

    @Test
    fun `resume paused habit succeeds and clears pausedAt`() = runTest {
        val habit = pausedHabit()
        val updateSlot = slot<Habit>()
        coEvery { habitRepository.getById(habit.id) } returns Result.Success(habit)
        coEvery { habitRepository.update(capture(updateSlot)) } answers {
            Result.Success(updateSlot.captured)
        }

        val result = useCase(habit.id)

        assertTrue(result is Result.Success)
        val resumed = (result as Result.Success).value
        assertTrue(resumed.status is HabitStatus.Active)
        assertNull(resumed.pausedAt)
    }

    @Test
    fun `resume from Active status is rejected`() = runTest {
        val habit = Habit(
            name = "Meditate",
            anchorBehavior = "After brushing teeth",
            anchorType = AnchorType.AfterBehavior,
            category = HabitCategory.Morning,
            frequency = HabitFrequency.Daily,
            status = HabitStatus.Active,
            createdAt = Instant.parse("2025-01-01T00:00:00Z"),
            updatedAt = Instant.parse("2025-01-01T00:00:00Z")
        )
        coEvery { habitRepository.getById(habit.id) } returns Result.Success(habit)

        val result = useCase(habit.id)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("expected Paused"))
        coVerify(exactly = 0) { habitRepository.update(any()) }
    }

    @Test
    fun `resume from Archived status is rejected`() = runTest {
        val habit = pausedHabit().copy(
            status = HabitStatus.Archived,
            archivedAt = Instant.now()
        )
        coEvery { habitRepository.getById(habit.id) } returns Result.Success(habit)

        val result = useCase(habit.id)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("expected Paused"))
    }

    @Test
    fun `resume non-existent habit returns error`() = runTest {
        val habit = pausedHabit()
        coEvery { habitRepository.getById(habit.id) } returns Result.Error("Not found")

        val result = useCase(habit.id)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("Habit not found"))
    }
}
