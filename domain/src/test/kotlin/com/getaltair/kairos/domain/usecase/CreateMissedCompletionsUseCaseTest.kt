package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Completion
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.enums.AnchorType
import com.getaltair.kairos.domain.enums.CompletionType
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.HabitFrequency
import com.getaltair.kairos.domain.repository.CompletionRepository
import com.getaltair.kairos.domain.repository.HabitRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CreateMissedCompletionsUseCaseTest {

    private lateinit var habitRepository: HabitRepository
    private lateinit var completionRepository: CompletionRepository
    private lateinit var useCase: CreateMissedCompletionsUseCase

    private val yesterday = LocalDate.now().minusDays(1)

    private fun dailyHabit(name: String = "Meditate") = Habit(
        name = name,
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
        useCase = CreateMissedCompletionsUseCase(habitRepository, completionRepository)
    }

    @Test
    fun `creates MISSED completions for habits with no completion on date`() = runTest {
        val habit1 = dailyHabit("Meditate")
        val habit2 = dailyHabit("Exercise")
        coEvery { habitRepository.getActiveHabits() } returns Result.Success(listOf(habit1, habit2))
        coEvery { completionRepository.getForHabitOnDate(habit1.id, yesterday) } returns Result.Success(null)
        coEvery { completionRepository.getForHabitOnDate(habit2.id, yesterday) } returns Result.Success(null)

        val completionSlot = slot<Completion>()
        coEvery { completionRepository.insert(capture(completionSlot)) } answers {
            Result.Success(completionSlot.captured)
        }

        val result = useCase(yesterday)

        assertTrue(result is Result.Success)
        assertEquals(2, (result as Result.Success).value)
        coVerify(exactly = 2) { completionRepository.insert(any()) }
    }

    @Test
    fun `skips habits that already have a completion`() = runTest {
        val habit = dailyHabit()
        val existing = Completion(
            habitId = habit.id,
            date = yesterday,
            type = CompletionType.Full
        )
        coEvery { habitRepository.getActiveHabits() } returns Result.Success(listOf(habit))
        coEvery { completionRepository.getForHabitOnDate(habit.id, yesterday) } returns Result.Success(existing)

        val result = useCase(yesterday)

        assertTrue(result is Result.Success)
        assertEquals(0, (result as Result.Success).value)
        coVerify(exactly = 0) { completionRepository.insert(any()) }
    }

    @Test
    fun `skips habits not due on the given date`() = runTest {
        val weekendHabit = Habit(
            name = "Weekend Jog",
            anchorBehavior = "After waking up",
            anchorType = AnchorType.AfterBehavior,
            category = HabitCategory.Morning,
            frequency = HabitFrequency.Weekends,
            createdAt = Instant.parse("2025-01-01T00:00:00Z"),
            updatedAt = Instant.parse("2025-01-01T00:00:00Z")
        )
        coEvery { habitRepository.getActiveHabits() } returns Result.Success(listOf(weekendHabit))

        // Use a known weekday (Monday 2026-03-23)
        val monday = LocalDate.of(2026, 3, 23)
        val result = useCase(monday)

        assertTrue(result is Result.Success)
        assertEquals(0, (result as Result.Success).value)
        coVerify(exactly = 0) { completionRepository.insert(any()) }
    }

    @Test
    fun `returns error when habit repository fails`() = runTest {
        coEvery { habitRepository.getActiveHabits() } returns Result.Error("DB error")

        val result = useCase(yesterday)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("DB error"))
    }

    @Test
    fun `defaults to yesterday when no date provided`() = runTest {
        coEvery { habitRepository.getActiveHabits() } returns Result.Success(emptyList())

        val result = useCase()

        assertTrue(result is Result.Success)
        assertEquals(0, (result as Result.Success).value)
    }

    @Test
    fun `logs error and continues when completion check fails for one habit`() = runTest {
        val habit1 = dailyHabit("Meditate")
        val habit2 = dailyHabit("Exercise")
        coEvery { habitRepository.getActiveHabits() } returns Result.Success(listOf(habit1, habit2))

        // habit1 check fails, habit2 succeeds with no existing completion
        coEvery { completionRepository.getForHabitOnDate(habit1.id, yesterday) } returns Result.Error("DB error")
        coEvery { completionRepository.getForHabitOnDate(habit2.id, yesterday) } returns Result.Success(null)

        val completionSlot = slot<Completion>()
        coEvery { completionRepository.insert(capture(completionSlot)) } answers {
            Result.Success(completionSlot.captured)
        }

        val result = useCase(yesterday)

        assertTrue(result is Result.Success)
        // Only habit2 should be counted -- habit1 errored during check
        assertEquals(1, (result as Result.Success).value)
        coVerify(exactly = 1) { completionRepository.insert(any()) }
    }

    @Test
    fun `logs error and continues when insert fails for one habit`() = runTest {
        val habit1 = dailyHabit("Meditate")
        val habit2 = dailyHabit("Exercise")
        coEvery { habitRepository.getActiveHabits() } returns Result.Success(listOf(habit1, habit2))
        coEvery { completionRepository.getForHabitOnDate(habit1.id, yesterday) } returns Result.Success(null)
        coEvery { completionRepository.getForHabitOnDate(habit2.id, yesterday) } returns Result.Success(null)

        // Insert fails for habit1, succeeds for habit2
        coEvery { completionRepository.insert(match { it.habitId == habit1.id }) } returns Result.Error("Insert failed")
        val completionSlot = slot<Completion>()
        coEvery { completionRepository.insert(match { it.habitId == habit2.id }) } answers {
            completionSlot.captured = it.invocation.args[0] as Completion
            Result.Success(completionSlot.captured)
        }

        val result = useCase(yesterday)

        assertTrue(result is Result.Success)
        // Only habit2 should be counted -- habit1 insert failed
        assertEquals(1, (result as Result.Success).value)
    }
}
