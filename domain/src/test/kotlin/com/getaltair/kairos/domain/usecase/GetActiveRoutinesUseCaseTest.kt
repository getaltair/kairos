package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Routine
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.RoutineStatus
import com.getaltair.kairos.domain.repository.RoutineRepository
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

class GetActiveRoutinesUseCaseTest {

    private lateinit var routineRepository: RoutineRepository
    private lateinit var useCase: GetActiveRoutinesUseCase

    private fun routine(name: String, category: HabitCategory = HabitCategory.Morning) = Routine(
        name = name,
        category = category,
        status = RoutineStatus.Active,
        createdAt = Instant.parse("2025-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2025-01-01T00:00:00Z")
    )

    @Before
    fun setup() {
        routineRepository = mockk()
        useCase = GetActiveRoutinesUseCase(routineRepository)
    }

    @Test
    fun `invoke returns list of active routines`() = runTest {
        // given
        val routines = listOf(
            routine("Morning Routine"),
            routine("Evening Routine", HabitCategory.Evening)
        )
        coEvery { routineRepository.getActiveRoutines() } returns Result.Success(routines)

        // when
        val result = useCase()

        // then
        assertTrue(result.isSuccess)
        val returnedRoutines = (result as Result.Success).value
        assertEquals(2, returnedRoutines.size)
        assertEquals("Morning Routine", returnedRoutines[0].name)
        assertEquals("Evening Routine", returnedRoutines[1].name)
        coVerify(exactly = 1) { routineRepository.getActiveRoutines() }
    }

    @Test
    fun `invoke returns empty list when no active routines exist`() = runTest {
        // given
        coEvery { routineRepository.getActiveRoutines() } returns Result.Success(emptyList())

        // when
        val result = useCase()

        // then
        assertTrue(result.isSuccess)
        val returnedRoutines = (result as Result.Success).value
        assertTrue(returnedRoutines.isEmpty())
        assertEquals(0, returnedRoutines.size)
    }

    @Test
    fun `invoke returns error when repository fails`() = runTest {
        // given
        coEvery { routineRepository.getActiveRoutines() } returns Result.Error("Database locked")

        // when
        val result = useCase()

        // then
        assertTrue(result.isError)
        assertEquals("Database locked", (result as Result.Error).message)
    }

    @Test
    fun `invoke wraps unexpected exception in Result Error`() = runTest {
        // given
        coEvery { routineRepository.getActiveRoutines() } throws RuntimeException("DB error")

        // when
        val result = useCase()

        // then
        assertTrue(result.isError)
        assertTrue((result as Result.Error).message.contains("Failed to load routines"))
        assertTrue(result.message.contains("DB error"))
        assertTrue(result.cause is RuntimeException)
    }

    @Test
    fun `invoke rethrows CancellationException`() = runTest {
        // given
        coEvery {
            routineRepository.getActiveRoutines()
        } throws CancellationException("Job cancelled")

        // when / then
        try {
            useCase()
            assertTrue("Expected CancellationException to be rethrown", false)
        } catch (e: CancellationException) {
            assertEquals("Job cancelled", e.message)
        }
    }
}
