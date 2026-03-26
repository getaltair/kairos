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
import io.mockk.mockk
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetTodayHabitsUseCaseTest {

    private lateinit var habitRepository: HabitRepository
    private lateinit var completionRepository: CompletionRepository
    private lateinit var useCase: GetTodayHabitsUseCase

    private val today = LocalDate.now()
    private val weekStart = today.minusDays(6)

    private fun makeHabit(
        id: UUID = UUID.randomUUID(),
        name: String = "Test Habit",
        frequency: HabitFrequency = HabitFrequency.Daily
    ) = Habit(
        id = id,
        name = name,
        anchorBehavior = "After waking up",
        anchorType = AnchorType.AfterBehavior,
        category = HabitCategory.Morning,
        frequency = frequency,
        createdAt = Instant.parse("2025-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2025-01-01T00:00:00Z")
    )

    private fun makeCompletion(habitId: UUID, date: LocalDate = today, type: CompletionType = CompletionType.Full) =
        Completion(
            habitId = habitId,
            date = date,
            type = type
        )

    @Before
    fun setup() {
        habitRepository = mockk()
        completionRepository = mockk()
        useCase = GetTodayHabitsUseCase(habitRepository, completionRepository)
    }

    @Test
    fun `returns habits with completion status`() = runTest {
        val habitId = UUID.randomUUID()
        val habit = makeHabit(id = habitId, name = "Meditate")
        val todayCompletion = makeCompletion(habitId = habitId)

        coEvery { habitRepository.getHabitsForDate(today) } returns Result.Success(listOf(habit))
        coEvery { completionRepository.getForDate(today) } returns Result.Success(listOf(todayCompletion))
        coEvery {
            completionRepository.getForDateRange(weekStart, today)
        } returns Result.Success(listOf(todayCompletion))

        val result = useCase()

        assertTrue(result is Result.Success)
        val habitsWithStatus = (result as Result.Success).value
        assertEquals(1, habitsWithStatus.size)
        assertEquals(habit, habitsWithStatus[0].habit)
        assertEquals(todayCompletion, habitsWithStatus[0].todayCompletion)
    }

    @Test
    fun `habit without today completion has null todayCompletion`() = runTest {
        val habitId = UUID.randomUUID()
        val habit = makeHabit(id = habitId)

        coEvery { habitRepository.getHabitsForDate(today) } returns Result.Success(listOf(habit))
        coEvery { completionRepository.getForDate(today) } returns Result.Success(emptyList())
        coEvery {
            completionRepository.getForDateRange(weekStart, today)
        } returns Result.Success(emptyList())

        val result = useCase()

        assertTrue(result is Result.Success)
        val habitsWithStatus = (result as Result.Success).value
        assertEquals(1, habitsWithStatus.size)
        assertNull(habitsWithStatus[0].todayCompletion)
    }

    @Test
    fun `calculates week completion rate correctly for daily habit`() = runTest {
        val habitId = UUID.randomUUID()
        val habit = makeHabit(id = habitId, frequency = HabitFrequency.Daily)

        // 3 completions out of 7 days (daily habit, so 7 due days)
        val weekCompletions = listOf(
            makeCompletion(habitId = habitId, date = today),
            makeCompletion(habitId = habitId, date = today.minusDays(1)),
            makeCompletion(habitId = habitId, date = today.minusDays(2))
        )

        coEvery { habitRepository.getHabitsForDate(today) } returns Result.Success(listOf(habit))
        coEvery { completionRepository.getForDate(today) } returns Result.Success(
            listOf(weekCompletions[0])
        )
        coEvery {
            completionRepository.getForDateRange(weekStart, today)
        } returns Result.Success(weekCompletions)

        val result = useCase()

        assertTrue(result is Result.Success)
        val habitsWithStatus = (result as Result.Success).value
        // 3 completions / 7 due days = ~0.4286
        val expectedRate = 3f / 7f
        assertEquals(expectedRate, habitsWithStatus[0].weekCompletionRate, 0.001f)
    }

    @Test
    fun `empty habit list returns empty result`() = runTest {
        coEvery { habitRepository.getHabitsForDate(today) } returns Result.Success(emptyList())
        coEvery { completionRepository.getForDate(today) } returns Result.Success(emptyList())
        coEvery {
            completionRepository.getForDateRange(weekStart, today)
        } returns Result.Success(emptyList())

        val result = useCase()

        assertTrue(result is Result.Success)
        val habitsWithStatus = (result as Result.Success).value
        assertTrue(habitsWithStatus.isEmpty())
    }

    @Test
    fun `repository error for habits is propagated`() = runTest {
        coEvery {
            habitRepository.getHabitsForDate(today)
        } returns Result.Error("Database unavailable")

        val result = useCase()

        assertTrue(result is Result.Error)
    }
}
