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
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HabitDetailViewModelTest {

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

    private fun makeHabit(id: String, name: String = "Test Habit") = WearHabitData(
        id = id,
        name = name,
        anchorBehavior = "after-coffee",
        category = "MORNING",
        estimatedSeconds = 300,
        icon = null,
        color = null,
    )

    // ------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------

    @Test
    fun `completeHabit calls repository with correct params`() = runTest(testDispatcher) {
        val viewModel = HabitDetailViewModel("habit-1", repository)
        viewModel.completeHabit("FULL")
        advanceUntilIdle()

        coVerify { repository.completeHabit("habit-1", "FULL", null) }
    }

    @Test
    fun `completeHabit with partial calls repository with percent`() = runTest(testDispatcher) {
        val viewModel = HabitDetailViewModel("habit-1", repository)
        viewModel.completeHabit("PARTIAL", 50)
        advanceUntilIdle()

        coVerify { repository.completeHabit("habit-1", "PARTIAL", 50) }
    }

    @Test
    fun `skipHabit calls repository skipHabit`() = runTest(testDispatcher) {
        val viewModel = HabitDetailViewModel("habit-1", repository)
        viewModel.skipHabit("not today")
        advanceUntilIdle()

        coVerify { repository.skipHabit("habit-1", "not today") }
    }

    @Test
    fun `skipHabit with no reason calls repository with null`() = runTest(testDispatcher) {
        val viewModel = HabitDetailViewModel("habit-1", repository)
        viewModel.skipHabit()
        advanceUntilIdle()

        coVerify { repository.skipHabit("habit-1", null) }
    }

    // ------------------------------------------------------------------
    // uiState
    // ------------------------------------------------------------------

    @Test
    fun `uiState emits loading initially`() = runTest(testDispatcher) {
        val viewModel = HabitDetailViewModel("habit-1", repository)
        val state = viewModel.uiState.value
        assertTrue("Initial state should be loading", state.isLoading)
    }

    @Test
    fun `uiState finds habit by id when habits flow emits`() = runTest(testDispatcher) {
        habitsFlow.value = listOf(
            makeHabit("habit-1", "Morning Run"),
            makeHabit("habit-2", "Meditate"),
        )

        val viewModel = HabitDetailViewModel("habit-1", repository)
        val job = backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Morning Run", state.habit?.name)
        assertEquals("habit-1", state.habit?.id)
        assertEquals(false, state.isLoading)

        job.cancel()
    }

    @Test
    fun `uiState returns null habit when id not found`() = runTest(testDispatcher) {
        habitsFlow.value = listOf(
            makeHabit("habit-2", "Meditate"),
            makeHabit("habit-3", "Read"),
        )

        val viewModel = HabitDetailViewModel("habit-999", repository)
        val job = backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull("Habit should be null when id not found", state.habit)
        assertEquals(false, state.isLoading)

        job.cancel()
    }
}
