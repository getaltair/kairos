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

class CreateHabitUseCaseTest {

    private lateinit var habitRepository: HabitRepository
    private lateinit var useCase: CreateHabitUseCase

    private fun validHabit() = Habit(
        name = "Meditate",
        anchorBehavior = "After brushing teeth",
        anchorType = AnchorType.AfterBehavior,
        category = HabitCategory.Morning,
        frequency = HabitFrequency.Daily,
        allowPartialCompletion = true,
        lapseThresholdDays = 3,
        relapseThresholdDays = 7,
        createdAt = Instant.parse("2025-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2025-01-01T00:00:00Z")
    )

    @Before
    fun setup() {
        habitRepository = mockk()
        useCase = CreateHabitUseCase(habitRepository)
    }

    @Test
    fun `valid habit is created successfully`() = runTest {
        val habit = validHabit()
        coEvery { habitRepository.insert(habit) } returns Result.Success(habit)

        val result = useCase(habit)

        assertTrue(result is Result.Success)
        assertEquals(habit, (result as Result.Success).value)
        coVerify(exactly = 1) { habitRepository.insert(habit) }
    }

    @Test
    fun `invalid habit with blank anchor returns validation error`() = runTest {
        val habit = validHabit().let {
            Habit(
                id = it.id,
                name = it.name,
                anchorBehavior = "   ",
                anchorType = it.anchorType,
                category = it.category,
                frequency = it.frequency,
                allowPartialCompletion = it.allowPartialCompletion,
                lapseThresholdDays = it.lapseThresholdDays,
                relapseThresholdDays = it.relapseThresholdDays,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        }

        val result = useCase(habit)

        assertTrue(result is Result.Error)
        assertEquals("anchorBehavior must not be blank", (result as Result.Error).message)
        coVerify(exactly = 0) { habitRepository.insert(any()) }
    }

    @Test
    fun `repository error is propagated`() = runTest {
        val habit = validHabit()
        coEvery { habitRepository.insert(habit) } returns Result.Error("Database error")

        val result = useCase(habit)

        assertTrue(result is Result.Error)
        assertEquals("Database error", (result as Result.Error).message)
    }

    @Test
    fun `repository exception is caught and wrapped`() = runTest {
        val habit = validHabit()
        coEvery { habitRepository.insert(habit) } throws RuntimeException("Connection lost")

        val result = useCase(habit)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("Connection lost"))
    }
}
