package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Completion
import com.getaltair.kairos.domain.entity.RoutineExecution
import com.getaltair.kairos.domain.enums.CompletionType
import com.getaltair.kairos.domain.enums.ExecutionStatus
import com.getaltair.kairos.domain.enums.StepResult
import com.getaltair.kairos.domain.repository.CompletionRepository
import com.getaltair.kairos.domain.repository.RoutineExecutionRepository
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

class AdvanceRoutineStepUseCaseTest {

    private lateinit var routineExecutionRepository: RoutineExecutionRepository
    private lateinit var completionRepository: CompletionRepository
    private lateinit var useCase: AdvanceRoutineStepUseCase

    private val routineId = UUID.randomUUID()
    private val executionId = UUID.randomUUID()
    private val habitId = UUID.randomUUID()

    private fun inProgressExecution(stepIndex: Int = 0) = RoutineExecution(
        id = executionId,
        routineId = routineId,
        status = ExecutionStatus.InProgress,
        currentStepIndex = stepIndex,
    )

    @Before
    fun setup() {
        routineExecutionRepository = mockk()
        completionRepository = mockk()
        useCase = AdvanceRoutineStepUseCase(routineExecutionRepository, completionRepository)
    }

    @Test
    fun `advances to next step and increments currentStepIndex`() = runTest {
        val execution = inProgressExecution(stepIndex = 1)
        coEvery {
            routineExecutionRepository.getById(executionId)
        } returns Result.Success(execution)

        val completionSlot = slot<Completion>()
        coEvery { completionRepository.insert(capture(completionSlot)) } answers {
            Result.Success(completionSlot.captured)
        }

        val updateSlot = slot<RoutineExecution>()
        coEvery {
            routineExecutionRepository.update(capture(updateSlot))
        } answers {
            Result.Success(updateSlot.captured)
        }

        val result = useCase(executionId, StepResult.Completed, habitId)

        assertTrue(result is Result.Success)
        val updated = (result as Result.Success).value
        assertEquals(2, updated.currentStepIndex)
        coVerify(exactly = 1) { completionRepository.insert(any()) }
        coVerify(exactly = 1) { routineExecutionRepository.update(any()) }
    }

    @Test
    fun `E-3 Completed step creates Full completion`() = runTest {
        val execution = inProgressExecution(stepIndex = 0)
        coEvery {
            routineExecutionRepository.getById(executionId)
        } returns Result.Success(execution)

        val completionSlot = slot<Completion>()
        coEvery { completionRepository.insert(capture(completionSlot)) } answers {
            Result.Success(completionSlot.captured)
        }

        val updateSlot = slot<RoutineExecution>()
        coEvery {
            routineExecutionRepository.update(capture(updateSlot))
        } answers {
            Result.Success(updateSlot.captured)
        }

        useCase(executionId, StepResult.Completed, habitId)

        val inserted = completionSlot.captured
        assertEquals(habitId, inserted.habitId)
        assertTrue(inserted.type is CompletionType.Full)
    }

    @Test
    fun `E-3 Skipped step creates Skipped completion`() = runTest {
        val execution = inProgressExecution(stepIndex = 0)
        coEvery {
            routineExecutionRepository.getById(executionId)
        } returns Result.Success(execution)

        val completionSlot = slot<Completion>()
        coEvery { completionRepository.insert(capture(completionSlot)) } answers {
            Result.Success(completionSlot.captured)
        }

        val updateSlot = slot<RoutineExecution>()
        coEvery {
            routineExecutionRepository.update(capture(updateSlot))
        } answers {
            Result.Success(updateSlot.captured)
        }

        useCase(executionId, StepResult.Skipped, habitId)

        val inserted = completionSlot.captured
        assertEquals(habitId, inserted.habitId)
        assertTrue(inserted.type is CompletionType.Skipped)
    }

    @Test
    fun `advances from step 0 to step 1`() = runTest {
        val execution = inProgressExecution(stepIndex = 0)
        coEvery {
            routineExecutionRepository.getById(executionId)
        } returns Result.Success(execution)

        val completionSlot = slot<Completion>()
        coEvery { completionRepository.insert(capture(completionSlot)) } answers {
            Result.Success(completionSlot.captured)
        }

        val updateSlot = slot<RoutineExecution>()
        coEvery {
            routineExecutionRepository.update(capture(updateSlot))
        } answers {
            Result.Success(updateSlot.captured)
        }

        val result = useCase(executionId, StepResult.Completed, habitId)

        assertTrue(result is Result.Success)
        assertEquals(1, (result as Result.Success).value.currentStepIndex)
    }

    @Test
    fun `returns failure when execution is not found`() = runTest {
        val unknownId = UUID.randomUUID()
        coEvery {
            routineExecutionRepository.getById(unknownId)
        } returns Result.Success(null)

        val result = useCase(unknownId, StepResult.Completed, habitId)

        assertTrue(result is Result.Error)
        assertTrue(
            (result as Result.Error).message.contains("Execution not found")
        )
        coVerify(exactly = 0) { completionRepository.insert(any()) }
        coVerify(exactly = 0) { routineExecutionRepository.update(any()) }
    }

    @Test
    fun `returns failure when execution lookup returns error`() = runTest {
        coEvery {
            routineExecutionRepository.getById(executionId)
        } returns Result.Error("Database error")

        val result = useCase(executionId, StepResult.Completed, habitId)

        assertTrue(result is Result.Error)
        assertTrue(
            (result as Result.Error).message.contains("Execution not found")
        )
        coVerify(exactly = 0) { completionRepository.insert(any()) }
    }

    @Test
    fun `returns failure when execution is not InProgress`() = runTest {
        val pausedExecution = RoutineExecution(
            id = executionId,
            routineId = routineId,
            status = ExecutionStatus.Paused,
            currentStepIndex = 1,
        )

        coEvery {
            routineExecutionRepository.getById(executionId)
        } returns Result.Success(pausedExecution)

        val result = useCase(executionId, StepResult.Completed, habitId)

        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.message.contains("Cannot advance step"))
        assertTrue(error.message.contains("Paused"))
        coVerify(exactly = 0) { completionRepository.insert(any()) }
    }

    @Test
    fun `returns failure when execution is Completed`() = runTest {
        val completedExecution = RoutineExecution(
            id = executionId,
            routineId = routineId,
            status = ExecutionStatus.Completed,
            currentStepIndex = 3,
        )

        coEvery {
            routineExecutionRepository.getById(executionId)
        } returns Result.Success(completedExecution)

        val result = useCase(executionId, StepResult.Completed, habitId)

        assertTrue(result is Result.Error)
        assertTrue(
            (result as Result.Error).message.contains("Cannot advance step")
        )
        coVerify(exactly = 0) { completionRepository.insert(any()) }
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

        val result = useCase(executionId, StepResult.Completed, habitId)

        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.message.contains("Cannot advance step"))
        assertTrue(error.message.contains("Abandoned"))
        coVerify(exactly = 0) { completionRepository.insert(any()) }
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

        val result = useCase(executionId, StepResult.Completed, habitId)

        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.message.contains("Cannot advance step"))
        assertTrue(error.message.contains("Not started"))
        coVerify(exactly = 0) { completionRepository.insert(any()) }
        coVerify(exactly = 0) { routineExecutionRepository.update(any()) }
    }

    @Test
    fun `returns failure when completion insert fails`() = runTest {
        val execution = inProgressExecution(stepIndex = 0)
        coEvery {
            routineExecutionRepository.getById(executionId)
        } returns Result.Success(execution)

        coEvery {
            completionRepository.insert(any())
        } returns Result.Error("Insert failed")

        val result = useCase(executionId, StepResult.Completed, habitId)

        assertTrue(result is Result.Error)
        assertTrue(
            (result as Result.Error).message.contains("Failed to record habit completion")
        )
        coVerify(exactly = 0) { routineExecutionRepository.update(any()) }
    }

    @Test
    fun `repository exception is caught and wrapped`() = runTest {
        val execution = inProgressExecution(stepIndex = 0)
        coEvery {
            routineExecutionRepository.getById(executionId)
        } returns Result.Success(execution)

        coEvery {
            completionRepository.insert(any())
        } throws RuntimeException("Connection lost")

        val result = useCase(executionId, StepResult.Completed, habitId)

        assertTrue(result is Result.Error)
        assertTrue(
            (result as Result.Error).message.contains("Connection lost")
        )
    }

    @Test(expected = CancellationException::class)
    fun `CancellationException is rethrown not wrapped`() = runTest {
        val execution = inProgressExecution(stepIndex = 0)
        coEvery {
            routineExecutionRepository.getById(executionId)
        } returns Result.Success(execution)

        coEvery {
            completionRepository.insert(any())
        } throws CancellationException("Job cancelled")

        useCase(executionId, StepResult.Completed, habitId)
    }
}
