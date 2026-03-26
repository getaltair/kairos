package com.getaltair.kairos.feature.habit

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Completion
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.enums.AnchorType
import com.getaltair.kairos.domain.enums.CompletionType
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.HabitFrequency
import com.getaltair.kairos.domain.enums.HabitStatus
import com.getaltair.kairos.domain.model.HabitDetail
import com.getaltair.kairos.domain.usecase.ArchiveHabitUseCase
import com.getaltair.kairos.domain.usecase.BackdateCompletionUseCase
import com.getaltair.kairos.domain.usecase.DeleteHabitUseCase
import com.getaltair.kairos.domain.usecase.GetHabitDetailUseCase
import com.getaltair.kairos.domain.usecase.PauseHabitUseCase
import com.getaltair.kairos.domain.usecase.RestoreHabitUseCase
import com.getaltair.kairos.domain.usecase.ResumeHabitUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HabitDetailViewModelTest {

    private val getHabitDetailUseCase: GetHabitDetailUseCase = mockk()
    private val pauseHabitUseCase: PauseHabitUseCase = mockk()
    private val resumeHabitUseCase: ResumeHabitUseCase = mockk()
    private val archiveHabitUseCase: ArchiveHabitUseCase = mockk()
    private val restoreHabitUseCase: RestoreHabitUseCase = mockk()
    private val deleteHabitUseCase: DeleteHabitUseCase = mockk()
    private val backdateCompletionUseCase: BackdateCompletionUseCase = mockk()

    private lateinit var viewModel: HabitDetailViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    private val testHabitId = UUID.randomUUID()
    private val testHabit = Habit(
        id = testHabitId,
        name = "Morning Meditation",
        anchorBehavior = "After brushing teeth",
        anchorType = AnchorType.AfterBehavior,
        category = HabitCategory.Morning,
        frequency = HabitFrequency.Daily,
        status = HabitStatus.Active
    )
    private val testCompletions = listOf(
        Completion(
            habitId = testHabitId,
            date = LocalDate.now(),
            type = CompletionType.Full
        )
    )
    private val testDetail = HabitDetail(
        habit = testHabit,
        recentCompletions = testCompletions,
        weeklyCompletionRate = 0.71f
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = HabitDetailViewModel(
            getHabitDetailUseCase,
            pauseHabitUseCase,
            resumeHabitUseCase,
            archiveHabitUseCase,
            restoreHabitUseCase,
            deleteHabitUseCase,
            backdateCompletionUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -------------------------------------------------------------------------
    // 1. Initial state
    // -------------------------------------------------------------------------

    @Test
    fun `initial state - isLoading is true`() {
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `initial state - habit is null`() {
        assertNull(viewModel.uiState.value.habit)
    }

    // -------------------------------------------------------------------------
    // 2. loadHabit success
    // -------------------------------------------------------------------------

    @Test
    fun `loadHabit success populates state`() = runTest {
        coEvery { getHabitDetailUseCase(testHabitId) } returns Result.Success(testDetail)

        viewModel.loadHabit(testHabitId)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertNotNull(state.habit)
        assertEquals("Morning Meditation", state.habit?.name)
        assertEquals(1, state.recentCompletions.size)
        assertEquals(0.71f, state.weeklyCompletionRate)
    }

    // -------------------------------------------------------------------------
    // 3. loadHabit error
    // -------------------------------------------------------------------------

    @Test
    fun `loadHabit error sets error state`() = runTest {
        coEvery { getHabitDetailUseCase(testHabitId) } returns Result.Error("Not found")

        viewModel.loadHabit(testHabitId)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertNull(state.habit)
    }

    // -------------------------------------------------------------------------
    // 4. Pause action
    // -------------------------------------------------------------------------

    @Test
    fun `onPause calls pauseHabitUseCase and reloads`() = runTest {
        coEvery { getHabitDetailUseCase(testHabitId) } returns Result.Success(testDetail)
        val pausedHabit = testHabit.copy(status = HabitStatus.Paused)
        coEvery { pauseHabitUseCase(testHabitId) } returns Result.Success(pausedHabit)

        viewModel.loadHabit(testHabitId)
        advanceUntilIdle()
        viewModel.onPause()
        advanceUntilIdle()

        coVerify { pauseHabitUseCase(testHabitId) }
        // Should reload after pause
        coVerify(atLeast = 2) { getHabitDetailUseCase(testHabitId) }
    }

    // -------------------------------------------------------------------------
    // 5. Resume action
    // -------------------------------------------------------------------------

    @Test
    fun `onResume calls resumeHabitUseCase and reloads`() = runTest {
        val pausedHabit = testHabit.copy(status = HabitStatus.Paused)
        val pausedDetail = testDetail.copy(habit = pausedHabit)
        coEvery { getHabitDetailUseCase(testHabitId) } returns Result.Success(pausedDetail)
        val resumedHabit = testHabit.copy(status = HabitStatus.Active)
        coEvery { resumeHabitUseCase(testHabitId) } returns Result.Success(resumedHabit)

        viewModel.loadHabit(testHabitId)
        advanceUntilIdle()
        viewModel.onResume()
        advanceUntilIdle()

        coVerify { resumeHabitUseCase(testHabitId) }
        coVerify(atLeast = 2) { getHabitDetailUseCase(testHabitId) }
    }

    // -------------------------------------------------------------------------
    // 6. Archive action
    // -------------------------------------------------------------------------

    @Test
    fun `onArchive calls archiveHabitUseCase and reloads`() = runTest {
        coEvery { getHabitDetailUseCase(testHabitId) } returns Result.Success(testDetail)
        val archivedHabit = testHabit.copy(status = HabitStatus.Archived)
        coEvery { archiveHabitUseCase(testHabitId) } returns Result.Success(archivedHabit)

        viewModel.loadHabit(testHabitId)
        advanceUntilIdle()
        viewModel.onArchive()
        advanceUntilIdle()

        coVerify { archiveHabitUseCase(testHabitId) }
        coVerify(atLeast = 2) { getHabitDetailUseCase(testHabitId) }
    }

    // -------------------------------------------------------------------------
    // 7. Restore action
    // -------------------------------------------------------------------------

    @Test
    fun `onRestore calls restoreHabitUseCase and reloads`() = runTest {
        val archivedHabit = testHabit.copy(status = HabitStatus.Archived)
        val archivedDetail = testDetail.copy(habit = archivedHabit)
        coEvery { getHabitDetailUseCase(testHabitId) } returns Result.Success(archivedDetail)
        val restoredHabit = testHabit.copy(status = HabitStatus.Active)
        coEvery { restoreHabitUseCase(testHabitId) } returns Result.Success(restoredHabit)

        viewModel.loadHabit(testHabitId)
        advanceUntilIdle()
        viewModel.onRestore()
        advanceUntilIdle()

        coVerify { restoreHabitUseCase(testHabitId) }
        coVerify(atLeast = 2) { getHabitDetailUseCase(testHabitId) }
    }

    // -------------------------------------------------------------------------
    // 7b. Action error paths
    // -------------------------------------------------------------------------

    @Test
    fun `onPause error sets actionResult`() = runTest {
        coEvery { getHabitDetailUseCase(testHabitId) } returns Result.Success(testDetail)
        coEvery { pauseHabitUseCase(testHabitId) } returns Result.Error("Failed to pause")

        viewModel.loadHabit(testHabitId)
        advanceUntilIdle()
        viewModel.onPause()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.actionResult)
    }

    @Test
    fun `onResume error sets actionResult`() = runTest {
        coEvery { getHabitDetailUseCase(testHabitId) } returns Result.Success(testDetail)
        coEvery { resumeHabitUseCase(testHabitId) } returns Result.Error("Failed to resume")

        viewModel.loadHabit(testHabitId)
        advanceUntilIdle()
        viewModel.onResume()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.actionResult)
    }

    @Test
    fun `onArchive error sets actionResult`() = runTest {
        coEvery { getHabitDetailUseCase(testHabitId) } returns Result.Success(testDetail)
        coEvery { archiveHabitUseCase(testHabitId) } returns Result.Error("Failed to archive")

        viewModel.loadHabit(testHabitId)
        advanceUntilIdle()
        viewModel.onArchive()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.actionResult)
    }

    @Test
    fun `onRestore error sets actionResult`() = runTest {
        coEvery { getHabitDetailUseCase(testHabitId) } returns Result.Success(testDetail)
        coEvery { restoreHabitUseCase(testHabitId) } returns Result.Error("Failed to restore")

        viewModel.loadHabit(testHabitId)
        advanceUntilIdle()
        viewModel.onRestore()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.actionResult)
    }

    // -------------------------------------------------------------------------
    // 8. Delete flow
    // -------------------------------------------------------------------------

    @Test
    fun `onDeleteRequested shows confirmation dialog`() = runTest {
        coEvery { getHabitDetailUseCase(testHabitId) } returns Result.Success(testDetail)

        viewModel.loadHabit(testHabitId)
        advanceUntilIdle()
        viewModel.onDeleteRequested()

        assertTrue(viewModel.uiState.value.showDeleteConfirmation)
    }

    @Test
    fun `onDismissDeleteDialog hides confirmation dialog`() {
        viewModel.onDeleteRequested()
        assertTrue(viewModel.uiState.value.showDeleteConfirmation)

        viewModel.onDismissDeleteDialog()

        assertFalse(viewModel.uiState.value.showDeleteConfirmation)
    }

    @Test
    fun `onDeleteConfirmed calls deleteHabitUseCase and sets isDeleted`() = runTest {
        coEvery { getHabitDetailUseCase(testHabitId) } returns Result.Success(testDetail)
        coEvery { deleteHabitUseCase(testHabitId) } returns Result.Success(Unit)

        viewModel.loadHabit(testHabitId)
        advanceUntilIdle()
        viewModel.onDeleteRequested()
        viewModel.onDeleteConfirmed()
        advanceUntilIdle()

        coVerify { deleteHabitUseCase(testHabitId) }
        assertTrue(viewModel.uiState.value.isDeleted)
    }

    @Test
    fun `onDeleteConfirmed failure sets actionResult error`() = runTest {
        coEvery { getHabitDetailUseCase(testHabitId) } returns Result.Success(testDetail)
        coEvery { deleteHabitUseCase(testHabitId) } returns Result.Error("Failed to delete habit")

        viewModel.loadHabit(testHabitId)
        advanceUntilIdle()
        viewModel.onDeleteRequested()
        viewModel.onDeleteConfirmed()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.actionResult)
    }

    // -------------------------------------------------------------------------
    // 9. Backdate completion
    // -------------------------------------------------------------------------

    @Test
    fun `onBackdate success reloads habit and sets action result`() = runTest {
        coEvery { getHabitDetailUseCase(testHabitId) } returns Result.Success(testDetail)
        val completion = Completion(
            habitId = testHabitId,
            date = LocalDate.now().minusDays(1),
            type = CompletionType.Full
        )
        coEvery {
            backdateCompletionUseCase(testHabitId, any(), any(), any())
        } returns Result.Success(completion)

        viewModel.loadHabit(testHabitId)
        advanceUntilIdle()
        viewModel.onBackdate(LocalDate.now().minusDays(1), CompletionType.Full)
        advanceUntilIdle()

        coVerify { backdateCompletionUseCase(testHabitId, any(), any(), any()) }
        assertEquals("Completion added", viewModel.uiState.value.actionResult)
    }

    @Test
    fun `onBackdate failure sets actionResult error`() = runTest {
        coEvery { getHabitDetailUseCase(testHabitId) } returns Result.Success(testDetail)
        coEvery {
            backdateCompletionUseCase(testHabitId, any(), any(), any())
        } returns Result.Error("A completion already exists for this habit on 2026-03-25")

        viewModel.loadHabit(testHabitId)
        advanceUntilIdle()
        viewModel.onBackdate(LocalDate.now().minusDays(1), CompletionType.Full)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.actionResult)
    }

    // -------------------------------------------------------------------------
    // 10. Error handling
    // -------------------------------------------------------------------------

    @Test
    fun `action error when no habit loaded does not crash`() = runTest {
        // onPause before loadHabit -- habitId is null, should no-op
        viewModel.onPause()
        advanceUntilIdle()

        coVerify(exactly = 0) { pauseHabitUseCase(any()) }
    }

    @Test
    fun `clearActionResult clears action result`() {
        // Manually trigger an action result scenario by checking it gets cleared
        viewModel.clearActionResult()
        assertNull(viewModel.uiState.value.actionResult)
    }
}
