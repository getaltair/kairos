package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.enums.AnchorType
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.HabitFrequency
import com.getaltair.kairos.domain.repository.HabitRepository
import io.mockk.coEvery
import io.mockk.mockk
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetHabitUseCaseTest {

    private lateinit var habitRepository: HabitRepository
    private lateinit var useCase: GetHabitUseCase

    private fun validHabit() = Habit(
        name = "Meditate",
        anchorBehavior = "After brushing teeth",
        anchorType = AnchorType.AfterBehavior,
        category = HabitCategory.Morning,
        frequency = HabitFrequency.Daily,
        createdAt = Instant.parse("2025-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2025-01-01T00:00:00Z")
    )

    @Before
    fun setup() {
        habitRepository = mockk()
        useCase = GetHabitUseCase(habitRepository)
    }

    @Test
    fun `returns habit on success`() = runTest {
        val habit = validHabit()
        coEvery { habitRepository.getById(habit.id) } returns Result.Success(habit)

        val result = useCase(habit.id)

        assertTrue(result is Result.Success)
        assertEquals(habit, (result as Result.Success).value)
    }

    @Test
    fun `returns error when habit not found`() = runTest {
        val id = UUID.randomUUID()
        coEvery { habitRepository.getById(id) } returns Result.Error("Not found")

        val result = useCase(id)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("Habit not found:"))
    }

    @Test
    fun `repository exception is caught and wrapped`() = runTest {
        val id = UUID.randomUUID()
        coEvery { habitRepository.getById(id) } throws RuntimeException("DB error")

        val result = useCase(id)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("Failed to get habit:"))
        assertTrue(result.message.contains("DB error"))
    }

    @Test(expected = CancellationException::class)
    fun `CancellationException is rethrown`() = runTest {
        val id = UUID.randomUUID()
        coEvery { habitRepository.getById(id) } throws CancellationException("cancelled")

        useCase(id)
    }
}
