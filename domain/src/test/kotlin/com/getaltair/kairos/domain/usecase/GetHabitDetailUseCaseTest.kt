package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Completion
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.enums.AnchorType
import com.getaltair.kairos.domain.enums.CompletionType
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.HabitFrequency
import com.getaltair.kairos.domain.model.HabitDetail
import com.getaltair.kairos.domain.repository.CompletionRepository
import com.getaltair.kairos.domain.repository.HabitRepository
import io.mockk.coEvery
import io.mockk.mockk
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetHabitDetailUseCaseTest {

    private lateinit var habitRepository: HabitRepository
    private lateinit var completionRepository: CompletionRepository
    private lateinit var useCase: GetHabitDetailUseCase

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
        completionRepository = mockk()
        useCase = GetHabitDetailUseCase(habitRepository, completionRepository)
    }

    @Test
    fun `returns habit detail with completions and rate`() = runTest {
        val habit = validHabit()
        val today = LocalDate.now()
        val completions = listOf(
            Completion(
                habitId = habit.id,
                date = today,
                type = CompletionType.Full
            ),
            Completion(
                habitId = habit.id,
                date = today.minusDays(1),
                type = CompletionType.Full
            ),
            Completion(
                habitId = habit.id,
                date = today.minusDays(2),
                type = CompletionType.Partial,
                partialPercent = 50
            ),
            Completion(
                habitId = habit.id,
                date = today.minusDays(10),
                type = CompletionType.Full
            )
        )
        coEvery { habitRepository.getById(habit.id) } returns Result.Success(habit)
        coEvery {
            completionRepository.getForHabitInDateRange(habit.id, any(), any())
        } returns Result.Success(completions)

        val result = useCase(habit.id)

        assertTrue(result is Result.Success)
        val detail = (result as Result.Success).value
        assertEquals(habit, detail.habit)
        assertEquals(4, detail.recentCompletions.size)
        // 3 completions in last 7 days (Full, Full, Partial) / 7 = ~0.4286
        assertEquals(3f / 7f, detail.weeklyCompletionRate, 0.001f)
    }

    @Test
    fun `returns zero rate when no completions`() = runTest {
        val habit = validHabit()
        coEvery { habitRepository.getById(habit.id) } returns Result.Success(habit)
        coEvery {
            completionRepository.getForHabitInDateRange(habit.id, any(), any())
        } returns Result.Success(emptyList())

        val result = useCase(habit.id)

        assertTrue(result is Result.Success)
        val detail = (result as Result.Success).value
        assertEquals(0f, detail.weeklyCompletionRate, 0.001f)
        assertTrue(detail.recentCompletions.isEmpty())
    }

    @Test
    fun `habit not found returns error`() = runTest {
        val habit = validHabit()
        coEvery { habitRepository.getById(habit.id) } returns Result.Error("Not found")

        val result = useCase(habit.id)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("Habit not found"))
    }

    @Test
    fun `repository exception is caught and wrapped`() = runTest {
        val habit = validHabit()
        coEvery { habitRepository.getById(habit.id) } throws RuntimeException("DB error")

        val result = useCase(habit.id)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("DB error"))
    }
}
