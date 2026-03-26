package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Completion
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.enums.AnchorType
import com.getaltair.kairos.domain.enums.CompletionType
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.HabitFrequency
import com.getaltair.kairos.domain.enums.HabitStatus
import com.getaltair.kairos.domain.repository.CompletionRepository
import com.getaltair.kairos.domain.repository.HabitRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BackdateCompletionUseCaseTest {

    private lateinit var habitRepository: HabitRepository
    private lateinit var completionRepository: CompletionRepository
    private lateinit var useCase: BackdateCompletionUseCase

    private fun activeHabit() = Habit(
        name = "Meditate",
        anchorBehavior = "After brushing teeth",
        anchorType = AnchorType.AfterBehavior,
        category = HabitCategory.Morning,
        frequency = HabitFrequency.Daily,
        status = HabitStatus.Active,
        createdAt = Instant.parse("2025-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2025-01-01T00:00:00Z")
    )

    @Before
    fun setup() {
        habitRepository = mockk()
        completionRepository = mockk()
        useCase = BackdateCompletionUseCase(habitRepository, completionRepository)
    }

    @Test
    fun `valid backdate within 7 days succeeds`() = runTest {
        val habit = activeHabit()
        val yesterday = LocalDate.now().minusDays(1)
        coEvery { habitRepository.getById(habit.id) } returns Result.Success(habit)
        coEvery { completionRepository.getForHabitOnDate(habit.id, yesterday) } returns Result.Success(null)
        coEvery { completionRepository.insert(any()) } answers {
            Result.Success(firstArg<Completion>())
        }

        val result = useCase(habit.id, yesterday, CompletionType.Full)

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { completionRepository.insert(any()) }
    }

    @Test
    fun `backdate rejects future date`() = runTest {
        val habit = activeHabit()
        val tomorrow = LocalDate.now().plusDays(1)
        coEvery { habitRepository.getById(habit.id) } returns Result.Success(habit)

        val result = useCase(habit.id, tomorrow, CompletionType.Full)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("future"))
        coVerify(exactly = 0) { completionRepository.insert(any()) }
    }

    @Test
    fun `backdate rejects date older than 7 days`() = runTest {
        val habit = activeHabit()
        val tooOld = LocalDate.now().minusDays(8)
        coEvery { habitRepository.getById(habit.id) } returns Result.Success(habit)

        val result = useCase(habit.id, tooOld, CompletionType.Full)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("7 days"))
        coVerify(exactly = 0) { completionRepository.insert(any()) }
    }

    @Test
    fun `backdate rejects duplicate completion`() = runTest {
        val habit = activeHabit()
        val yesterday = LocalDate.now().minusDays(1)
        val existing = Completion(
            habitId = habit.id,
            date = yesterday,
            type = CompletionType.Full
        )
        coEvery { habitRepository.getById(habit.id) } returns Result.Success(habit)
        coEvery { completionRepository.getForHabitOnDate(habit.id, yesterday) } returns Result.Success(existing)

        val result = useCase(habit.id, yesterday, CompletionType.Full)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("already exists"))
        coVerify(exactly = 0) { completionRepository.insert(any()) }
    }

    @Test
    fun `backdate rejects non-Active habit`() = runTest {
        val habit = activeHabit().copy(
            status = HabitStatus.Paused,
            pausedAt = Instant.now()
        )
        val yesterday = LocalDate.now().minusDays(1)
        coEvery { habitRepository.getById(habit.id) } returns Result.Success(habit)

        val result = useCase(habit.id, yesterday, CompletionType.Full)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("expected Active"))
    }

    @Test
    fun `backdate rejects MISSED completion type`() = runTest {
        val habit = activeHabit()
        val yesterday = LocalDate.now().minusDays(1)
        coEvery { habitRepository.getById(habit.id) } returns Result.Success(habit)

        val result = useCase(habit.id, yesterday, CompletionType.Missed)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("MISSED"))
    }

    @Test
    fun `backdate non-existent habit returns error`() = runTest {
        val habit = activeHabit()
        val yesterday = LocalDate.now().minusDays(1)
        coEvery { habitRepository.getById(habit.id) } returns Result.Error("Not found")

        val result = useCase(habit.id, yesterday, CompletionType.Full)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("Habit not found"))
    }
}
