package com.getaltair.kairos.feature.routine

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.entity.Routine
import com.getaltair.kairos.domain.entity.RoutineExecution
import com.getaltair.kairos.domain.entity.RoutineHabit
import com.getaltair.kairos.domain.enums.AnchorType
import com.getaltair.kairos.domain.enums.ExecutionStatus
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.HabitFrequency
import com.getaltair.kairos.domain.enums.StepResult
import com.getaltair.kairos.domain.usecase.AbandonRoutineUseCase
import com.getaltair.kairos.domain.usecase.AdvanceRoutineStepUseCase
import com.getaltair.kairos.domain.usecase.CompleteRoutineUseCase
import com.getaltair.kairos.domain.usecase.GetRoutineDetailUseCase
import com.getaltair.kairos.domain.usecase.StartRoutineUseCase
import com.getaltair.kairos.feature.routine.service.RoutineTimerState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoutineRunnerViewModelTest {

    // StandardTestDispatcher gives us control over virtual time, which is
    // necessary because the ViewModel runs an infinite timer loop with delay(100).
    private val testDispatcher = StandardTestDispatcher()

    private val application = mockk<Application>(relaxed = true)
    private val getRoutineDetailUseCase: GetRoutineDetailUseCase = mockk()
    private val startRoutineUseCase: StartRoutineUseCase = mockk()
    private val advanceRoutineStepUseCase: AdvanceRoutineStepUseCase = mockk()
    private val completeRoutineUseCase: CompleteRoutineUseCase = mockk()
    private val abandonRoutineUseCase: AbandonRoutineUseCase = mockk()

    private val routineId = UUID.randomUUID()
    private val executionId = UUID.randomUUID()

    /**
     * Virtual clock that advances in sync with [testDispatcher].
     * Each delay(100) in the timer loop advances virtual time by 100ms,
     * so we mirror that by advancing the clock 100ms per call.
     */
    private val virtualNanos = AtomicLong(0L)
    private val testClock = NanoClock {
        virtualNanos.addAndGet(100_000_000L) // 100ms per read
    }

    /** Tracks created ViewModels for cleanup. */
    private val createdViewModels = mutableListOf<RoutineRunnerViewModel>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        RoutineTimerState.clearAction()
        Dispatchers.resetMain()
    }

    /**
     * Wrapper around [runTest] that cancels all ViewModel scopes before
     * the test scope completes. This prevents runTest from hanging on
     * infinite coroutines (timer loop, action collector) in viewModelScope.
     */
    private fun runViewModelTest(block: suspend TestScope.() -> Unit) {
        runTest {
            try {
                block()
            } finally {
                createdViewModels.forEach { it.viewModelScope.cancel() }
                createdViewModels.clear()
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helper factories
    // -------------------------------------------------------------------------

    private fun mockHabit(
        name: String = "Test Habit",
        id: UUID = UUID.randomUUID(),
        estimatedSeconds: Int = 300,
    ): Habit = Habit(
        id = id,
        name = name,
        anchorBehavior = "After waking up",
        anchorType = AnchorType.AfterBehavior,
        category = HabitCategory.Morning,
        frequency = HabitFrequency.Daily,
        estimatedSeconds = estimatedSeconds,
    )

    private fun mockRoutineHabit(
        habitId: UUID,
        routineId: UUID = this.routineId,
        orderIndex: Int = 0,
        overrideDurationSeconds: Int? = null,
    ): RoutineHabit = RoutineHabit(
        routineId = routineId,
        habitId = habitId,
        orderIndex = orderIndex,
        overrideDurationSeconds = overrideDurationSeconds,
    )

    private fun mockRoutine(name: String = "Morning Routine",): Routine = Routine(
        id = routineId,
        name = name,
        category = HabitCategory.Morning,
    )

    private fun mockExecution(): RoutineExecution = RoutineExecution(
        id = executionId,
        routineId = routineId,
        status = ExecutionStatus.InProgress,
        currentStepIndex = 0,
    )

    private fun createViewModel(id: String = routineId.toString()): RoutineRunnerViewModel {
        val vm = RoutineRunnerViewModel(
            application = application,
            routineId = id,
            getRoutineDetailUseCase = getRoutineDetailUseCase,
            startRoutineUseCase = startRoutineUseCase,
            advanceRoutineStepUseCase = advanceRoutineStepUseCase,
            completeRoutineUseCase = completeRoutineUseCase,
            abandonRoutineUseCase = abandonRoutineUseCase,
            nanoClock = testClock,
        )
        createdViewModels.add(vm)
        return vm
    }

    /**
     * Sets up mocks for a standard 2-habit routine and returns the ViewModel.
     * habit1 (Meditate, 60s) at index 0, habit2 (Journal, 120s) at index 1.
     */
    private fun setupTwoHabitRoutine(): Triple<RoutineRunnerViewModel, Habit, Habit> {
        val habit1 = mockHabit(name = "Meditate", estimatedSeconds = 60)
        val habit2 = mockHabit(name = "Journal", estimatedSeconds = 120)
        val rh1 = mockRoutineHabit(habitId = habit1.id, orderIndex = 0)
        val rh2 = mockRoutineHabit(habitId = habit2.id, orderIndex = 1)
        val routine = mockRoutine()

        coEvery { getRoutineDetailUseCase(routineId) } returns Result.Success(
            Pair(routine, listOf(Pair(rh1, habit1), Pair(rh2, habit2)))
        )
        coEvery { startRoutineUseCase(routineId) } returns Result.Success(mockExecution())

        val viewModel = createViewModel()
        return Triple(viewModel, habit1, habit2)
    }

    // -------------------------------------------------------------------------
    // 1. Init loads routine and starts execution
    // -------------------------------------------------------------------------

    @Test
    fun `init loads routine and starts execution`() = runViewModelTest {
        val (viewModel, _, _) = setupTwoHabitRoutine()
        advanceTimeBy(500)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals("Morning Routine", state.routineName)
        assertEquals(0, state.currentStepIndex)
        assertEquals(2, state.totalSteps)
        assertEquals("Meditate", state.currentHabitName)
        assertEquals(60, state.totalTimeSeconds)
        assertEquals("Journal", state.upNextHabitName)
        assertFalse(state.isPaused)
        assertFalse(state.isComplete)
        assertNotNull(state.executionId)
        assertEquals(2, state.stepResults.size)
        assertTrue(state.stepResults.all { it == StepResultType.PENDING })
    }

    // -------------------------------------------------------------------------
    // 2. Init with GetRoutineDetailUseCase failure sets error
    // -------------------------------------------------------------------------

    @Test
    fun `init with detail load failure sets error state`() = runViewModelTest {
        coEvery { getRoutineDetailUseCase(routineId) } returns
            Result.Error("Routine not found")

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        // No timer started on error, so no cleanup needed
    }

    // -------------------------------------------------------------------------
    // 3. Init with StartRoutineUseCase failure sets error
    // -------------------------------------------------------------------------

    @Test
    fun `init with start failure sets error state`() = runViewModelTest {
        val habit = mockHabit(name = "Meditate")
        val rh = mockRoutineHabit(habitId = habit.id)
        val routine = mockRoutine()

        coEvery { getRoutineDetailUseCase(routineId) } returns Result.Success(
            Pair(routine, listOf(Pair(rh, habit)))
        )
        coEvery { startRoutineUseCase(routineId) } returns
            Result.Error("Another execution already active (E-1)")

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        // No timer started on error, so no cleanup needed
    }

    // -------------------------------------------------------------------------
    // 4. onDone advances to next step
    // -------------------------------------------------------------------------

    @Test
    fun `onDone advances to next step`() = runViewModelTest {
        val (viewModel, habit1, _) = setupTwoHabitRoutine()
        advanceTimeBy(500)

        val updatedExecution = mockExecution().copy(currentStepIndex = 1)
        coEvery {
            advanceRoutineStepUseCase(
                executionId = executionId,
                stepResult = StepResult.Completed,
                habitId = habit1.id,
            )
        } returns Result.Success(updatedExecution)

        viewModel.onDone()
        advanceTimeBy(500)

        val state = viewModel.uiState.value
        assertEquals(1, state.currentStepIndex)
        assertEquals("Journal", state.currentHabitName)
        assertEquals(120, state.totalTimeSeconds)
        assertNull(state.upNextHabitName) // Last step has no "up next"
        assertEquals(StepResultType.DONE, state.stepResults[0])
        assertEquals(StepResultType.PENDING, state.stepResults[1])
        assertFalse(state.isComplete)
    }

    // -------------------------------------------------------------------------
    // 5. onSkip advances with SKIPPED result
    // -------------------------------------------------------------------------

    @Test
    fun `onSkip advances to next step with SKIPPED result`() = runViewModelTest {
        val (viewModel, habit1, _) = setupTwoHabitRoutine()
        advanceTimeBy(500)

        val updatedExecution = mockExecution().copy(currentStepIndex = 1)
        coEvery {
            advanceRoutineStepUseCase(
                executionId = executionId,
                stepResult = StepResult.Skipped,
                habitId = habit1.id,
            )
        } returns Result.Success(updatedExecution)

        viewModel.onSkip()
        advanceTimeBy(500)

        val state = viewModel.uiState.value
        assertEquals(1, state.currentStepIndex)
        assertEquals("Journal", state.currentHabitName)
        assertEquals(StepResultType.SKIPPED, state.stepResults[0])
    }

    // -------------------------------------------------------------------------
    // 6. onPause sets isPaused
    // -------------------------------------------------------------------------

    @Test
    fun `onPause sets isPaused to true`() = runViewModelTest {
        val (viewModel, _, _) = setupTwoHabitRoutine()
        advanceTimeBy(500)

        assertFalse(viewModel.uiState.value.isPaused)

        viewModel.onPause()

        assertTrue(viewModel.uiState.value.isPaused)
    }

    // -------------------------------------------------------------------------
    // 7. onResume clears isPaused
    // -------------------------------------------------------------------------

    @Test
    fun `onResume sets isPaused to false`() = runViewModelTest {
        val (viewModel, _, _) = setupTwoHabitRoutine()
        advanceTimeBy(500)

        viewModel.onPause()
        assertTrue(viewModel.uiState.value.isPaused)

        viewModel.onResume()
        assertFalse(viewModel.uiState.value.isPaused)
    }

    // -------------------------------------------------------------------------
    // 8. onDone on last step triggers completion
    // -------------------------------------------------------------------------

    @Test
    fun `onDone on last step triggers completion`() = runViewModelTest {
        val (viewModel, habit1, habit2) = setupTwoHabitRoutine()
        advanceTimeBy(500)

        // Advance past first step
        val afterStep1 = mockExecution().copy(currentStepIndex = 1)
        coEvery {
            advanceRoutineStepUseCase(
                executionId = executionId,
                stepResult = StepResult.Completed,
                habitId = habit1.id,
            )
        } returns Result.Success(afterStep1)

        viewModel.onDone()
        advanceTimeBy(500)

        // Now on last step (index 1) -- complete the routine
        val afterStep2 = mockExecution().copy(currentStepIndex = 2)
        coEvery {
            advanceRoutineStepUseCase(
                executionId = executionId,
                stepResult = StepResult.Completed,
                habitId = habit2.id,
            )
        } returns Result.Success(afterStep2)

        val completedExecution = mockExecution().copy(
            status = ExecutionStatus.Completed,
        )
        coEvery { completeRoutineUseCase(executionId) } returns
            Result.Success(completedExecution)

        viewModel.onDone()
        advanceTimeBy(500)

        val state = viewModel.uiState.value
        assertTrue(state.isComplete)
        assertEquals(StepResultType.DONE, state.stepResults[0])
        assertEquals(StepResultType.DONE, state.stepResults[1])

        coVerify { completeRoutineUseCase(executionId) }
        // Timer is cancelled by the ViewModel on completion, but cancel explicitly too
    }

    // -------------------------------------------------------------------------
    // 9. Mixed done and skip then complete
    // -------------------------------------------------------------------------

    @Test
    fun `mixed done and skip then complete`() = runViewModelTest {
        val (viewModel, habit1, habit2) = setupTwoHabitRoutine()
        advanceTimeBy(500)

        // Skip first step
        val afterStep1 = mockExecution().copy(currentStepIndex = 1)
        coEvery {
            advanceRoutineStepUseCase(
                executionId = executionId,
                stepResult = StepResult.Skipped,
                habitId = habit1.id,
            )
        } returns Result.Success(afterStep1)

        viewModel.onSkip()
        advanceTimeBy(500)

        // Done on second (last) step
        val afterStep2 = mockExecution().copy(currentStepIndex = 2)
        coEvery {
            advanceRoutineStepUseCase(
                executionId = executionId,
                stepResult = StepResult.Completed,
                habitId = habit2.id,
            )
        } returns Result.Success(afterStep2)

        val completedExecution = mockExecution().copy(
            status = ExecutionStatus.Completed,
        )
        coEvery { completeRoutineUseCase(executionId) } returns
            Result.Success(completedExecution)

        viewModel.onDone()
        advanceTimeBy(500)

        val state = viewModel.uiState.value
        assertTrue(state.isComplete)
        assertEquals(StepResultType.SKIPPED, state.stepResults[0])
        assertEquals(StepResultType.DONE, state.stepResults[1])
    }

    // -------------------------------------------------------------------------
    // 10. Advance step error sets error state
    // -------------------------------------------------------------------------

    @Test
    fun `advance step error sets error in state`() = runViewModelTest {
        val (viewModel, habit1, _) = setupTwoHabitRoutine()
        advanceTimeBy(500)

        coEvery {
            advanceRoutineStepUseCase(
                executionId = executionId,
                stepResult = StepResult.Completed,
                habitId = habit1.id,
            )
        } returns Result.Error("Failed to advance")

        viewModel.onDone()
        advanceTimeBy(500)

        assertNotNull(viewModel.uiState.value.error)
        // Should remain on same step
        assertEquals(0, viewModel.uiState.value.currentStepIndex)
    }

    // -------------------------------------------------------------------------
    // 11. onAbandon calls AbandonRoutineUseCase
    // -------------------------------------------------------------------------

    @Test
    fun `onAbandon calls AbandonRoutineUseCase`() = runViewModelTest {
        val (viewModel, _, _) = setupTwoHabitRoutine()
        advanceTimeBy(500)

        coEvery { abandonRoutineUseCase(executionId) } returns
            Result.Success(mockExecution().copy(status = ExecutionStatus.Abandoned))

        viewModel.onAbandon()
        advanceTimeBy(500)

        coVerify { abandonRoutineUseCase(executionId) }
        // onAbandon cancels the timer internally, but cancel explicitly too
    }

    // -------------------------------------------------------------------------
    // 12. Duration override is respected
    // -------------------------------------------------------------------------

    @Test
    fun `duration override is used instead of habit estimatedSeconds`() = runViewModelTest {
        val habit1 = mockHabit(name = "Meditate", estimatedSeconds = 300)
        val habit2 = mockHabit(name = "Journal", estimatedSeconds = 600)
        val rh1 = mockRoutineHabit(
            habitId = habit1.id,
            orderIndex = 0,
            overrideDurationSeconds = 45,
        )
        val rh2 = mockRoutineHabit(habitId = habit2.id, orderIndex = 1)
        val routine = mockRoutine()

        coEvery { getRoutineDetailUseCase(routineId) } returns Result.Success(
            Pair(routine, listOf(Pair(rh1, habit1), Pair(rh2, habit2)))
        )
        coEvery { startRoutineUseCase(routineId) } returns Result.Success(mockExecution())

        val viewModel = createViewModel()
        advanceTimeBy(500)

        // First step should use override (45s), not estimatedSeconds (300s)
        val state = viewModel.uiState.value
        assertEquals(45, state.totalTimeSeconds)
    }

    // -------------------------------------------------------------------------
    // 13. Empty habits list sets error
    // -------------------------------------------------------------------------

    @Test
    fun `init with empty habits list sets error`() = runViewModelTest {
        val routine = mockRoutine()
        coEvery { getRoutineDetailUseCase(routineId) } returns Result.Success(
            Pair(routine, emptyList())
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
    }

    // -------------------------------------------------------------------------
    // 14. Unexpected exception during init sets error
    // -------------------------------------------------------------------------

    @Test
    fun `init with unexpected exception sets error`() = runViewModelTest {
        coEvery { getRoutineDetailUseCase(routineId) } throws
            RuntimeException("Unexpected crash")

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
    }

    // -------------------------------------------------------------------------
    // 15. Pause then resume then done works correctly
    // -------------------------------------------------------------------------

    @Test
    fun `pause then resume then done transitions correctly`() = runViewModelTest {
        val (viewModel, habit1, _) = setupTwoHabitRoutine()
        advanceTimeBy(500)

        // Pause
        viewModel.onPause()
        assertTrue(viewModel.uiState.value.isPaused)
        assertEquals(0, viewModel.uiState.value.currentStepIndex)

        // Resume
        viewModel.onResume()
        assertFalse(viewModel.uiState.value.isPaused)

        // Done should still advance
        val updatedExecution = mockExecution().copy(currentStepIndex = 1)
        coEvery {
            advanceRoutineStepUseCase(
                executionId = executionId,
                stepResult = StepResult.Completed,
                habitId = habit1.id,
            )
        } returns Result.Success(updatedExecution)

        viewModel.onDone()
        advanceTimeBy(500)

        assertEquals(1, viewModel.uiState.value.currentStepIndex)
        assertEquals("Journal", viewModel.uiState.value.currentHabitName)
    }

    // -------------------------------------------------------------------------
    // 16. Notification DONE action calls onDone via RoutineTimerState bridge
    // -------------------------------------------------------------------------

    @Test
    fun `notification DONE action calls onDone`() = runViewModelTest {
        val (viewModel, _, _) = setupTwoHabitRoutine()
        advanceTimeBy(500)

        coEvery {
            advanceRoutineStepUseCase(any(), any(), any())
        } returns Result.Success(mockExecution().copy(currentStepIndex = 1))

        // Simulate notification Done tap
        RoutineTimerState.emitAction(RoutineTimerState.TimerAction.DONE)
        advanceTimeBy(500)

        // ViewModel should have advanced the step
        assertEquals(1, viewModel.uiState.value.currentStepIndex)
    }
}
