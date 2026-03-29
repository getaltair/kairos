package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.RoutineExecution
import com.getaltair.kairos.domain.enums.ExecutionStatus
import com.getaltair.kairos.domain.repository.RoutineExecutionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AbandonRoutineUseCaseTest {

    private lateinit var routineExecutionRepository: RoutineExecutionRepository
    private lateinit var useCase: AbandonRoutineUseCase

    private val routineId = UUID.randomUUID()

    private fun inProgressExecution(executionId: UUID = UUID.randomUUID()) = RoutineExecution(
        id = executionId,
        routineId = routineId,
        status = ExecutionStatus.InProgress,
        currentStepIndex = 2,
    )

    private fun pausedExecution(executionId: UUID = UUID.randomUUID()) = RoutineExecution(
        id = executionId,
        routineId = routineId,
        status = ExecutionStatus.Paused,
        currentStepIndex = 1,
    )

    @Before
    fun setup() {
        routineExecutionRepository = mockk()
        useCase = AbandonRoutineUseCase(routineExecutionRepository)
    }

    @Test
    fun `abandons InProgress execution successfully`() = runTest {
        val execution = inProgressExecution()
        coEvery { routineExecutionRepository.getById(execution.id) } returns Result.Success(execution)

        val updateSlot = slot<RoutineExecution>()
        coEvery { routineExecutionRepository.update(capture(updateSlot)) } answers {
            Result.Success(updateSlot.captured)
        }

        val result = useCase(execution.id)

        assertTrue(result is Result.Success)
        val abandoned = (result as Result.Success).value
        assertEquals(ExecutionStatus.Abandoned, abandoned.status)
        assertNotNull(abandoned.completedAt)
        assertEquals(execution.routineId, abandoned.routineId)
        coVerify(exactly = 1) { routineExecutionRepository.update(any()) }
    }

    @Test
    fun `abandons Paused execution successfully`() = runTest {
        val execution = pausedExecution()
        coEvery { routineExecutionRepository.getById(execution.id) } returns Result.Success(execution)

        val updateSlot = slot<RoutineExecution>()
        coEvery { routineExecutionRepository.update(capture(updateSlot)) } answers {
            Result.Success(updateSlot.captured)
        }

        val result = useCase(execution.id)

        assertTrue(result is Result.Success)
        val abandoned = (result as Result.Success).value
        assertEquals(ExecutionStatus.Abandoned, abandoned.status)
        assertNotNull(abandoned.completedAt)
    }

    @Test
    fun `rejects abandoning a Completed execution`() = runTest {
        val execution = RoutineExecution(
            routineId = routineId,
            status = ExecutionStatus.Completed,
            currentStepIndex = 3,
            completedAt = java.time.Instant.now(),
        )
        coEvery { routineExecutionRepository.getById(execution.id) } returns Result.Success(execution)

        val result = useCase(execution.id)

        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.message.contains("Cannot abandon execution"))
        assertTrue(error.message.contains("Completed"))
        coVerify(exactly = 0) { routineExecutionRepository.update(any()) }
    }

    @Test
    fun `rejects abandoning an already Abandoned execution`() = runTest {
        val execution = RoutineExecution(
            routineId = routineId,
            status = ExecutionStatus.Abandoned,
            currentStepIndex = 1,
            completedAt = java.time.Instant.now(),
        )
        coEvery { routineExecutionRepository.getById(execution.id) } returns Result.Success(execution)

        val result = useCase(execution.id)

        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.message.contains("Cannot abandon execution"))
        assertTrue(error.message.contains("Abandoned"))
        coVerify(exactly = 0) { routineExecutionRepository.update(any()) }
    }

    @Test
    fun `rejects abandoning a NotStarted execution`() = runTest {
        val execution = RoutineExecution(
            routineId = routineId,
            status = ExecutionStatus.NotStarted,
            currentStepIndex = 0,
        )
        coEvery { routineExecutionRepository.getById(execution.id) } returns Result.Success(execution)

        val result = useCase(execution.id)

        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.message.contains("Cannot abandon execution"))
        assertTrue(error.message.contains("Not started"))
        coVerify(exactly = 0) { routineExecutionRepository.update(any()) }
    }

    @Test
    fun `returns error when execution not found (null)`() = runTest {
        val unknownId = UUID.randomUUID()
        coEvery { routineExecutionRepository.getById(unknownId) } returns Result.Success(null)

        val result = useCase(unknownId)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("Execution not found"))
        coVerify(exactly = 0) { routineExecutionRepository.update(any()) }
    }

    @Test
    fun `returns error when repository getById returns error`() = runTest {
        val unknownId = UUID.randomUUID()
        coEvery {
            routineExecutionRepository.getById(unknownId)
        } returns Result.Error("Database timeout")

        val result = useCase(unknownId)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("Execution not found"))
        assertTrue(result.message.contains("Database timeout"))
        coVerify(exactly = 0) { routineExecutionRepository.update(any()) }
    }

    @Test
    fun `wraps unexpected exception in Result Error`() = runTest {
        val execution = inProgressExecution()
        coEvery { routineExecutionRepository.getById(execution.id) } returns Result.Success(execution)
        coEvery {
            routineExecutionRepository.update(any())
        } throws RuntimeException("Update failed")

        val result = useCase(execution.id)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("Failed to abandon routine"))
        assertTrue(result.message.contains("Update failed"))
    }

    @Test(expected = CancellationException::class)
    fun `CancellationException is rethrown not wrapped`() = runTest {
        val execution = inProgressExecution()
        coEvery { routineExecutionRepository.getById(execution.id) } returns Result.Success(execution)
        coEvery {
            routineExecutionRepository.update(any())
        } throws CancellationException("Job cancelled")

        useCase(execution.id)
    }

    @Test
    fun `preserves original execution fields after abandoning`() = runTest {
        val execution = inProgressExecution()
        coEvery { routineExecutionRepository.getById(execution.id) } returns Result.Success(execution)

        val updateSlot = slot<RoutineExecution>()
        coEvery { routineExecutionRepository.update(capture(updateSlot)) } answers {
            Result.Success(updateSlot.captured)
        }

        useCase(execution.id)

        val updated = updateSlot.captured
        assertEquals(execution.id, updated.id)
        assertEquals(execution.routineId, updated.routineId)
        assertEquals(execution.currentStepIndex, updated.currentStepIndex)
    }
}
