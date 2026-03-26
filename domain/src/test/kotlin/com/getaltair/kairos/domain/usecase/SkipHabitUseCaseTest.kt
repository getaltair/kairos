package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Completion
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.enums.AnchorType
import com.getaltair.kairos.domain.enums.CompletionType
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.HabitFrequency
import com.getaltair.kairos.domain.enums.SkipReason
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

class SkipHabitUseCaseTest {

    private lateinit var habitRepository: HabitRepository
    private lateinit var completionRepository: CompletionRepository
    private lateinit var useCase: SkipHabitUseCase

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
        useCase = SkipHabitUseCase(habitRepository, completionRepository)
    }

    @Test
    fun `skip with reason succeeds`() = runTest {
        val habit = validHabit()
        coEvery { habitRepository.getById(habitId) } returns Result.Success(habit)
        coEvery {
            completionRepository.getForHabitOnDate(habitId, any())
        } returns Result.Success(null)

        val completionSlot = slot<Completion>()
        coEvery { completionRepository.insert(capture(completionSlot)) } answers {
            Result.Success(completionSlot.captured)
        }

        val result = useCase(habitId, SkipReason.TooTired)

        assertTrue(result is Result.Success)
        val completion = (result as Result.Success).value
        assertEquals(habitId, completion.habitId)
        assertTrue(completion.type is CompletionType.Skipped)
        assertEquals(SkipReason.TooTired, completion.skipReason)
        coVerify(exactly = 1) { completionRepository.insert(any()) }
    }

    @Test
    fun `skip without reason succeeds`() = runTest {
        val habit = validHabit()
        coEvery { habitRepository.getById(habitId) } returns Result.Success(habit)
        coEvery {
            completionRepository.getForHabitOnDate(habitId, any())
        } returns Result.Success(null)

        val completionSlot = slot<Completion>()
        coEvery { completionRepository.insert(capture(completionSlot)) } answers {
            Result.Success(completionSlot.captured)
        }

        val result = useCase(habitId, skipReason = null)

        assertTrue(result is Result.Success)
        val completion = (result as Result.Success).value
        assertEquals(habitId, completion.habitId)
        assertTrue(completion.type is CompletionType.Skipped)
        assertEquals(null, completion.skipReason)
        coVerify(exactly = 1) { completionRepository.insert(any()) }
    }

    @Test
    fun `habit not found returns error`() = runTest {
        val unknownId = UUID.randomUUID()
        coEvery { habitRepository.getById(unknownId) } returns Result.Error("Not found")

        val result = useCase(unknownId)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("Habit not found"))
        coVerify(exactly = 0) { completionRepository.insert(any()) }
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

        val result = useCase(habitId)

        assertTrue(result is Result.Error)
        assertEquals(
            "A completion already exists for this habit today",
            (result as Result.Error).message
        )
        coVerify(exactly = 0) { completionRepository.insert(any()) }
    }

    @Test
    fun `repository error on duplicate check is propagated`() = runTest {
        val habit = validHabit()
        coEvery { habitRepository.getById(habitId) } returns Result.Success(habit)
        coEvery {
            completionRepository.getForHabitOnDate(habitId, any())
        } returns Result.Error("Database read error")

        val result = useCase(habitId)

        assertTrue(result is Result.Error)
        assertEquals("Database read error", (result as Result.Error).message)
        coVerify(exactly = 0) { completionRepository.insert(any()) }
    }

    @Test
    fun `general exception is caught and wrapped in Result Error`() = runTest {
        coEvery { habitRepository.getById(habitId) } throws RuntimeException("Connection lost")

        val result = useCase(habitId)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("Connection lost"))
        coVerify(exactly = 0) { completionRepository.insert(any()) }
    }

    @Test
    fun `insert repository error is propagated`() = runTest {
        val habit = validHabit()
        coEvery { habitRepository.getById(habitId) } returns Result.Success(habit)
        coEvery {
            completionRepository.getForHabitOnDate(habitId, any())
        } returns Result.Success(null)
        coEvery {
            completionRepository.insert(any())
        } returns Result.Error("Insert failed")

        val result = useCase(habitId)

        assertTrue(result is Result.Error)
        assertEquals("Insert failed", (result as Result.Error).message)
    }
}
