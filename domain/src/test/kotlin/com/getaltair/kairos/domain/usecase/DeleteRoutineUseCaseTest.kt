package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.repository.RoutineRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeleteRoutineUseCaseTest {

    private lateinit var routineRepository: RoutineRepository
    private lateinit var useCase: DeleteRoutineUseCase

    private val routineId = UUID.randomUUID()

    @Before
    fun setup() {
        routineRepository = mockk()
        useCase = DeleteRoutineUseCase(routineRepository)
    }

    @Test
    fun `deletes routine successfully`() = runTest {
        coEvery { routineRepository.delete(routineId) } returns Result.Success(Unit)

        val result = useCase(routineId)

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { routineRepository.delete(routineId) }
    }

    @Test
    fun `propagates repository error`() = runTest {
        coEvery {
            routineRepository.delete(routineId)
        } returns Result.Error("Routine not found")

        val result = useCase(routineId)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("Routine not found"))
    }

    @Test
    fun `wraps unexpected exception in Result Error`() = runTest {
        coEvery {
            routineRepository.delete(routineId)
        } throws RuntimeException("Database locked")

        val result = useCase(routineId)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("Failed to delete routine"))
        assertTrue(result.message.contains("Database locked"))
    }

    @Test(expected = CancellationException::class)
    fun `CancellationException is rethrown not wrapped`() = runTest {
        coEvery {
            routineRepository.delete(routineId)
        } throws CancellationException("Job cancelled")

        useCase(routineId)
    }

    @Test
    fun `passes correct ID to repository`() = runTest {
        val specificId = UUID.randomUUID()
        coEvery { routineRepository.delete(specificId) } returns Result.Success(Unit)

        val result = useCase(specificId)

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { routineRepository.delete(specificId) }
    }
}
