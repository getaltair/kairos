package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.enums.AnchorType
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.HabitFrequency
import com.getaltair.kairos.domain.repository.HabitRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EditHabitUseCaseTest {

    private lateinit var habitRepository: HabitRepository
    private lateinit var useCase: EditHabitUseCase

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
        useCase = EditHabitUseCase(habitRepository)
    }

    @Test
    fun `successful edit returns updated habit`() = runTest {
        val habit = validHabit()
        val updated = habit.copy(name = "Meditate longer")
        coEvery { habitRepository.getById(habit.id) } returns Result.Success(habit)
        coEvery { habitRepository.update(any()) } returns Result.Success(updated)

        val result = useCase(updated)

        assertTrue(result is Result.Success)
        assertEquals("Meditate longer", (result as Result.Success).value.name)
        coVerify(exactly = 1) { habitRepository.update(any()) }
    }

    @Test
    fun `edit with blank anchor returns validation error`() = runTest {
        val habit = validHabit()
        val invalid = Habit(
            id = habit.id,
            name = habit.name,
            anchorBehavior = "   ",
            anchorType = habit.anchorType,
            category = habit.category,
            frequency = habit.frequency,
            createdAt = habit.createdAt,
            updatedAt = Instant.now()
        )
        coEvery { habitRepository.getById(habit.id) } returns Result.Success(habit)

        val result = useCase(invalid)

        assertTrue(result is Result.Error)
        assertEquals("anchorBehavior must not be blank", (result as Result.Error).message)
        coVerify(exactly = 0) { habitRepository.update(any()) }
    }

    @Test
    fun `edit with non-existent habit returns error`() = runTest {
        val habit = validHabit()
        coEvery { habitRepository.getById(habit.id) } returns Result.Error("Not found")

        val result = useCase(habit)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("Habit not found"))
        coVerify(exactly = 0) { habitRepository.update(any()) }
    }

    @Test
    fun `repository exception is caught and wrapped`() = runTest {
        val habit = validHabit()
        coEvery { habitRepository.getById(habit.id) } returns Result.Success(habit)
        coEvery { habitRepository.update(any()) } throws RuntimeException("DB error")

        val result = useCase(habit)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("DB error"))
    }
}
