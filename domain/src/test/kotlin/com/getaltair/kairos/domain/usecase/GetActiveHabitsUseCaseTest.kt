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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetActiveHabitsUseCaseTest {

    private lateinit var habitRepository: HabitRepository
    private lateinit var useCase: GetActiveHabitsUseCase

    private fun habit(name: String, category: HabitCategory = HabitCategory.Morning) = Habit(
        name = name,
        anchorBehavior = "After waking up",
        anchorType = AnchorType.AfterBehavior,
        category = category,
        frequency = HabitFrequency.Daily,
        createdAt = Instant.parse("2025-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2025-01-01T00:00:00Z")
    )

    @Before
    fun setup() {
        habitRepository = mockk()
        useCase = GetActiveHabitsUseCase(habitRepository)
    }

    @Test
    fun `invoke returns list of active habits`() = runTest {
        // given
        val habits = listOf(
            habit("Meditate"),
            habit("Exercise", HabitCategory.Afternoon),
            habit("Journal", HabitCategory.Evening)
        )
        coEvery { habitRepository.getActiveHabits() } returns Result.Success(habits)

        // when
        val result = useCase()

        // then
        assertTrue(result.isSuccess)
        val returnedHabits = (result as Result.Success).value
        assertEquals(3, returnedHabits.size)
        assertEquals("Meditate", returnedHabits[0].name)
        assertEquals("Exercise", returnedHabits[1].name)
        assertEquals("Journal", returnedHabits[2].name)
        coVerify(exactly = 1) { habitRepository.getActiveHabits() }
    }

    @Test
    fun `invoke returns empty list when no active habits exist`() = runTest {
        // given
        coEvery { habitRepository.getActiveHabits() } returns Result.Success(emptyList())

        // when
        val result = useCase()

        // then
        assertTrue(result.isSuccess)
        val returnedHabits = (result as Result.Success).value
        assertTrue(returnedHabits.isEmpty())
        assertEquals(0, returnedHabits.size)
    }

    @Test
    fun `invoke returns error when repository fails`() = runTest {
        // given
        coEvery { habitRepository.getActiveHabits() } returns Result.Error("Database locked")

        // when
        val result = useCase()

        // then
        assertTrue(result.isError)
        assertEquals("Database locked", (result as Result.Error).message)
    }

    @Test
    fun `invoke wraps unexpected exception in Result Error`() = runTest {
        // given
        coEvery { habitRepository.getActiveHabits() } throws RuntimeException("IO error")

        // when
        val result = useCase()

        // then
        assertTrue(result.isError)
        assertTrue((result as Result.Error).message.contains("Failed to load active habits"))
        assertTrue(result.message.contains("IO error"))
        assertTrue(result.cause is RuntimeException)
    }

    @Test
    fun `invoke rethrows CancellationException`() = runTest {
        // given
        coEvery { habitRepository.getActiveHabits() } throws CancellationException("Job cancelled")

        // when / then
        try {
            useCase()
            assertTrue("Expected CancellationException to be rethrown", false)
        } catch (e: CancellationException) {
            assertEquals("Job cancelled", e.message)
        }
    }
}
