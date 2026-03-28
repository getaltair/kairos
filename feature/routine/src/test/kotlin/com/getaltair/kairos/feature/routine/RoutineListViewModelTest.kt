package com.getaltair.kairos.feature.routine

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Routine
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.usecase.GetActiveRoutinesUseCase
import io.mockk.coEvery
import io.mockk.mockk
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoutineListViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val getActiveRoutinesUseCase: GetActiveRoutinesUseCase = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): RoutineListViewModel = RoutineListViewModel(getActiveRoutinesUseCase)

    // -------------------------------------------------------------------------
    // Helper factories
    // -------------------------------------------------------------------------

    private fun mockRoutine(
        name: String = "Morning Routine",
        category: HabitCategory = HabitCategory.Morning,
    ): Routine = Routine(
        name = name,
        category = category,
    )

    // -------------------------------------------------------------------------
    // 1. Init loads active routines successfully
    // -------------------------------------------------------------------------

    @Test
    fun `init loads active routines successfully`() = runTest {
        val routines = listOf(
            mockRoutine(name = "Morning Routine"),
            mockRoutine(name = "Evening Wind Down", category = HabitCategory.Evening),
        )
        coEvery { getActiveRoutinesUseCase() } returns Result.Success(routines)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(2, state.routines.size)
        assertEquals("Morning Routine", state.routines[0].name)
        assertEquals("Evening Wind Down", state.routines[1].name)
    }

    // -------------------------------------------------------------------------
    // 2. Init with empty routines shows empty state
    // -------------------------------------------------------------------------

    @Test
    fun `init with empty routines shows empty state`() = runTest {
        coEvery { getActiveRoutinesUseCase() } returns Result.Success(emptyList())

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertTrue(state.routines.isEmpty())
    }

    // -------------------------------------------------------------------------
    // 3. Init with repository error shows error state
    // -------------------------------------------------------------------------

    @Test
    fun `init with repository error shows error state`() = runTest {
        coEvery { getActiveRoutinesUseCase() } returns Result.Error("Database unavailable")

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertTrue(state.routines.isEmpty())
    }

    // -------------------------------------------------------------------------
    // 4. Init with unexpected exception shows error state
    // -------------------------------------------------------------------------

    @Test
    fun `init with unexpected exception shows error state`() = runTest {
        coEvery { getActiveRoutinesUseCase() } throws RuntimeException("Unexpected crash")

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
    }

    // -------------------------------------------------------------------------
    // 5. Refresh reloads routines
    // -------------------------------------------------------------------------

    @Test
    fun `refresh reloads routines`() = runTest {
        coEvery { getActiveRoutinesUseCase() } returns Result.Success(emptyList())

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.routines.isEmpty())

        val updatedRoutines = listOf(mockRoutine(name = "New Routine"))
        coEvery { getActiveRoutinesUseCase() } returns Result.Success(updatedRoutines)

        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.routines.size)
        assertEquals("New Routine", state.routines[0].name)
    }

    // -------------------------------------------------------------------------
    // 6. Retry resets loading state and reloads
    // -------------------------------------------------------------------------

    @Test
    fun `retryLoad resets error and reloads`() = runTest {
        coEvery { getActiveRoutinesUseCase() } returns Result.Error("Network error")

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.error)

        val routines = listOf(mockRoutine())
        coEvery { getActiveRoutinesUseCase() } returns Result.Success(routines)

        viewModel.retryLoad()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(1, state.routines.size)
    }
}
