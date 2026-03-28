package com.getaltair.kairos.wear.presentation

import com.getaltair.kairos.domain.wear.WearCompletionData
import com.getaltair.kairos.domain.wear.WearHabitData
import com.getaltair.kairos.domain.wear.WearRoutineData
import com.getaltair.kairos.wear.data.WearDataRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoutineRunnerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val habitsFlow = MutableStateFlow<List<WearHabitData>>(emptyList())
    private val completionsFlow = MutableStateFlow<List<WearCompletionData>>(emptyList())
    private val routineFlow = MutableStateFlow<WearRoutineData?>(null)
    private val repository = mockk<WearDataRepository>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { repository.todayHabits } returns habitsFlow
        every { repository.todayCompletions } returns completionsFlow
        every { repository.activeRoutine } returns routineFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun makeRoutine(routineId: String = "routine-1", executionId: String = "exec-1",) = WearRoutineData(
        routineId = routineId,
        executionId = executionId,
        name = "Morning Routine",
        steps = listOf("Step 1", "Step 2"),
        currentStepIndex = 0,
        status = "RUNNING",
        remainingSeconds = 60,
    )

    // ------------------------------------------------------------------
    // onDone
    // ------------------------------------------------------------------

    @Test
    fun `onDone calls repository advanceRoutineStep with executionId`() = runTest(testDispatcher) {
        routineFlow.value = makeRoutine()

        val viewModel = RoutineRunnerViewModel("routine-1", repository)
        advanceUntilIdle()

        viewModel.onDone()
        advanceUntilIdle()

        coVerify { repository.advanceRoutineStep("exec-1") }
    }

    @Test
    fun `onDone returns early when executionId is null`() = runTest(testDispatcher) {
        // No routine emitted, so currentExecutionId stays null
        val viewModel = RoutineRunnerViewModel("routine-1", repository)
        advanceUntilIdle()

        viewModel.onDone()
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.advanceRoutineStep(any()) }
    }

    // ------------------------------------------------------------------
    // onSkip
    // ------------------------------------------------------------------

    @Test
    fun `onSkip calls repository skipRoutineStep with executionId`() = runTest(testDispatcher) {
        routineFlow.value = makeRoutine()

        val viewModel = RoutineRunnerViewModel("routine-1", repository)
        advanceUntilIdle()

        viewModel.onSkip()
        advanceUntilIdle()

        coVerify { repository.skipRoutineStep("exec-1") }
    }

    @Test
    fun `onSkip returns early when executionId is null`() = runTest(testDispatcher) {
        // No routine emitted, so currentExecutionId stays null
        val viewModel = RoutineRunnerViewModel("routine-1", repository)
        advanceUntilIdle()

        viewModel.onSkip()
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.skipRoutineStep(any()) }
    }

    // ------------------------------------------------------------------
    // onPause
    // ------------------------------------------------------------------

    @Test
    fun `onPause calls repository pauseRoutine with executionId`() = runTest(testDispatcher) {
        routineFlow.value = makeRoutine()

        val viewModel = RoutineRunnerViewModel("routine-1", repository)
        advanceUntilIdle()

        viewModel.onPause()
        advanceUntilIdle()

        coVerify { repository.pauseRoutine("exec-1") }
    }
}
