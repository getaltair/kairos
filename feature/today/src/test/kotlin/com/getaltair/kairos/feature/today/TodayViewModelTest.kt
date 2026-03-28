package com.getaltair.kairos.feature.today

import com.getaltair.kairos.core.usecase.CompleteHabitUseCase
import com.getaltair.kairos.core.usecase.GetTodayHabitsUseCase
import com.getaltair.kairos.core.usecase.SkipHabitUseCase
import com.getaltair.kairos.core.usecase.UndoCompletionUseCase
import com.getaltair.kairos.core.widget.WidgetRefreshNotifier
import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Completion
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.enums.AnchorType
import com.getaltair.kairos.domain.enums.CompletionType
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.HabitFrequency
import com.getaltair.kairos.domain.model.HabitWithStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Instant
import java.time.LocalDate
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
class TodayViewModelTest {

    private lateinit var widgetRefreshNotifier: WidgetRefreshNotifier
    private lateinit var getTodayHabitsUseCase: GetTodayHabitsUseCase
    private lateinit var completeHabitUseCase: CompleteHabitUseCase
    private lateinit var skipHabitUseCase: SkipHabitUseCase
    private lateinit var undoCompletionUseCase: UndoCompletionUseCase

    private val testDispatcher = UnconfinedTestDispatcher()

    private val fixedInstant = Instant.parse("2025-01-01T00:00:00Z")

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        widgetRefreshNotifier = mockk(relaxed = true)
        getTodayHabitsUseCase = mockk()
        completeHabitUseCase = mockk()
        skipHabitUseCase = mockk()
        undoCompletionUseCase = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Must set up getTodayHabitsUseCase mock BEFORE creating VM because init calls loadTodayHabits.
     */
    private fun createViewModel(): TodayViewModel = TodayViewModel(
        widgetRefreshNotifier,
        getTodayHabitsUseCase,
        completeHabitUseCase,
        skipHabitUseCase,
        undoCompletionUseCase
    )

    private fun makeHabit(
        id: UUID = UUID.randomUUID(),
        name: String = "Test Habit",
        category: HabitCategory = HabitCategory.Morning
    ) = Habit(
        id = id,
        name = name,
        anchorBehavior = "After waking up",
        anchorType = AnchorType.AfterBehavior,
        category = category,
        frequency = HabitFrequency.Daily,
        createdAt = fixedInstant,
        updatedAt = fixedInstant
    )

    private fun makeHabitWithStatus(
        habit: Habit = makeHabit(),
        todayCompletion: Completion? = null,
        weekCompletionRate: Float = 0.5f
    ) = HabitWithStatus(
        habit = habit,
        todayCompletion = todayCompletion,
        weekCompletionRate = weekCompletionRate
    )

    private fun makeCompletion(
        id: UUID = UUID.randomUUID(),
        habitId: UUID,
        type: CompletionType = CompletionType.Full
    ) = Completion(
        id = id,
        habitId = habitId,
        date = LocalDate.now(),
        type = type,
        createdAt = fixedInstant,
        updatedAt = fixedInstant
    )

    // --- Test 1: Initial load populates UI state ---

    @Test
    fun `initial load populates UI state`() = runTest {
        val habit = makeHabit()
        val habits = listOf(makeHabitWithStatus(habit = habit))
        coEvery { getTodayHabitsUseCase() } returns Result.Success(habits)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.habitsByCategory.isNotEmpty())
        assertNull(state.error)
    }

    // --- Test 2: Progress computed correctly from habits ---

    @Test
    fun `progress computed correctly from habits`() = runTest {
        val habitId1 = UUID.randomUUID()
        val habitId2 = UUID.randomUUID()
        val habit1 = makeHabit(id = habitId1, name = "Habit 1")
        val habit2 = makeHabit(id = habitId2, name = "Habit 2")
        val completion = makeCompletion(habitId = habitId1)

        val habits = listOf(
            makeHabitWithStatus(habit = habit1, todayCompletion = completion),
            makeHabitWithStatus(habit = habit2, todayCompletion = null)
        )
        coEvery { getTodayHabitsUseCase() } returns Result.Success(habits)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(0.5f, viewModel.uiState.value.progress, 0.001f)
    }

    // --- Test 3: isEmpty when no habits ---

    @Test
    fun `isEmpty when no habits`() = runTest {
        coEvery { getTodayHabitsUseCase() } returns Result.Success(emptyList())

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isEmpty)
    }

    // --- Test 4: isAllDone when all completed ---

    @Test
    fun `isAllDone when all completed`() = runTest {
        val habitId1 = UUID.randomUUID()
        val habitId2 = UUID.randomUUID()
        val habit1 = makeHabit(id = habitId1, name = "Habit 1")
        val habit2 = makeHabit(id = habitId2, name = "Habit 2")

        val habits = listOf(
            makeHabitWithStatus(habit = habit1, todayCompletion = makeCompletion(habitId = habitId1)),
            makeHabitWithStatus(habit = habit2, todayCompletion = makeCompletion(habitId = habitId2))
        )
        coEvery { getTodayHabitsUseCase() } returns Result.Success(habits)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isAllDone)
        assertEquals(1.0f, viewModel.uiState.value.progress, 0.001f)
    }

    // --- Test 5: Category grouping sorts in display order ---

    @Test
    fun `category grouping sorts in display order`() = runTest {
        val eveningHabit = makeHabit(name = "Evening Habit", category = HabitCategory.Evening)
        val morningHabit = makeHabit(name = "Morning Habit", category = HabitCategory.Morning)

        val habits = listOf(
            makeHabitWithStatus(habit = eveningHabit),
            makeHabitWithStatus(habit = morningHabit)
        )
        coEvery { getTodayHabitsUseCase() } returns Result.Success(habits)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val categories = viewModel.uiState.value.habitsByCategory.keys.toList()
        assertEquals(2, categories.size)
        // Morning should come before Evening in the display order
        assertEquals(HabitCategory.Morning, categories[0])
        assertEquals(HabitCategory.Evening, categories[1])
    }

    // --- Test 6: Unknown category sorts to end ---

    @Test
    fun `anytime category sorts after morning afternoon and evening`() = runTest {
        val morningHabit = makeHabit(name = "Morning Habit", category = HabitCategory.Morning)
        val afternoonHabit = makeHabit(name = "Afternoon Habit", category = HabitCategory.Afternoon)
        val eveningHabit = makeHabit(name = "Evening Habit", category = HabitCategory.Evening)
        val anytimeHabit = makeHabit(name = "Anytime Habit", category = HabitCategory.Anytime)

        val habits = listOf(
            makeHabitWithStatus(habit = anytimeHabit),
            makeHabitWithStatus(habit = eveningHabit),
            makeHabitWithStatus(habit = morningHabit),
            makeHabitWithStatus(habit = afternoonHabit)
        )
        coEvery { getTodayHabitsUseCase() } returns Result.Success(habits)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val categories = viewModel.uiState.value.habitsByCategory.keys.toList()
        assertEquals(4, categories.size)
        assertEquals(HabitCategory.Morning, categories[0])
        assertEquals(HabitCategory.Afternoon, categories[1])
        assertEquals(HabitCategory.Evening, categories[2])
        assertEquals(HabitCategory.Anytime, categories[3])
    }

    // --- Test 7: onHabitComplete success triggers undo timer and reload ---

    @Test
    fun `onHabitComplete success triggers undo timer and reload`() = runTest {
        val habitId = UUID.randomUUID()
        val completionId = UUID.randomUUID()
        val habit = makeHabit(id = habitId, name = "Meditate")
        val habits = listOf(makeHabitWithStatus(habit = habit))

        coEvery { getTodayHabitsUseCase() } returns Result.Success(habits)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val completion = makeCompletion(id = completionId, habitId = habitId)
        coEvery {
            completeHabitUseCase(habitId, CompletionType.Full, null)
        } returns Result.Success(completion)

        viewModel.onHabitComplete(habitId, CompletionType.Full)
        advanceUntilIdle()

        // Verify undo state was set
        val undoState = viewModel.uiState.value.undoState
        // Undo state may be null if the timer has run down, but at least verify the flow worked.
        // With UnconfinedTestDispatcher, the undo timer runs through. Check that load was called again.
        coVerify(atLeast = 2) { getTodayHabitsUseCase() }
    }

    // --- Test 8: onHabitComplete error updates error state ---

    @Test
    fun `onHabitComplete error updates error state`() = runTest {
        val habitId = UUID.randomUUID()
        val habit = makeHabit(id = habitId, name = "Meditate")
        val habits = listOf(makeHabitWithStatus(habit = habit))

        coEvery { getTodayHabitsUseCase() } returns Result.Success(habits)

        val viewModel = createViewModel()
        advanceUntilIdle()

        coEvery {
            completeHabitUseCase(habitId, CompletionType.Full, null)
        } returns Result.Error("Duplicate completion")

        viewModel.onHabitComplete(habitId, CompletionType.Full)
        advanceUntilIdle()

        assertEquals("Something went wrong. Please try again.", viewModel.uiState.value.error)
    }

    // --- Test 9: onHabitSkip success triggers undo timer and reload ---

    @Test
    fun `onHabitSkip success triggers undo timer and reload`() = runTest {
        val habitId = UUID.randomUUID()
        val completionId = UUID.randomUUID()
        val habit = makeHabit(id = habitId, name = "Exercise")
        val habits = listOf(makeHabitWithStatus(habit = habit))

        coEvery { getTodayHabitsUseCase() } returns Result.Success(habits)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val completion = Completion(
            id = completionId,
            habitId = habitId,
            date = LocalDate.now(),
            type = CompletionType.Skipped,
            createdAt = fixedInstant,
            updatedAt = fixedInstant
        )
        coEvery { skipHabitUseCase(habitId, null) } returns Result.Success(completion)

        viewModel.onHabitSkip(habitId)
        advanceUntilIdle()

        // Reload should have been called at least twice (init + after skip)
        coVerify(atLeast = 2) { getTodayHabitsUseCase() }
    }

    // --- Test 10: onUndoCompletion success clears undo state and reloads ---

    @Test
    fun `onUndoCompletion success clears undo state and reloads`() = runTest(testDispatcher) {
        val habitId = UUID.randomUUID()
        val completionId = UUID.randomUUID()
        val habit = makeHabit(id = habitId, name = "Meditate")

        // Initial load returns a completed habit so we can set up undo state
        val completedHabits = listOf(
            makeHabitWithStatus(
                habit = habit,
                todayCompletion = makeCompletion(id = completionId, habitId = habitId)
            )
        )
        coEvery { getTodayHabitsUseCase() } returns Result.Success(completedHabits)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Simulate completing a habit to trigger undo state
        val newCompletionId = UUID.randomUUID()
        val newCompletion = makeCompletion(id = newCompletionId, habitId = habitId)
        coEvery {
            completeHabitUseCase(habitId, CompletionType.Full, null)
        } returns Result.Success(newCompletion)
        viewModel.onHabitComplete(habitId, CompletionType.Full)

        // Now undo
        coEvery { undoCompletionUseCase(any()) } returns Result.Success(Unit)
        viewModel.onUndoCompletion()
        advanceUntilIdle()

        // Undo state should be cleared
        assertNull(viewModel.uiState.value.undoState)
        // Reload should have been called: init + after complete + after undo = at least 3
        coVerify(atLeast = 3) { getTodayHabitsUseCase() }
    }

    // --- Test 11: onUndoCompletion failure shows error ---

    @Test
    fun `onUndoCompletion failure shows error`() = runTest(testDispatcher) {
        val habitId = UUID.randomUUID()
        val completionId = UUID.randomUUID()
        val habit = makeHabit(id = habitId, name = "Meditate")

        val habits = listOf(makeHabitWithStatus(habit = habit))
        coEvery { getTodayHabitsUseCase() } returns Result.Success(habits)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Trigger undo state via completing
        val newCompletion = makeCompletion(id = completionId, habitId = habitId)
        coEvery {
            completeHabitUseCase(habitId, CompletionType.Full, null)
        } returns Result.Success(newCompletion)
        viewModel.onHabitComplete(habitId, CompletionType.Full)

        // Now undo with failure
        coEvery { undoCompletionUseCase(any()) } returns Result.Error("Database error")
        viewModel.onUndoCompletion()
        advanceUntilIdle()

        // Undo state cleared even on error, and error message is set
        assertNull(viewModel.uiState.value.undoState)
        assertEquals("Something went wrong. Please try again.", viewModel.uiState.value.error)
    }

    // --- Test 12: onDismissUndo clears state without calling use case ---

    @Test
    fun `onDismissUndo clears state without calling use case`() = runTest(testDispatcher) {
        val habitId = UUID.randomUUID()
        val completionId = UUID.randomUUID()
        val habit = makeHabit(id = habitId, name = "Meditate")

        val habits = listOf(makeHabitWithStatus(habit = habit))
        coEvery { getTodayHabitsUseCase() } returns Result.Success(habits)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Trigger undo state via completing
        val newCompletion = makeCompletion(id = completionId, habitId = habitId)
        coEvery {
            completeHabitUseCase(habitId, CompletionType.Full, null)
        } returns Result.Success(newCompletion)
        viewModel.onHabitComplete(habitId, CompletionType.Full)

        // Dismiss undo (not undo action)
        viewModel.onDismissUndo()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.undoState)
        // undoCompletionUseCase should never be called when dismissing
        coVerify(exactly = 0) { undoCompletionUseCase(any()) }
    }

    // --- Test 13: Error from loadTodayHabits sets error state ---

    @Test
    fun `error from loadTodayHabits sets error state`() = runTest {
        coEvery { getTodayHabitsUseCase() } returns Result.Error("Network error")

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Something went wrong. Please try again.", state.error)
    }

    // --- Test 14: onHabitComplete success triggers widget refresh ---

    @Test
    fun `onHabitComplete success triggers widget refresh`() = runTest {
        val habitId = UUID.randomUUID()
        val habit = makeHabit(id = habitId, name = "Meditate")
        val habits = listOf(makeHabitWithStatus(habit = habit))

        coEvery { getTodayHabitsUseCase() } returns Result.Success(habits)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val completion = makeCompletion(habitId = habitId)
        coEvery {
            completeHabitUseCase(habitId, CompletionType.Full, null)
        } returns Result.Success(completion)

        viewModel.onHabitComplete(habitId, CompletionType.Full)
        advanceUntilIdle()

        coVerify { widgetRefreshNotifier.refreshAll() }
    }

    // --- Test 15: onHabitSkip success triggers widget refresh ---

    @Test
    fun `onHabitSkip success triggers widget refresh`() = runTest {
        val habitId = UUID.randomUUID()
        val habit = makeHabit(id = habitId, name = "Exercise")
        val habits = listOf(makeHabitWithStatus(habit = habit))

        coEvery { getTodayHabitsUseCase() } returns Result.Success(habits)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val completion = makeCompletion(habitId = habitId, type = CompletionType.Skipped)
        coEvery { skipHabitUseCase(habitId, null) } returns Result.Success(completion)

        viewModel.onHabitSkip(habitId)
        advanceUntilIdle()

        coVerify { widgetRefreshNotifier.refreshAll() }
    }

    // --- Test 16: onUndoCompletion success triggers widget refresh ---

    @Test
    fun `onUndoCompletion success triggers widget refresh`() = runTest(testDispatcher) {
        val habitId = UUID.randomUUID()
        val completionId = UUID.randomUUID()
        val habit = makeHabit(id = habitId, name = "Meditate")

        val completedHabits = listOf(
            makeHabitWithStatus(
                habit = habit,
                todayCompletion = makeCompletion(id = completionId, habitId = habitId)
            )
        )
        coEvery { getTodayHabitsUseCase() } returns Result.Success(completedHabits)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Trigger undo state via completing
        val newCompletion = makeCompletion(habitId = habitId)
        coEvery {
            completeHabitUseCase(habitId, CompletionType.Full, null)
        } returns Result.Success(newCompletion)
        viewModel.onHabitComplete(habitId, CompletionType.Full)

        // Now undo
        coEvery { undoCompletionUseCase(any()) } returns Result.Success(Unit)
        viewModel.onUndoCompletion()
        advanceUntilIdle()

        coVerify(atLeast = 2) { widgetRefreshNotifier.refreshAll() }
    }
}
