package com.getaltair.kairos.feature.routine

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.entity.Routine
import com.getaltair.kairos.domain.enums.AnchorType
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.HabitFrequency
import com.getaltair.kairos.domain.usecase.CreateRoutineUseCase
import com.getaltair.kairos.domain.usecase.GetActiveHabitsUseCase
import com.getaltair.kairos.domain.usecase.GetRoutineDetailUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoutineBuilderViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val getRoutineDetailUseCase: GetRoutineDetailUseCase = mockk()
    private val getActiveHabitsUseCase: GetActiveHabitsUseCase = mockk()
    private val createRoutineUseCase: CreateRoutineUseCase = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Creates a ViewModel in create mode (routineId = null).
     * Sets up getActiveHabitsUseCase to return the provided habits.
     */
    private fun createViewModel(habits: List<Habit> = emptyList(),): RoutineBuilderViewModel {
        coEvery { getActiveHabitsUseCase() } returns Result.Success(habits)
        return RoutineBuilderViewModel(
            routineId = null,
            getRoutineDetailUseCase = getRoutineDetailUseCase,
            getActiveHabitsUseCase = getActiveHabitsUseCase,
            createRoutineUseCase = createRoutineUseCase,
        )
    }

    // -------------------------------------------------------------------------
    // Helper factories
    // -------------------------------------------------------------------------

    private fun mockHabit(name: String = "Test Habit", id: UUID = UUID.randomUUID(),): Habit = Habit(
        id = id,
        name = name,
        anchorBehavior = "After waking up",
        anchorType = AnchorType.AfterBehavior,
        category = HabitCategory.Morning,
        frequency = HabitFrequency.Daily,
        estimatedSeconds = 300,
    )

    // -------------------------------------------------------------------------
    // 1. Init loads available habits
    // -------------------------------------------------------------------------

    @Test
    fun `init in create mode loads available habits`() = runTest {
        val habits = listOf(mockHabit(name = "Meditate"), mockHabit(name = "Journal"))
        val viewModel = createViewModel(habits)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertFalse(state.isEditMode)
        assertEquals(2, state.availableHabits.size)
        assertEquals("Meditate", state.availableHabits[0].name)
    }

    // -------------------------------------------------------------------------
    // 2. Update name
    // -------------------------------------------------------------------------

    @Test
    fun `updateName updates name in state`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateName("Morning Routine")

        assertEquals("Morning Routine", viewModel.uiState.value.name)
    }

    @Test
    fun `updateName clears previous error`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Trigger a validation error by saving with blank name
        viewModel.save()
        assertNotNull(viewModel.uiState.value.error)

        viewModel.updateName("Valid Name")
        assertNull(viewModel.uiState.value.error)
    }

    // -------------------------------------------------------------------------
    // 3. Update category
    // -------------------------------------------------------------------------

    @Test
    fun `updateCategory updates category in state`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(HabitCategory.Morning, viewModel.uiState.value.category)

        viewModel.updateCategory(HabitCategory.Evening)

        assertEquals(HabitCategory.Evening, viewModel.uiState.value.category)
    }

    // -------------------------------------------------------------------------
    // 4. Add habit
    // -------------------------------------------------------------------------

    @Test
    fun `addHabit adds to selectedHabits`() = runTest {
        val habit = mockHabit(name = "Meditate")
        val viewModel = createViewModel(listOf(habit))
        advanceUntilIdle()

        viewModel.addHabit(habit)

        val selected = viewModel.uiState.value.selectedHabits
        assertEquals(1, selected.size)
        assertEquals("Meditate", selected[0].first.name)
        assertNull(selected[0].second) // No duration override
    }

    @Test
    fun `addHabit same habit twice is idempotent`() = runTest {
        val habit = mockHabit(name = "Meditate")
        val viewModel = createViewModel(listOf(habit))
        advanceUntilIdle()

        viewModel.addHabit(habit)
        viewModel.addHabit(habit)

        assertEquals(1, viewModel.uiState.value.selectedHabits.size)
    }

    @Test
    fun `addHabit clears previous error`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Trigger an error
        viewModel.save()
        assertNotNull(viewModel.uiState.value.error)

        val habit = mockHabit()
        viewModel.addHabit(habit)
        assertNull(viewModel.uiState.value.error)
    }

    // -------------------------------------------------------------------------
    // 5. Remove habit
    // -------------------------------------------------------------------------

    @Test
    fun `removeHabit removes from selectedHabits`() = runTest {
        val habit1 = mockHabit(name = "Meditate")
        val habit2 = mockHabit(name = "Journal")
        val viewModel = createViewModel(listOf(habit1, habit2))
        advanceUntilIdle()

        viewModel.addHabit(habit1)
        viewModel.addHabit(habit2)
        assertEquals(2, viewModel.uiState.value.selectedHabits.size)

        viewModel.removeHabit(habit1.id)

        val selected = viewModel.uiState.value.selectedHabits
        assertEquals(1, selected.size)
        assertEquals("Journal", selected[0].first.name)
    }

    @Test
    fun `removeHabit with non-existent id does nothing`() = runTest {
        val habit = mockHabit(name = "Meditate")
        val viewModel = createViewModel(listOf(habit))
        advanceUntilIdle()

        viewModel.addHabit(habit)
        viewModel.removeHabit(UUID.randomUUID())

        assertEquals(1, viewModel.uiState.value.selectedHabits.size)
    }

    // -------------------------------------------------------------------------
    // 6. Reorder habits
    // -------------------------------------------------------------------------

    @Test
    fun `reorderHabits swaps items correctly`() = runTest {
        val habit1 = mockHabit(name = "Meditate")
        val habit2 = mockHabit(name = "Journal")
        val habit3 = mockHabit(name = "Exercise")
        val viewModel = createViewModel(listOf(habit1, habit2, habit3))
        advanceUntilIdle()

        viewModel.addHabit(habit1)
        viewModel.addHabit(habit2)
        viewModel.addHabit(habit3)

        // Move first item to last position
        viewModel.reorderHabits(0, 2)

        val selected = viewModel.uiState.value.selectedHabits
        assertEquals("Journal", selected[0].first.name)
        assertEquals("Exercise", selected[1].first.name)
        assertEquals("Meditate", selected[2].first.name)
    }

    @Test
    fun `reorderHabits with out-of-bounds indices does nothing`() = runTest {
        val habit1 = mockHabit(name = "Meditate")
        val habit2 = mockHabit(name = "Journal")
        val viewModel = createViewModel(listOf(habit1, habit2))
        advanceUntilIdle()

        viewModel.addHabit(habit1)
        viewModel.addHabit(habit2)

        // Out-of-bounds: should be a no-op
        viewModel.reorderHabits(-1, 5)

        val selected = viewModel.uiState.value.selectedHabits
        assertEquals("Meditate", selected[0].first.name)
        assertEquals("Journal", selected[1].first.name)
    }

    // -------------------------------------------------------------------------
    // 7. Set duration override
    // -------------------------------------------------------------------------

    @Test
    fun `setDurationOverride updates duration for specific habit`() = runTest {
        val habit = mockHabit(name = "Meditate")
        val viewModel = createViewModel(listOf(habit))
        advanceUntilIdle()

        viewModel.addHabit(habit)
        viewModel.setDurationOverride(habit.id, 120)

        val selected = viewModel.uiState.value.selectedHabits
        assertEquals(120, selected[0].second)
    }

    @Test
    fun `setDurationOverride with null clears override`() = runTest {
        val habit = mockHabit(name = "Meditate")
        val viewModel = createViewModel(listOf(habit))
        advanceUntilIdle()

        viewModel.addHabit(habit)
        viewModel.setDurationOverride(habit.id, 120)
        assertEquals(120, viewModel.uiState.value.selectedHabits[0].second)

        viewModel.setDurationOverride(habit.id, null)
        assertNull(viewModel.uiState.value.selectedHabits[0].second)
    }

    // -------------------------------------------------------------------------
    // 8. Save validation - blank name
    // -------------------------------------------------------------------------

    @Test
    fun `save with blank name sets error`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.save()

        val state = viewModel.uiState.value
        assertNotNull(state.error)
        assertFalse(state.isSaved)
    }

    // -------------------------------------------------------------------------
    // 9. Save validation - less than 2 habits (R-1)
    // -------------------------------------------------------------------------

    @Test
    fun `save with less than 2 habits sets error`() = runTest {
        val habit = mockHabit(name = "Meditate")
        val viewModel = createViewModel(listOf(habit))
        advanceUntilIdle()

        viewModel.updateName("Morning Routine")
        viewModel.addHabit(habit)
        viewModel.save()

        val state = viewModel.uiState.value
        assertNotNull(state.error)
        assertFalse(state.isSaved)
    }

    // -------------------------------------------------------------------------
    // 10. Save with valid routine calls CreateRoutineUseCase
    // -------------------------------------------------------------------------

    @Test
    fun `save with valid routine calls CreateRoutineUseCase`() = runTest {
        val habit1 = mockHabit(name = "Meditate")
        val habit2 = mockHabit(name = "Journal")
        val viewModel = createViewModel(listOf(habit1, habit2))
        advanceUntilIdle()

        val savedRoutine = Routine(name = "Morning Routine", category = HabitCategory.Morning)
        coEvery {
            createRoutineUseCase(
                name = any(),
                category = any(),
                habitIds = any(),
                durations = any(),
            )
        } returns Result.Success(savedRoutine)

        viewModel.updateName("Morning Routine")
        viewModel.addHabit(habit1)
        viewModel.addHabit(habit2)
        viewModel.save()
        advanceUntilIdle()

        coVerify {
            createRoutineUseCase(
                name = "Morning Routine",
                category = HabitCategory.Morning,
                habitIds = listOf(habit1.id, habit2.id),
                durations = any(),
            )
        }
    }

    // -------------------------------------------------------------------------
    // 11. Save success sets isSaved
    // -------------------------------------------------------------------------

    @Test
    fun `save success sets isSaved to true`() = runTest {
        val habit1 = mockHabit(name = "Meditate")
        val habit2 = mockHabit(name = "Journal")
        val viewModel = createViewModel(listOf(habit1, habit2))
        advanceUntilIdle()

        val savedRoutine = Routine(name = "Morning Routine", category = HabitCategory.Morning)
        coEvery {
            createRoutineUseCase(
                name = any(),
                category = any(),
                habitIds = any(),
                durations = any(),
            )
        } returns Result.Success(savedRoutine)

        viewModel.updateName("Morning Routine")
        viewModel.addHabit(habit1)
        viewModel.addHabit(habit2)
        viewModel.save()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isSaved)
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertNotNull(state.savedRoutineId)
    }

    // -------------------------------------------------------------------------
    // 12. Save failure sets error message
    // -------------------------------------------------------------------------

    @Test
    fun `save failure sets error message`() = runTest {
        val habit1 = mockHabit(name = "Meditate")
        val habit2 = mockHabit(name = "Journal")
        val viewModel = createViewModel(listOf(habit1, habit2))
        advanceUntilIdle()

        coEvery {
            createRoutineUseCase(
                name = any(),
                category = any(),
                habitIds = any(),
                durations = any(),
            )
        } returns Result.Error("Validation failed")

        viewModel.updateName("Morning Routine")
        viewModel.addHabit(habit1)
        viewModel.addHabit(habit2)
        viewModel.save()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSaved)
        assertFalse(state.isLoading)
        assertNotNull(state.error)
    }

    // -------------------------------------------------------------------------
    // 13. Save with unexpected exception sets error
    // -------------------------------------------------------------------------

    @Test
    fun `save with unexpected exception sets error`() = runTest {
        val habit1 = mockHabit(name = "Meditate")
        val habit2 = mockHabit(name = "Journal")
        val viewModel = createViewModel(listOf(habit1, habit2))
        advanceUntilIdle()

        coEvery {
            createRoutineUseCase(
                name = any(),
                category = any(),
                habitIds = any(),
                durations = any(),
            )
        } throws RuntimeException("Network error")

        viewModel.updateName("Morning Routine")
        viewModel.addHabit(habit1)
        viewModel.addHabit(habit2)
        viewModel.save()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSaved)
        assertNotNull(state.error)
    }

    // -------------------------------------------------------------------------
    // 14. Clear error
    // -------------------------------------------------------------------------

    @Test
    fun `clearError clears error state`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Trigger error
        viewModel.save()
        assertNotNull(viewModel.uiState.value.error)

        viewModel.clearError()
        assertNull(viewModel.uiState.value.error)
    }

    // -------------------------------------------------------------------------
    // 15. Init failure loading habits shows error
    // -------------------------------------------------------------------------

    @Test
    fun `init with habit loading failure shows error`() = runTest {
        coEvery { getActiveHabitsUseCase() } returns Result.Error("Failed to load")
        val viewModel = RoutineBuilderViewModel(
            routineId = null,
            getRoutineDetailUseCase = getRoutineDetailUseCase,
            getActiveHabitsUseCase = getActiveHabitsUseCase,
            createRoutineUseCase = createRoutineUseCase,
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
    }

    // -------------------------------------------------------------------------
    // 16. Save passes duration overrides correctly
    // -------------------------------------------------------------------------

    @Test
    fun `save passes duration overrides to use case`() = runTest {
        val habit1 = mockHabit(name = "Meditate")
        val habit2 = mockHabit(name = "Journal")
        val viewModel = createViewModel(listOf(habit1, habit2))
        advanceUntilIdle()

        val savedRoutine = Routine(name = "Morning Routine", category = HabitCategory.Morning)
        coEvery {
            createRoutineUseCase(
                name = any(),
                category = any(),
                habitIds = any(),
                durations = any(),
            )
        } returns Result.Success(savedRoutine)

        viewModel.updateName("Morning Routine")
        viewModel.addHabit(habit1)
        viewModel.addHabit(habit2)
        viewModel.setDurationOverride(habit1.id, 60)
        viewModel.save()
        advanceUntilIdle()

        coVerify {
            createRoutineUseCase(
                name = "Morning Routine",
                category = HabitCategory.Morning,
                habitIds = listOf(habit1.id, habit2.id),
                durations = mapOf(habit1.id to 60),
            )
        }
    }
}
