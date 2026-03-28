package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Routine
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.repository.RoutineRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CreateRoutineUseCaseTest {

    private lateinit var routineRepository: RoutineRepository
    private lateinit var useCase: CreateRoutineUseCase

    @Before
    fun setup() {
        routineRepository = mockk()
        useCase = CreateRoutineUseCase(routineRepository)
    }

    @Test
    fun `happy path creates routine with valid params`() = runTest {
        val habitIds = listOf(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
        val routineSlot = slot<Routine>()

        coEvery {
            routineRepository.insert(capture(routineSlot), eq(habitIds))
        } answers {
            Result.Success(routineSlot.captured)
        }

        val result = useCase("Morning Routine", HabitCategory.Morning, habitIds)

        assertTrue(result is Result.Success)
        val routine = (result as Result.Success).value
        assertEquals("Morning Routine", routine.name)
        assertEquals(HabitCategory.Morning, routine.category)
        coVerify(exactly = 1) { routineRepository.insert(any(), eq(habitIds)) }
    }

    @Test
    fun `R-1 returns failure when less than 2 habits`() = runTest {
        val habitIds = listOf(UUID.randomUUID())

        val result = useCase("Morning Routine", HabitCategory.Morning, habitIds)

        assertTrue(result is Result.Error)
        assertTrue(
            (result as Result.Error).message.contains("at least 2 habits")
        )
        coVerify(exactly = 0) { routineRepository.insert(any(), any()) }
    }

    @Test
    fun `R-1 returns failure when 0 habits`() = runTest {
        val result = useCase("Morning Routine", HabitCategory.Morning, emptyList())

        assertTrue(result is Result.Error)
        assertTrue(
            (result as Result.Error).message.contains("at least 2 habits")
        )
        coVerify(exactly = 0) { routineRepository.insert(any(), any()) }
    }

    @Test
    fun `returns failure when name is blank`() = runTest {
        val habitIds = listOf(UUID.randomUUID(), UUID.randomUUID())

        val result = useCase("   ", HabitCategory.Morning, habitIds)

        assertTrue(result is Result.Error)
        assertTrue(
            (result as Result.Error).message.contains("blank")
        )
        coVerify(exactly = 0) { routineRepository.insert(any(), any()) }
    }

    @Test
    fun `returns failure when name exceeds 50 characters`() = runTest {
        val habitIds = listOf(UUID.randomUUID(), UUID.randomUUID())
        val longName = "A".repeat(51)

        val result = useCase(longName, HabitCategory.Morning, habitIds)

        assertTrue(result is Result.Error)
        assertTrue(
            (result as Result.Error).message.contains("50 characters")
        )
        coVerify(exactly = 0) { routineRepository.insert(any(), any()) }
    }

    @Test
    fun `R-4 returns failure when duration override is zero`() = runTest {
        val habitId1 = UUID.randomUUID()
        val habitId2 = UUID.randomUUID()
        val habitIds = listOf(habitId1, habitId2)
        val durations = mapOf(habitId1 to 0)

        val result = useCase("Morning Routine", HabitCategory.Morning, habitIds, durations)

        assertTrue(result is Result.Error)
        assertTrue(
            (result as Result.Error).message.contains("positive")
        )
        coVerify(exactly = 0) { routineRepository.insert(any(), any()) }
    }

    @Test
    fun `R-4 returns failure when duration override is negative`() = runTest {
        val habitId1 = UUID.randomUUID()
        val habitId2 = UUID.randomUUID()
        val habitIds = listOf(habitId1, habitId2)
        val durations = mapOf(habitId1 to -10)

        val result = useCase("Morning Routine", HabitCategory.Morning, habitIds, durations)

        assertTrue(result is Result.Error)
        assertTrue(
            (result as Result.Error).message.contains("positive")
        )
        coVerify(exactly = 0) { routineRepository.insert(any(), any()) }
    }

    @Test
    fun `repository failure is propagated`() = runTest {
        val habitIds = listOf(UUID.randomUUID(), UUID.randomUUID())

        coEvery {
            routineRepository.insert(any(), any())
        } returns Result.Error("Database error")

        val result = useCase("Morning Routine", HabitCategory.Morning, habitIds)

        assertTrue(result is Result.Error)
        assertEquals("Database error", (result as Result.Error).message)
    }

    @Test
    fun `repository exception is caught and wrapped`() = runTest {
        val habitIds = listOf(UUID.randomUUID(), UUID.randomUUID())

        coEvery {
            routineRepository.insert(any(), any())
        } throws RuntimeException("Connection lost")

        val result = useCase("Morning Routine", HabitCategory.Morning, habitIds)

        assertTrue(result is Result.Error)
        assertTrue(
            (result as Result.Error).message.contains("Connection lost")
        )
    }

    @Test(expected = CancellationException::class)
    fun `CancellationException is rethrown not wrapped`() = runTest {
        val habitIds = listOf(UUID.randomUUID(), UUID.randomUUID())

        coEvery {
            routineRepository.insert(any(), any())
        } throws CancellationException("Job cancelled")

        useCase("Morning Routine", HabitCategory.Morning, habitIds)
    }

    @Test
    fun `accepts valid duration overrides with null values`() = runTest {
        val habitId1 = UUID.randomUUID()
        val habitId2 = UUID.randomUUID()
        val habitIds = listOf(habitId1, habitId2)
        val durations = mapOf<UUID, Int?>(habitId1 to 60, habitId2 to null)
        val routineSlot = slot<Routine>()

        coEvery {
            routineRepository.insert(capture(routineSlot), eq(habitIds))
        } answers {
            Result.Success(routineSlot.captured)
        }

        val result = useCase("Morning Routine", HabitCategory.Morning, habitIds, durations)

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { routineRepository.insert(any(), eq(habitIds)) }
    }
}
