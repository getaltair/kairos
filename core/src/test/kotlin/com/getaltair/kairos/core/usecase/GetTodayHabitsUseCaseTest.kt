package com.getaltair.kairos.core.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Completion
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.enums.AnchorType
import com.getaltair.kairos.domain.enums.CompletionType
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.HabitFrequency
import com.getaltair.kairos.domain.model.HabitWithStatus
import com.getaltair.kairos.domain.repository.CompletionRepository
import com.getaltair.kairos.domain.repository.HabitRepository
import io.mockk.coEvery
import io.mockk.mockk
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetTodayHabitsUseCaseTest {

    private lateinit var habitRepository: HabitRepository
    private lateinit var completionRepository: CompletionRepository
    private lateinit var useCase: GetTodayHabitsUseCase

    private val habitId = UUID.randomUUID()

    private fun validHabit(
        id: UUID = habitId,
        name: String = "Meditate",
        category: HabitCategory = HabitCategory.Morning,
        frequency: HabitFrequency = HabitFrequency.Daily,
        activeDays: Set<DayOfWeek>? = null
    ) = Habit(
        id = id,
        name = name,
        anchorBehavior = "After brushing teeth",
        anchorType = AnchorType.AfterBehavior,
        category = category,
        frequency = frequency,
        activeDays = activeDays,
        createdAt = Instant.parse("2025-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2025-01-01T00:00:00Z")
    )

    @Before
    fun setup() {
        habitRepository = mockk()
        completionRepository = mockk()
        useCase = GetTodayHabitsUseCase(habitRepository, completionRepository)
    }

    @Test
    fun `returns habits with completion status`() = runTest {
        val habit = validHabit()
        val completion = Completion(
            habitId = habitId,
            date = LocalDate.now(),
            type = CompletionType.Full
        )

        coEvery { habitRepository.getActiveHabits() } returns Result.Success(listOf(habit))
        coEvery { completionRepository.getForDate(any()) } returns Result.Success(listOf(completion))
        coEvery {
            completionRepository.getForHabitInDateRange(habitId, any(), any())
        } returns Result.Success(listOf(completion))

        val result = useCase()

        assertTrue(result is Result.Success)
        val habits = (result as Result.Success).value
        assertEquals(1, habits.size)
        assertEquals(habitId, habits[0].habit.id)
        assertEquals(completion, habits[0].todayCompletion)
    }

    @Test
    fun `Departure category habits filtered out`() = runTest {
        val departureHabit = validHabit(
            id = UUID.randomUUID(),
            name = "Leave house",
            category = HabitCategory.Departure
        )
        val morningHabit = validHabit(
            id = UUID.randomUUID(),
            name = "Meditate",
            category = HabitCategory.Morning
        )

        coEvery {
            habitRepository.getActiveHabits()
        } returns Result.Success(listOf(departureHabit, morningHabit))
        coEvery { completionRepository.getForDate(any()) } returns Result.Success(emptyList())
        coEvery {
            completionRepository.getForHabitInDateRange(any(), any(), any())
        } returns Result.Success(emptyList())

        val result = useCase()

        assertTrue(result is Result.Success)
        val habits = (result as Result.Success).value
        assertEquals(1, habits.size)
        assertEquals("Meditate", habits[0].habit.name)
    }

    @Test
    fun `habits not due today filtered out`() = runTest {
        // Create a weekday-only habit with active days that exclude today's day of week
        val today = LocalDate.now().dayOfWeek
        // Pick a day that is NOT today
        val notTodayDays = DayOfWeek.entries.filter { it != today }.toSet()
        // Use Custom frequency with only days that are not today
        val notDueTodayHabit = validHabit(
            id = UUID.randomUUID(),
            name = "Not due today",
            frequency = HabitFrequency.Custom,
            activeDays = setOf(notTodayDays.first())
        )
        val dailyHabit = validHabit(
            id = UUID.randomUUID(),
            name = "Daily habit",
            frequency = HabitFrequency.Daily
        )

        coEvery {
            habitRepository.getActiveHabits()
        } returns Result.Success(listOf(notDueTodayHabit, dailyHabit))
        coEvery { completionRepository.getForDate(any()) } returns Result.Success(emptyList())
        coEvery {
            completionRepository.getForHabitInDateRange(any(), any(), any())
        } returns Result.Success(emptyList())

        val result = useCase()

        assertTrue(result is Result.Success)
        val habits = (result as Result.Success).value
        assertEquals(1, habits.size)
        assertEquals("Daily habit", habits[0].habit.name)
    }

    @Test
    fun `weekly rate uses actual due days not hardcoded 7`() = runTest {
        // A weekday-only habit has 5 due days in a 7-day window (Mon-Fri),
        // not 7. With 3 completions in the range, rate should be 3/5 = 0.6.
        // But the exact due days depend on which 7-day window we are in.
        // Use a Custom frequency habit with exactly 2 active days for predictability.
        val today = LocalDate.now()
        val todayDow = today.dayOfWeek
        val anotherDay = DayOfWeek.entries.first { it != todayDow }

        val twoActiveDays = setOf(todayDow, anotherDay)
        val id = UUID.randomUUID()
        val habit = validHabit(
            id = id,
            name = "Two-day habit",
            frequency = HabitFrequency.Custom,
            activeDays = twoActiveDays
        )

        // The use case computes weekAgo = today.minusDays(6)
        // With 2 active days per week, due count should be exactly 2 in 7 days
        // (today and anotherDay each appear once in a 7-day span).
        // With 1 completion in that range, rate = 1/2 = 0.5
        val completion = Completion(
            habitId = id,
            date = today,
            type = CompletionType.Full
        )

        coEvery { habitRepository.getActiveHabits() } returns Result.Success(listOf(habit))
        coEvery { completionRepository.getForDate(any()) } returns Result.Success(listOf(completion))
        coEvery {
            completionRepository.getForHabitInDateRange(id, any(), any())
        } returns Result.Success(listOf(completion))

        val result = useCase()

        assertTrue(result is Result.Success)
        val habits = (result as Result.Success).value
        assertEquals(1, habits.size)
        // The rate should NOT be 1/7 (0.14) which a hardcoded /7 would give.
        // It should be 1/dueCount. dueCount is 2 for two active days in a 7-day window.
        val rate = habits[0].weekCompletionRate
        assertTrue(
            "Weekly rate should reflect actual due days, got $rate",
            rate > 0.14f
        )
    }

    @Test
    fun `weekly rate clamped to 0 to 1 range`() = runTest {
        // If completions exceed due days, rate should be clamped to 1.0
        val id = UUID.randomUUID()
        val today = LocalDate.now()
        val todayDow = today.dayOfWeek
        val habit = validHabit(
            id = id,
            name = "Clamped habit",
            frequency = HabitFrequency.Custom,
            activeDays = setOf(todayDow)
        )

        // This habit is due on 1 day per week. Return 3 completions to force rate > 1.0.
        val completions = (1..3).map {
            Completion(
                habitId = id,
                date = today.minusDays(it.toLong()),
                type = CompletionType.Full
            )
        }
        val todayCompletion = Completion(
            habitId = id,
            date = today,
            type = CompletionType.Full
        )

        coEvery { habitRepository.getActiveHabits() } returns Result.Success(listOf(habit))
        coEvery { completionRepository.getForDate(any()) } returns Result.Success(listOf(todayCompletion))
        coEvery {
            completionRepository.getForHabitInDateRange(id, any(), any())
        } returns Result.Success(completions + todayCompletion)

        val result = useCase()

        assertTrue(result is Result.Success)
        val habits = (result as Result.Success).value
        assertEquals(1, habits.size)
        assertTrue(
            "Weekly rate should be clamped to 1.0, got ${habits[0].weekCompletionRate}",
            habits[0].weekCompletionRate <= 1.0f
        )
    }

    @Test
    fun `empty habits list returns empty result`() = runTest {
        coEvery { habitRepository.getActiveHabits() } returns Result.Success(emptyList())
        coEvery { completionRepository.getForDate(any()) } returns Result.Success(emptyList())

        val result = useCase()

        assertTrue(result is Result.Success)
        val habits = (result as Result.Success).value
        assertTrue(habits.isEmpty())
    }

    @Test
    fun `repository error propagated as Result Error`() = runTest {
        coEvery {
            habitRepository.getActiveHabits()
        } returns Result.Error("Database unavailable")

        val result = useCase()

        assertTrue(result is Result.Error)
        assertEquals("Database unavailable", (result as Result.Error).message)
    }

    @Test
    fun `computeWeeklyRate error falls back to 0f`() = runTest {
        val habit = validHabit()
        coEvery { habitRepository.getActiveHabits() } returns Result.Success(listOf(habit))
        coEvery { completionRepository.getForDate(any()) } returns Result.Success(emptyList())
        coEvery {
            completionRepository.getForHabitInDateRange(habitId, any(), any())
        } returns Result.Error("Range query failed")

        val result = useCase()

        assertTrue(result is Result.Success)
        val habits = (result as Result.Success).value
        assertEquals(1, habits.size)
        assertEquals(0f, habits[0].weekCompletionRate)
    }
}
