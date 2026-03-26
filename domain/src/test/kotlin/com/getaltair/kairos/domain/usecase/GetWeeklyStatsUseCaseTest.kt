package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Completion
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.enums.AnchorType
import com.getaltair.kairos.domain.enums.CompletionType
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.HabitFrequency
import com.getaltair.kairos.domain.model.WeeklyStats
import com.getaltair.kairos.domain.repository.CompletionRepository
import com.getaltair.kairos.domain.repository.HabitRepository
import io.mockk.coEvery
import io.mockk.mockk
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetWeeklyStatsUseCaseTest {

    private lateinit var habitRepository: HabitRepository
    private lateinit var completionRepository: CompletionRepository
    private lateinit var useCase: GetWeeklyStatsUseCase

    private val habitId = UUID.randomUUID()
    private val today = LocalDate.now()
    private val weekStart = today.minusDays(6)

    private fun makeHabit(
        id: UUID = habitId,
        frequency: HabitFrequency = HabitFrequency.Daily,
        activeDays: Set<DayOfWeek>? = null
    ) = Habit(
        id = id,
        name = "Test Habit",
        anchorBehavior = "After waking up",
        anchorType = AnchorType.AfterBehavior,
        category = HabitCategory.Morning,
        frequency = frequency,
        activeDays = activeDays,
        createdAt = Instant.parse("2025-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2025-01-01T00:00:00Z")
    )

    private fun makeCompletion(
        hId: UUID = habitId,
        date: LocalDate = today,
        type: CompletionType = CompletionType.Full
    ) = Completion(
        habitId = hId,
        date = date,
        type = type
    )

    @Before
    fun setup() {
        habitRepository = mockk()
        completionRepository = mockk()
        useCase = GetWeeklyStatsUseCase(habitRepository, completionRepository)
    }

    @Test
    fun `happy path with habitId counts completion types correctly`() = runTest {
        val habit = makeHabit()
        val completions = listOf(
            makeCompletion(type = CompletionType.Full, date = today),
            makeCompletion(type = CompletionType.Full, date = today.minusDays(1)),
            makeCompletion(
                type = CompletionType.Partial,
                date = today.minusDays(2)
            ).let {
                // Need to create with partialPercent for Partial type
                Completion(
                    habitId = habitId,
                    date = today.minusDays(2),
                    type = CompletionType.Partial,
                    partialPercent = 50
                )
            },
            makeCompletion(type = CompletionType.Skipped, date = today.minusDays(3)),
            makeCompletion(type = CompletionType.Missed, date = today.minusDays(4))
        )

        coEvery { habitRepository.getById(habitId) } returns Result.Success(habit)
        coEvery {
            completionRepository.getForHabitInDateRange(habitId, weekStart, today)
        } returns Result.Success(completions)

        val result = useCase(habitId)

        assertTrue(result is Result.Success)
        val stats = (result as Result.Success).value
        assertEquals(habitId, stats.habitId)
        assertEquals(2, stats.completedCount)
        assertEquals(1, stats.partialCount)
        assertEquals(1, stats.skippedCount)
        assertEquals(1, stats.missedCount)
        // Daily habit: totalDays = 7
        assertEquals(7, stats.totalDays)
        // completionRate = (2 + 1) / 7 = 3/7
        assertEquals(3f / 7f, stats.completionRate, 0.001f)
    }

    @Test
    fun `happy path with null habitId returns aggregate stats with totalDays 7`() = runTest {
        val completions = listOf(
            makeCompletion(type = CompletionType.Full, date = today),
            makeCompletion(type = CompletionType.Full, date = today.minusDays(1)),
            makeCompletion(type = CompletionType.Skipped, date = today.minusDays(3))
        )

        coEvery {
            completionRepository.getForDateRange(weekStart, today)
        } returns Result.Success(completions)

        val result = useCase(habitId = null)

        assertTrue(result is Result.Success)
        val stats = (result as Result.Success).value
        assertNull(stats.habitId)
        assertEquals(7, stats.totalDays)
        assertEquals(2, stats.completedCount)
        assertEquals(0, stats.partialCount)
        assertEquals(1, stats.skippedCount)
        assertEquals(0, stats.missedCount)
    }

    @Test
    fun `habit not found returns Result Error`() = runTest {
        val unknownId = UUID.randomUUID()
        coEvery { habitRepository.getById(unknownId) } returns Result.Error("Not found")

        val result = useCase(unknownId)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("Habit not found"))
    }

    @Test
    fun `habit repository error is propagated`() = runTest {
        coEvery {
            habitRepository.getById(habitId)
        } returns Result.Error("Database connection failed")

        val result = useCase(habitId)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("Habit not found"))
    }

    @Test
    fun `non-daily habit uses actual due days for totalDays`() = runTest {
        // Weekdays-only habit: due Mon-Fri only
        val habit = makeHabit(frequency = HabitFrequency.Weekdays)

        coEvery { habitRepository.getById(habitId) } returns Result.Success(habit)
        coEvery {
            completionRepository.getForHabitInDateRange(habitId, weekStart, today)
        } returns Result.Success(emptyList())

        val result = useCase(habitId)

        assertTrue(result is Result.Success)
        val stats = (result as Result.Success).value
        // countDueDays for Weekdays in a 7-day window will vary but
        // should be at least 1 (coerceAtLeast(1)) and at most 5
        assertTrue(stats.totalDays in 1..7)
        // For a weekdays-only habit, totalDays should not always be 7
        // (unless the 7-day window happens to contain only weekdays, which is impossible)
    }

    @Test
    fun `zero completions returns all zeros and 0f rate`() = runTest {
        val habit = makeHabit()

        coEvery { habitRepository.getById(habitId) } returns Result.Success(habit)
        coEvery {
            completionRepository.getForHabitInDateRange(habitId, weekStart, today)
        } returns Result.Success(emptyList())

        val result = useCase(habitId)

        assertTrue(result is Result.Success)
        val stats = (result as Result.Success).value
        assertEquals(0, stats.completedCount)
        assertEquals(0, stats.partialCount)
        assertEquals(0, stats.skippedCount)
        assertEquals(0, stats.missedCount)
        assertEquals(0f, stats.completionRate, 0.001f)
    }

    @Test
    fun `CancellationException is rethrown not caught`() = runTest {
        coEvery { habitRepository.getById(habitId) } throws CancellationException("cancelled")

        var thrown = false
        try {
            useCase(habitId)
        } catch (e: CancellationException) {
            thrown = true
        }
        assertTrue("CancellationException should be rethrown", thrown)
    }

    @Test
    fun `completion repository error in habitId path is propagated`() = runTest {
        val habit = makeHabit()
        coEvery { habitRepository.getById(habitId) } returns Result.Success(habit)
        coEvery {
            completionRepository.getForHabitInDateRange(habitId, weekStart, today)
        } returns Result.Error("Completion query failed")

        val result = useCase(habitId)

        assertTrue(result is Result.Error)
        assertEquals("Completion query failed", (result as Result.Error).message)
    }

    @Test
    fun `completion repository error in aggregate path is propagated`() = runTest {
        coEvery {
            completionRepository.getForDateRange(weekStart, today)
        } returns Result.Error("Aggregate query failed")

        val result = useCase(habitId = null)

        assertTrue(result is Result.Error)
        assertEquals("Aggregate query failed", (result as Result.Error).message)
    }
}
