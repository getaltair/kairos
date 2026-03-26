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
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CompleteHabitUseCaseTest {

    private lateinit var habitRepository: HabitRepository
    private lateinit var completionRepository: CompletionRepository
    private lateinit var useCase: CompleteHabitUseCase

    private val habitId = UUID.randomUUID()

    private fun validHabit() = Habit(
        id = habitId,
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
        useCase = CompleteHabitUseCase(habitRepository, completionRepository)
    }

    @Test
    fun `full completion creates successfully`() = runTest {
        val habit = validHabit()
        coEvery { habitRepository.getById(habitId) } returns Result.Success(habit)
        coEvery {
            completionRepository.getForHabitOnDate(habitId, any())
        } returns Result.Success(null)

        val completionSlot = slot<Completion>()
        coEvery { completionRepository.insert(capture(completionSlot)) } answers {
            Result.Success(completionSlot.captured)
        }

        val result = useCase(habitId, CompletionType.Full)

        assertTrue(result is Result.Success)
        val completion = (result as Result.Success).value
        assertEquals(habitId, completion.habitId)
        assertTrue(completion.type is CompletionType.Full)
        coVerify(exactly = 1) { completionRepository.insert(any()) }
    }

    @Test
    fun `partial completion with valid percent creates successfully`() = runTest {
        val habit = validHabit()
        coEvery { habitRepository.getById(habitId) } returns Result.Success(habit)
        coEvery {
            completionRepository.getForHabitOnDate(habitId, any())
        } returns Result.Success(null)

        val completionSlot = slot<Completion>()
        coEvery { completionRepository.insert(capture(completionSlot)) } answers {
            Result.Success(completionSlot.captured)
        }

        val result = useCase(habitId, CompletionType.Partial, partialPercent = 50)

        assertTrue(result is Result.Success)
        val completion = (result as Result.Success).value
        assertEquals(habitId, completion.habitId)
        assertTrue(completion.type is CompletionType.Partial)
        assertEquals(50, completion.partialPercent)
        coVerify(exactly = 1) { completionRepository.insert(any()) }
    }

    @Test
    fun `partial completion without percent fails validation`() = runTest {
        val habit = validHabit()
        coEvery { habitRepository.getById(habitId) } returns Result.Success(habit)

        val result = useCase(habitId, CompletionType.Partial, partialPercent = null)

        assertTrue(result is Result.Error)
    }

    @Test
    fun `C-3 duplicate completion for same day returns error`() = runTest {
        val habit = validHabit()
        val existingCompletion = Completion(
            habitId = habitId,
            date = LocalDate.now(),
            type = CompletionType.Full
        )

        coEvery { habitRepository.getById(habitId) } returns Result.Success(habit)
        coEvery {
            completionRepository.getForHabitOnDate(habitId, any())
        } returns Result.Success(existingCompletion)

        val result = useCase(habitId, CompletionType.Full)

        assertTrue(result is Result.Error)
        assertEquals(
            "A completion already exists for this habit today",
            (result as Result.Error).message
        )
        coVerify(exactly = 0) { completionRepository.insert(any()) }
    }

    @Test
    fun `C-1 MISSED type rejected`() = runTest {
        val habit = validHabit()
        coEvery { habitRepository.getById(habitId) } returns Result.Success(habit)

        val result = useCase(habitId, CompletionType.Missed)

        assertTrue(result is Result.Error)
        assertEquals(
            "Cannot manually create MISSED completions",
            (result as Result.Error).message
        )
        coVerify(exactly = 0) { completionRepository.insert(any()) }
    }

    @Test
    fun `non-existent habit returns error`() = runTest {
        val unknownId = UUID.randomUUID()
        coEvery { habitRepository.getById(unknownId) } returns Result.Error("Not found")

        val result = useCase(unknownId, CompletionType.Full)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("Habit not found"))
        coVerify(exactly = 0) { completionRepository.insert(any()) }
    }
}
