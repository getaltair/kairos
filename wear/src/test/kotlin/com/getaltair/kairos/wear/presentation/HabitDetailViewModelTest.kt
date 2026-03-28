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
}
