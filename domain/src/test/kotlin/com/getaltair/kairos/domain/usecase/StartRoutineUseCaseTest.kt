package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Routine
import com.getaltair.kairos.domain.entity.RoutineExecution
import com.getaltair.kairos.domain.enums.ExecutionStatus
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.repository.RoutineExecutionRepository
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

class StartRoutineUseCaseTest {

    private lateinit var routineRepository: RoutineRepository
    private lateinit var routineExecutionRepository: RoutineExecutionRepository
    private lateinit var useCase: StartRoutineUseCase

    private val routineId = UUID.randomUUID()

    private fun validRoutine() = Routine(
        id = routineId,
        name = "Morning Routine",
        category = HabitCategory.Morning,
    )

    @Before
    fun setup() {
        routineRepository = mockk()
        routineExecutionRepository = mockk()
        useCase = StartRoutineUseCase(routineRepository, routineExecutionRepository)
    }

    @Test
    fun `happy path starts new execution when no active execution exists`() = runTest {
        coEvery { routineRepository.getById(routineId) } returns Result.Success(validRoutine())
        coEvery {
            routineExecutionRepository.getActiveForRoutine(routineId)
        } returns Result.Success(null)

        val executionSlot = slot<RoutineExecution>()
        coEvery {
            routineExecutionRepository.insert(capture(executionSlot))
        } answers {
            Result.Success(executionSlot.captured)
        }

        val result = useCase(routineId)

        assertTrue(result is Result.Success)
        val execution = (result as Result.Success).value
        assertEquals(routineId, execution.routineId)
        assertEquals(ExecutionStatus.InProgress, execution.status)
        assertEquals(0, execution.currentStepIndex)
        coVerify(exactly = 1) { routineExecutionRepository.insert(any()) }
    }

    @Test
    fun `E-1 returns failure when InProgress execution already exists`() = runTest {
        val activeExecution = RoutineExecution(
            routineId = routineId,
            status = ExecutionStatus.InProgress,
            currentStepIndex = 1,
        )

        coEvery { routineRepository.getById(routineId) } returns Result.Success(validRoutine())
        coEvery {
            routineExecutionRepository.getActiveForRoutine(routineId)
        } returns Result.Success(activeExecution)

        val result = useCase(routineId)

        assertTrue(result is Result.Error)
        assertTrue(
            (result as Result.Error).message.contains("already has an active execution")
        )
        coVerify(exactly = 0) { routineExecutionRepository.insert(any()) }
    }

    @Test
    fun `E-1 returns failure when Paused execution already exists`() = runTest {
        val pausedExecution = RoutineExecution(
            routineId = routineId,
            status = ExecutionStatus.Paused,
            currentStepIndex = 2,
        )

        coEvery { routineRepository.getById(routineId) } returns Result.Success(validRoutine())
        coEvery {
            routineExecutionRepository.getActiveForRoutine(routineId)
        } returns Result.Success(pausedExecution)

        val result = useCase(routineId)

        assertTrue(result is Result.Error)
        assertTrue(
            (result as Result.Error).message.contains("already has an active execution")
        )
        coVerify(exactly = 0) { routineExecutionRepository.insert(any()) }
    }

    @Test
    fun `returns failure when routine does not exist`() = runTest {
        val unknownId = UUID.randomUUID()
        coEvery { routineRepository.getById(unknownId) } returns Result.Success(null)

        val result = useCase(unknownId)

        assertTrue(result is Result.Error)
        assertTrue(
            (result as Result.Error).message.contains("Routine not found")
        )
        coVerify(exactly = 0) { routineExecutionRepository.insert(any()) }
    }

    @Test
    fun `returns failure when routine lookup returns error`() = runTest {
        coEvery {
            routineRepository.getById(routineId)
        } returns Result.Error("Database error")

        val result = useCase(routineId)

        assertTrue(result is Result.Error)
        assertTrue(
            (result as Result.Error).message.contains("Routine not found")
        )
        coVerify(exactly = 0) { routineExecutionRepository.insert(any()) }
    }

    @Test
    fun `returns failure when active execution check returns error`() = runTest {
        coEvery { routineRepository.getById(routineId) } returns Result.Success(validRoutine())
        coEvery {
            routineExecutionRepository.getActiveForRoutine(routineId)
        } returns Result.Error("Query failed")

        val result = useCase(routineId)

        assertTrue(result is Result.Error)
        assertTrue(
            (result as Result.Error).message.contains("Failed to check active executions")
        )
        coVerify(exactly = 0) { routineExecutionRepository.insert(any()) }
    }

    @Test
    fun `repository insert exception is caught and wrapped`() = runTest {
        coEvery { routineRepository.getById(routineId) } returns Result.Success(validRoutine())
        coEvery {
            routineExecutionRepository.getActiveForRoutine(routineId)
        } returns Result.Success(null)
        coEvery {
            routineExecutionRepository.insert(any())
        } throws RuntimeException("Insert failed")

        val result = useCase(routineId)

        assertTrue(result is Result.Error)
        assertTrue(
            (result as Result.Error).message.contains("Insert failed")
        )
    }

    @Test(expected = CancellationException::class)
    fun `CancellationException is rethrown not wrapped`() = runTest {
        coEvery { routineRepository.getById(routineId) } returns Result.Success(validRoutine())
        coEvery {
            routineExecutionRepository.getActiveForRoutine(routineId)
        } returns Result.Success(null)
        coEvery {
            routineExecutionRepository.insert(any())
        } throws CancellationException("Job cancelled")

        useCase(routineId)
    }
}
