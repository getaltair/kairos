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
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeleteHabitUseCaseTest {

    private lateinit var habitRepository: HabitRepository
    private lateinit var useCase: DeleteHabitUseCase

    private fun archivedHabit() = Habit(
        name = "Meditate",
        anchorBehavior = "After brushing teeth",
        anchorType = AnchorType.AfterBehavior,
        category = HabitCategory.Morning,
        frequency = HabitFrequency.Daily,
        status = HabitStatus.Archived,
        archivedAt = Instant.parse("2025-02-01T00:00:00Z"),
        createdAt = Instant.parse("2025-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2025-02-01T00:00:00Z")
    )

    @Before
    fun setup() {
        habitRepository = mockk()
        useCase = DeleteHabitUseCase(habitRepository)
    }

    @Test
    fun `delete archived habit succeeds`() = runTest {
        val habit = archivedHabit()
        coEvery { habitRepository.getById(habit.id) } returns Result.Success(habit)
        coEvery { habitRepository.delete(habit.id) } returns Result.Success(Unit)

        val result = useCase(habit.id)

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { habitRepository.delete(habit.id) }
    }

    @Test
    fun `delete from Active status is rejected`() = runTest {
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
        assertTrue((result as Result.Error).message.contains("expected Archived"))
        coVerify(exactly = 0) { habitRepository.delete(any()) }
    }

    @Test
    fun `delete from Paused status is rejected`() = runTest {
        val habit = archivedHabit().copy(
            status = HabitStatus.Paused,
            pausedAt = Instant.now(),
            archivedAt = null
        )
        coEvery { habitRepository.getById(habit.id) } returns Result.Success(habit)

        val result = useCase(habit.id)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("expected Archived"))
    }

    @Test
    fun `delete non-existent habit returns error`() = runTest {
        val habit = archivedHabit()
        coEvery { habitRepository.getById(habit.id) } returns Result.Error("Not found")

        val result = useCase(habit.id)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("Habit not found"))
    }
}
