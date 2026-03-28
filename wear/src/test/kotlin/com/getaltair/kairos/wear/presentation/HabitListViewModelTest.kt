package com.getaltair.kairos.wear.presentation

import com.getaltair.kairos.domain.wear.WearCompletionData
import com.getaltair.kairos.domain.wear.WearHabitData
import com.getaltair.kairos.domain.wear.WearRoutineData
import com.getaltair.kairos.wear.data.WearDataRepository
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HabitListViewModelTest {

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

    @Test
    fun `initial state is loading`() = runTest(testDispatcher) {
        val viewModel = HabitListViewModel(repository)
        val state = viewModel.uiState.value
        assertTrue(state.isLoading)
        assertEquals(emptyMap<String, List<WearHabitData>>(), state.habitsByCategory)
    }

    @Test
    fun `DEPARTURE habits are filtered out`() = runTest(testDispatcher) {
        habitsFlow.value = listOf(
            makeHabit("1", "Morning Run", "MORNING"),
            makeHabit("2", "Pi Dashboard", "DEPARTURE"),
            makeHabit("3", "Read", "EVENING"),
        )
        completionsFlow.value = emptyList()

        val viewModel = HabitListViewModel(repository)
        // Subscribe to activate WhileSubscribed upstream
        val job = backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.totalCount)
        val allHabits = state.habitsByCategory.values.flatten()
        assertTrue(allHabits.none { it.category == "DEPARTURE" })
    }

    @Test
    fun `completedIds are correctly derived from completions`() = runTest(testDispatcher) {
        habitsFlow.value = listOf(
            makeHabit("1", "Morning Run", "MORNING"),
            makeHabit("2", "Meditate", "MORNING"),
        )
        completionsFlow.value = listOf(
            WearCompletionData("c1", "1", "2026-03-28", "FULL", null),
        )

        val viewModel = HabitListViewModel(repository)
        val job = backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(setOf("1"), state.completedIds)
        assertEquals(1, state.completedCount)
        assertEquals(2, state.totalCount)
    }

    @Test
    fun `habits are grouped by category in correct order`() = runTest(testDispatcher) {
        habitsFlow.value = listOf(
            makeHabit("1", "Evening Read", "EVENING"),
            makeHabit("2", "Morning Run", "MORNING"),
            makeHabit("3", "Afternoon Walk", "AFTERNOON"),
            makeHabit("4", "Anytime Journal", "ANYTIME"),
        )
        completionsFlow.value = emptyList()

        val viewModel = HabitListViewModel(repository)
        val job = backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val categories = state.habitsByCategory.keys.toList()
        assertEquals(listOf("MORNING", "AFTERNOON", "EVENING", "ANYTIME"), categories)
    }

    private fun makeHabit(id: String, name: String, category: String) = WearHabitData(
        id = id,
        name = name,
        anchorBehavior = "after-coffee",
        category = category,
        estimatedSeconds = 300,
        icon = null,
        color = null,
    )
}
