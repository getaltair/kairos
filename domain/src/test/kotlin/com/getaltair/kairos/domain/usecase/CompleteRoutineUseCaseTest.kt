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

class CompleteRoutineUseCaseTest {

    private lateinit var routineExecutionRepository: RoutineExecutionRepository
    private lateinit var useCase: CompleteRoutineUseCase

    private val routineId = UUID.randomUUID()
    private val executionId = UUID.randomUUID()

    private fun inProgressExecution() = RoutineExecution(
        id = executionId,
        routineId = routineId,
        status = ExecutionStatus.InProgress,
        currentStepIndex = 3,
    )

    @Before
    fun setup() {
        routineExecutionRepository = mockk()
        useCase = CompleteRoutineUseCase(routineExecutionRepository)
    }

    @Test
    fun `E-3 marks InProgress execution as Completed with completedAt timestamp`() = runTest {
        val execution = inProgressExecution()
        coEvery {
            routineExecutionRepository.getById(executionId)
        } returns Result.Success(execution)

        val updateSlot = slot<RoutineExecution>()
        coEvery {
            routineExecutionRepository.update(capture(updateSlot))
        } answers {
            Result.Success(updateSlot.captured)
        }

        val result = useCase(executionId)

        assertTrue(result is Result.Success)
        val completed = (result as Result.Success).value
        assertEquals(ExecutionStatus.Completed, completed.status)
        assertNotNull(completed.completedAt)
        assertEquals(executionId, completed.id)
        coVerify(exactly = 1) { routineExecutionRepository.update(any()) }
    }

    @Test
    fun `E-3 marks Paused execution as Completed`() = runTest {
        val execution = RoutineExecution(
            id = executionId,
            routineId = routineId,
            status = ExecutionStatus.Paused,
            currentStepIndex = 2,
        )

        coEvery {
            routineExecutionRepository.getById(executionId)
        } returns Result.Success(execution)

        val updateSlot = slot<RoutineExecution>()
        coEvery {
            routineExecutionRepository.update(capture(updateSlot))
        } answers {
            Result.Success(updateSlot.captured)
        }

        val result = useCase(executionId)

        assertTrue(result is Result.Success)
        val completed = (result as Result.Success).value
        assertEquals(ExecutionStatus.Completed, completed.status)
        assertNotNull(completed.completedAt)
    }

    @Test
    fun `returns failure when execution is not found`() = runTest {
        val unknownId = UUID.randomUUID()
        coEvery {
            routineExecutionRepository.getById(unknownId)
        } returns Result.Success(null)

        val result = useCase(unknownId)

        assertTrue(result is Result.Error)
        assertTrue(
            (result as Result.Error).message.contains("Execution not found")
        )
        coVerify(exactly = 0) { routineExecutionRepository.update(any()) }
    }

    @Test
    fun `returns failure when execution lookup returns error`() = runTest {
        coEvery {
            routineExecutionRepository.getById(executionId)
        } returns Result.Error("Database error")

        val result = useCase(executionId)

        assertTrue(result is Result.Error)
        assertTrue(
            (result as Result.Error).message.contains("Execution not found")
        )
        coVerify(exactly = 0) { routineExecutionRepository.update(any()) }
    }

    @Test
    fun `returns failure when execution is already Completed`() = runTest {
        val completedExecution = RoutineExecution(
            id = executionId,
            routineId = routineId,
            status = ExecutionStatus.Completed,
            currentStepIndex = 3,
        )

        coEvery {
            routineExecutionRepository.getById(executionId)
        } returns Result.Success(completedExecution)

        val result = useCase(executionId)

        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.message.contains("Cannot complete execution"))
        assertTrue(error.message.contains("Completed"))
        coVerify(exactly = 0) { routineExecutionRepository.update(any()) }
    }

    @Test
    fun `returns failure when execution is Abandoned`() = runTest {
        val abandonedExecution = RoutineExecution(
            id = executionId,
            routineId = routineId,
            status = ExecutionStatus.Abandoned,
            currentStepIndex = 1,
        )

        coEvery {
            routineExecutionRepository.getById(executionId)
        } returns Result.Success(abandonedExecution)

        val result = useCase(executionId)

        assertTrue(result is Result.Error)
        assertTrue(
            (result as Result.Error).message.contains("Cannot complete execution")
        )
        coVerify(exactly = 0) { routineExecutionRepository.update(any()) }
    }

    @Test
    fun `returns failure when execution is NotStarted`() = runTest {
        val notStartedExecution = RoutineExecution(
            id = executionId,
            routineId = routineId,
            status = ExecutionStatus.NotStarted,
            currentStepIndex = 0,
        )

        coEvery {
            routineExecutionRepository.getById(executionId)
        } returns Result.Success(notStartedExecution)

        val result = useCase(executionId)

        assertTrue(result is Result.Error)
        assertTrue(
            (result as Result.Error).message.contains("Cannot complete execution")
        )
        coVerify(exactly = 0) { routineExecutionRepository.update(any()) }
    }

    @Test
    fun `repository update exception is caught and wrapped`() = runTest {
        val execution = inProgressExecution()
        coEvery {
            routineExecutionRepository.getById(executionId)
        } returns Result.Success(execution)
        coEvery {
            routineExecutionRepository.update(any())
        } throws RuntimeException("Update failed")

        val result = useCase(executionId)

        assertTrue(result is Result.Error)
        assertTrue(
            (result as Result.Error).message.contains("Update failed")
        )
    }

    @Test(expected = CancellationException::class)
    fun `CancellationException is rethrown not wrapped`() = runTest {
        val execution = inProgressExecution()
        coEvery {
            routineExecutionRepository.getById(executionId)
        } returns Result.Success(execution)
        coEvery {
            routineExecutionRepository.update(any())
        } throws CancellationException("Job cancelled")

        useCase(executionId)
    }
}
