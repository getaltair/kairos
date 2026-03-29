package com.getaltair.kairos.feature.routine

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.entity.Routine
import com.getaltair.kairos.domain.entity.RoutineExecution
import com.getaltair.kairos.domain.entity.RoutineHabit
import com.getaltair.kairos.domain.enums.AnchorType
import com.getaltair.kairos.domain.enums.ExecutionStatus
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.HabitFrequency
import com.getaltair.kairos.domain.model.RoutineStep
import com.getaltair.kairos.domain.usecase.GetRoutineDetailUseCase
import com.getaltair.kairos.domain.usecase.GetRoutineExecutionUseCase
import io.mockk.coEvery
import io.mockk.mockk
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoutineSummaryViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var getRoutineExecutionUseCase: GetRoutineExecutionUseCase
    private lateinit var getRoutineDetailUseCase: GetRoutineDetailUseCase

    private val fixedStart = Instant.parse("2025-06-01T07:00:00Z")
    private val fixedEnd = Instant.parse("2025-06-01T07:15:00Z") // 15 minutes later

    private val routineId = UUID.randomUUID()
    private val executionId = UUID.randomUUID()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getRoutineExecutionUseCase = mockk()
        getRoutineDetailUseCase = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Must configure mocks BEFORE creating the ViewModel because init calls loadSummary.
     */
    private fun createViewModel(execId: String = executionId.toString(),): RoutineSummaryViewModel =
        RoutineSummaryViewModel(
            executionId = execId,
            getRoutineExecutionUseCase = getRoutineExecutionUseCase,
            getRoutineDetailUseCase = getRoutineDetailUseCase,
        )

    // -------------------------------------------------------------------------
    // Factory helpers
    // -------------------------------------------------------------------------

    private fun makeRoutine(
        id: UUID = routineId,
        name: String = "Morning Routine",
        category: HabitCategory = HabitCategory.Morning,
    ) = Routine(
        id = id,
        name = name,
        category = category,
    )

    private fun makeHabit(id: UUID = UUID.randomUUID(), name: String = "Test Habit", estimatedSeconds: Int = 300,) =
        Habit(
            id = id,
            name = name,
            anchorBehavior = "After waking up",
            anchorType = AnchorType.AfterBehavior,
            category = HabitCategory.Morning,
            frequency = HabitFrequency.Daily,
            estimatedSeconds = estimatedSeconds,
            createdAt = fixedStart,
            updatedAt = fixedStart,
        )

    private fun makeRoutineHabit(
        routineId: UUID = this.routineId,
        habitId: UUID,
        orderIndex: Int = 0,
        overrideDurationSeconds: Int? = null,
    ) = RoutineHabit(
        routineId = routineId,
        habitId = habitId,
        orderIndex = orderIndex,
        overrideDurationSeconds = overrideDurationSeconds,
        createdAt = fixedStart,
        updatedAt = fixedStart,
    )

    private fun makeExecution(
        id: UUID = executionId,
        routineId: UUID = this.routineId,
        status: ExecutionStatus = ExecutionStatus.Completed,
        currentStepIndex: Int = 0,
        totalPausedSeconds: Int = 0,
        startedAt: Instant = fixedStart,
        completedAt: Instant? = fixedEnd,
    ) = RoutineExecution(
        id = id,
        routineId = routineId,
        startedAt = startedAt,
        completedAt = completedAt,
        status = status,
        currentStepIndex = currentStepIndex,
        totalPausedSeconds = totalPausedSeconds,
        createdAt = fixedStart,
        updatedAt = fixedStart,
    )

    private fun makeStep(habit: Habit, routineHabit: RoutineHabit,) =
        RoutineStep(routineHabit = routineHabit, habit = habit)

    /**
     * Convenience: builds a list of RoutineStep from (name, durationOverride?) pairs.
     */
    private fun buildSteps(vararg specs: Pair<String, Int?>,): List<RoutineStep> =
        specs.mapIndexed { index, (name, override) ->
            val habit = makeHabit(name = name, estimatedSeconds = 300)
            val rh = makeRoutineHabit(
                habitId = habit.id,
                orderIndex = index,
                overrideDurationSeconds = override,
            )
            makeStep(habit, rh)
        }

    // -------------------------------------------------------------------------
    // 1. Successful load populates UI state correctly
    // -------------------------------------------------------------------------

    @Test
    fun `successful load populates UI state with routine data`() = runTest {
        val steps = buildSteps("Meditate" to null, "Stretch" to 120, "Journal" to null)
        val execution = makeExecution(currentStepIndex = 3) // all 3 completed

        coEvery { getRoutineExecutionUseCase(executionId) } returns Result.Success(execution)
        coEvery { getRoutineDetailUseCase(routineId) } returns
            Result.Success(Pair(makeRoutine(), steps))

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals("Morning Routine", state.routineName)
        assertEquals(3, state.habitsCompleted)
        assertEquals(0, state.habitsSkipped)
        assertEquals(3, state.habitResults.size)
    }

    // -------------------------------------------------------------------------
    // 2. Total time computed correctly (completedAt - startedAt - paused)
    // -------------------------------------------------------------------------

    @Test
    fun `total time excludes paused seconds`() = runTest {
        val steps = buildSteps("Meditate" to null)
        // 15 min = 900s total, minus 60s paused = 840s
        val execution = makeExecution(
            currentStepIndex = 1,
            totalPausedSeconds = 60,
        )

        coEvery { getRoutineExecutionUseCase(executionId) } returns Result.Success(execution)
        coEvery { getRoutineDetailUseCase(routineId) } returns
            Result.Success(Pair(makeRoutine(), steps))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(840L, viewModel.uiState.value.totalTimeSeconds)
    }

    // -------------------------------------------------------------------------
    // 3. Total time is zero when execution has no completedAt
    // -------------------------------------------------------------------------

    @Test
    fun `total time is zero when execution not completed`() = runTest {
        val steps = buildSteps("Meditate" to null)
        val execution = makeExecution(
            currentStepIndex = 0,
            completedAt = null,
            status = ExecutionStatus.Abandoned,
        )

        coEvery { getRoutineExecutionUseCase(executionId) } returns Result.Success(execution)
        coEvery { getRoutineDetailUseCase(routineId) } returns
            Result.Success(Pair(makeRoutine(), steps))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(0L, viewModel.uiState.value.totalTimeSeconds)
    }

    // -------------------------------------------------------------------------
    // 4. Partially completed routine marks skipped steps
    // -------------------------------------------------------------------------

    @Test
    fun `partially completed routine calculates skipped steps`() = runTest {
        val steps = buildSteps("Meditate" to null, "Stretch" to null, "Journal" to null)
        // Only completed first 2 of 3 steps
        val execution = makeExecution(currentStepIndex = 2)

        coEvery { getRoutineExecutionUseCase(executionId) } returns Result.Success(execution)
        coEvery { getRoutineDetailUseCase(routineId) } returns
            Result.Success(Pair(makeRoutine(), steps))

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.habitsCompleted)
        assertEquals(1, state.habitsSkipped)
        // First two marked wasCompleted, last is not
        assertTrue(state.habitResults[0].wasCompleted)
        assertTrue(state.habitResults[1].wasCompleted)
        assertFalse(state.habitResults[2].wasCompleted)
    }

    // -------------------------------------------------------------------------
    // 5. HabitSummaryItem uses override duration when present
    // -------------------------------------------------------------------------

    @Test
    fun `habit summary uses override duration when present`() = runTest {
        val steps = buildSteps("Meditate" to 120, "Stretch" to null)
        val execution = makeExecution(currentStepIndex = 2)

        coEvery { getRoutineExecutionUseCase(executionId) } returns Result.Success(execution)
        coEvery { getRoutineDetailUseCase(routineId) } returns
            Result.Success(Pair(makeRoutine(), steps))

        val viewModel = createViewModel()
        advanceUntilIdle()

        val results = viewModel.uiState.value.habitResults
        assertEquals(120, results[0].durationSeconds) // overridden
        assertEquals(300, results[1].durationSeconds) // default estimatedSeconds
    }

    // -------------------------------------------------------------------------
    // 6. Execution load error sets error state
    // -------------------------------------------------------------------------

    @Test
    fun `execution load error sets error state`() = runTest {
        coEvery {
            getRoutineExecutionUseCase(executionId)
        } returns Result.Error("Execution not found")

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Something went wrong loading the summary.", state.error)
    }

    // -------------------------------------------------------------------------
    // 7. Routine detail load error sets error state
    // -------------------------------------------------------------------------

    @Test
    fun `routine detail load error sets error state`() = runTest {
        val execution = makeExecution(currentStepIndex = 1)
        coEvery { getRoutineExecutionUseCase(executionId) } returns Result.Success(execution)
        coEvery { getRoutineDetailUseCase(routineId) } returns Result.Error("Routine missing")

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Something went wrong loading routine details.", state.error)
    }

    // -------------------------------------------------------------------------
    // 8. Invalid execution ID shows unexpected error
    // -------------------------------------------------------------------------

    @Test
    fun `invalid execution ID shows unexpected error`() = runTest {
        val viewModel = createViewModel(execId = "not-a-uuid")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("An unexpected error occurred", state.error)
    }

    // -------------------------------------------------------------------------
    // 9. Initial state starts as loading
    // -------------------------------------------------------------------------

    @Test
    fun `initial uiState starts with isLoading true`() {
        // Verify the default RoutineSummaryUiState has isLoading = true
        val defaultState = RoutineSummaryUiState()
        assertTrue(defaultState.isLoading)
        assertEquals("", defaultState.routineName)
        assertEquals(0, defaultState.habitsCompleted)
        assertEquals(0, defaultState.habitsSkipped)
        assertTrue(defaultState.habitResults.isEmpty())
        assertNull(defaultState.error)
    }

    // -------------------------------------------------------------------------
    // 10. Habit names are propagated to summary items
    // -------------------------------------------------------------------------

    @Test
    fun `habit names are propagated to summary items`() = runTest {
        val steps = buildSteps("Meditate" to null, "Cold Shower" to null)
        val execution = makeExecution(currentStepIndex = 2)

        coEvery { getRoutineExecutionUseCase(executionId) } returns Result.Success(execution)
        coEvery { getRoutineDetailUseCase(routineId) } returns
            Result.Success(Pair(makeRoutine(), steps))

        val viewModel = createViewModel()
        advanceUntilIdle()

        val names = viewModel.uiState.value.habitResults.map { it.habitName }
        assertEquals(listOf("Meditate", "Cold Shower"), names)
    }

    // -------------------------------------------------------------------------
    // 11. Total time is non-negative even with large paused seconds
    // -------------------------------------------------------------------------

    @Test
    fun `total time is clamped to zero when paused exceeds duration`() = runTest {
        val steps = buildSteps("Meditate" to null)
        // 900s total time, 1000s paused -- should clamp to 0
        val execution = makeExecution(
            currentStepIndex = 1,
            totalPausedSeconds = 1000,
        )

        coEvery { getRoutineExecutionUseCase(executionId) } returns Result.Success(execution)
        coEvery { getRoutineDetailUseCase(routineId) } returns
            Result.Success(Pair(makeRoutine(), steps))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(0L, viewModel.uiState.value.totalTimeSeconds)
    }

    // -------------------------------------------------------------------------
    // 12. Empty routine (zero steps) loads successfully
    // -------------------------------------------------------------------------

    @Test
    fun `empty routine with zero steps loads successfully`() = runTest {
        val steps = emptyList<RoutineStep>()
        val execution = makeExecution(currentStepIndex = 0)

        coEvery { getRoutineExecutionUseCase(executionId) } returns Result.Success(execution)
        coEvery { getRoutineDetailUseCase(routineId) } returns
            Result.Success(Pair(makeRoutine(), steps))

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(0, state.habitsCompleted)
        assertEquals(0, state.habitsSkipped)
        assertTrue(state.habitResults.isEmpty())
    }
}
