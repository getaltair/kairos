package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.RoutineExecution
import com.getaltair.kairos.domain.enums.ExecutionStatus
import com.getaltair.kairos.domain.repository.RoutineExecutionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetRoutineExecutionUseCaseTest {

    private lateinit var routineExecutionRepository: RoutineExecutionRepository
    private lateinit var useCase: GetRoutineExecutionUseCase

    private val routineId = UUID.randomUUID()

    private fun completedExecution(executionId: UUID = UUID.randomUUID()) = RoutineExecution(
        id = executionId,
        routineId = routineId,
        status = ExecutionStatus.Completed,
        currentStepIndex = 3,
        completedAt = java.time.Instant.now(),
    )

    @Before
    fun setup() {
        routineExecutionRepository = mockk()
        useCase = GetRoutineExecutionUseCase(routineExecutionRepository)
    }

    @Test
    fun `returns execution when found`() = runTest {
        val execution = completedExecution()
        coEvery { routineExecutionRepository.getById(execution.id) } returns Result.Success(execution)

        val result = useCase(execution.id)

        assertTrue(result is Result.Success)
        val returned = (result as Result.Success).value
        assertEquals(execution.id, returned.id)
        assertEquals(execution.routineId, returned.routineId)
        assertEquals(ExecutionStatus.Completed, returned.status)
        coVerify(exactly = 1) { routineExecutionRepository.getById(execution.id) }
    }

    @Test
    fun `returns error when execution is null`() = runTest {
        val unknownId = UUID.randomUUID()
        coEvery { routineExecutionRepository.getById(unknownId) } returns Result.Success(null)

        val result = useCase(unknownId)

        assertTrue(result is Result.Error)
        assertEquals("Execution not found", (result as Result.Error).message)
    }

    @Test
    fun `propagates repository error`() = runTest {
        val executionId = UUID.randomUUID()
        coEvery {
            routineExecutionRepository.getById(executionId)
        } returns Result.Error("Database unavailable")

        val result = useCase(executionId)

        assertTrue(result is Result.Error)
        assertEquals("Database unavailable", (result as Result.Error).message)
    }

    @Test
    fun `wraps unexpected exception in Result Error`() = runTest {
        val executionId = UUID.randomUUID()
        coEvery {
            routineExecutionRepository.getById(executionId)
        } throws RuntimeException("Connection reset")

        val result = useCase(executionId)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("Failed to load execution"))
        assertTrue(result.message.contains("Connection reset"))
    }

    @Test(expected = CancellationException::class)
    fun `CancellationException is rethrown not wrapped`() = runTest {
        val executionId = UUID.randomUUID()
        coEvery {
            routineExecutionRepository.getById(executionId)
        } throws CancellationException("Job cancelled")

        useCase(executionId)
    }

    @Test
    fun `returns InProgress execution correctly`() = runTest {
        val execution = RoutineExecution(
            routineId = routineId,
            status = ExecutionStatus.InProgress,
            currentStepIndex = 1,
        )
        coEvery { routineExecutionRepository.getById(execution.id) } returns Result.Success(execution)

        val result = useCase(execution.id)

        assertTrue(result is Result.Success)
        val returned = (result as Result.Success).value
        assertEquals(ExecutionStatus.InProgress, returned.status)
        assertEquals(1, returned.currentStepIndex)
    }

    @Test
    fun `returns Abandoned execution correctly`() = runTest {
        val execution = RoutineExecution(
            routineId = routineId,
            status = ExecutionStatus.Abandoned,
            currentStepIndex = 2,
            completedAt = java.time.Instant.now(),
        )
        coEvery { routineExecutionRepository.getById(execution.id) } returns Result.Success(execution)

        val result = useCase(execution.id)

        assertTrue(result is Result.Success)
        val returned = (result as Result.Success).value
        assertEquals(ExecutionStatus.Abandoned, returned.status)
    }
}
