package com.getaltair.kairos.feature.recovery

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.entity.RecoverySession
import com.getaltair.kairos.domain.enums.AnchorType
import com.getaltair.kairos.domain.enums.Blocker
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.HabitFrequency
import com.getaltair.kairos.domain.enums.RecoveryAction
import com.getaltair.kairos.domain.enums.RecoveryType
import com.getaltair.kairos.domain.enums.SessionStatus
import com.getaltair.kairos.domain.usecase.CompleteRecoverySessionUseCase
import com.getaltair.kairos.domain.usecase.GetPendingRecoveriesUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
class RecoverySessionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getPendingRecoveries = mockk<GetPendingRecoveriesUseCase>()
    private val completeRecovery = mockk<CompleteRecoverySessionUseCase>()

    private val habitId = UUID.randomUUID()
    private val sessionId = UUID.randomUUID()

    private val testHabit = Habit(
        id = habitId,
        name = "Test Habit",
        icon = "🧪",
        anchorBehavior = "After waking up",
        anchorType = AnchorType.AfterBehavior,
        category = HabitCategory.Morning,
        frequency = HabitFrequency.Daily,
        microVersion = "Tiny test"
    )

    private val testSession = RecoverySession(
        id = sessionId,
        habitId = habitId,
        type = RecoveryType.Lapse,
        status = SessionStatus.Pending,
        blockers = listOf(Blocker.NoEnergy)
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        id: String = habitId.toString(),
        pendingResult: Result<List<Pair<RecoverySession, Habit>>> =
            Result.Success(listOf(testSession to testHabit))
    ): RecoverySessionViewModel {
        coEvery { getPendingRecoveries() } returns pendingResult
        return RecoverySessionViewModel(id, getPendingRecoveries, completeRecovery)
    }

    // -----------------------------------------------------------------------
    // Loading
    // -----------------------------------------------------------------------

    @Test
    fun `init loads matching session and habit`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(testHabit, state.habit)
        assertEquals(testSession, state.session)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `init sets error when no matching session found`() = runTest {
        val otherId = UUID.randomUUID()
        val vm = createViewModel(id = otherId.toString())
        advanceUntilIdle()

        val state = vm.uiState.value
        assertNull(state.habit)
        assertNotNull(state.error)
    }

    @Test
    fun `init sets error on use case failure`() = runTest {
        val vm = createViewModel(
            pendingResult = Result.Error("db error")
        )
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.error)
    }

    // -----------------------------------------------------------------------
    // Step navigation
    // -----------------------------------------------------------------------

    @Test
    fun `proceedFromWelcome advances to BlockerSelection`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.proceedFromWelcome()
        assertEquals(RecoveryStep.BlockerSelection, vm.uiState.value.currentStep)
    }

    @Test
    fun `skipBlockers advances to ActionSelection`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.proceedFromWelcome()
        vm.skipBlockers()
        assertEquals(RecoveryStep.ActionSelection, vm.uiState.value.currentStep)
    }

    @Test
    fun `proceedFromBlockers advances to ActionSelection`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.proceedFromWelcome()
        vm.proceedFromBlockers()
        assertEquals(RecoveryStep.ActionSelection, vm.uiState.value.currentStep)
    }

    @Test
    fun `goBack retreats steps correctly`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        // Welcome -> BlockerSelection
        vm.proceedFromWelcome()
        assertEquals(RecoveryStep.BlockerSelection, vm.uiState.value.currentStep)

        // BlockerSelection -> ActionSelection
        vm.proceedFromBlockers()
        assertEquals(RecoveryStep.ActionSelection, vm.uiState.value.currentStep)

        // ActionSelection -> BlockerSelection
        vm.goBack()
        assertEquals(RecoveryStep.BlockerSelection, vm.uiState.value.currentStep)

        // BlockerSelection -> Welcome
        vm.goBack()
        assertEquals(RecoveryStep.Welcome, vm.uiState.value.currentStep)

        // Welcome -> Welcome (no-op)
        vm.goBack()
        assertEquals(RecoveryStep.Welcome, vm.uiState.value.currentStep)
    }

    @Test
    fun `goBack from Confirmation clears chosen action`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.proceedFromWelcome()
        vm.proceedFromBlockers()
        vm.chooseAction(RecoveryAction.Resume)
        assertEquals(RecoveryStep.Confirmation, vm.uiState.value.currentStep)

        vm.goBack()
        assertEquals(RecoveryStep.ActionSelection, vm.uiState.value.currentStep)
        assertNull(vm.uiState.value.chosenAction)
        assertNull(vm.uiState.value.confirmationMessage)
    }

    // -----------------------------------------------------------------------
    // Blockers
    // -----------------------------------------------------------------------

    @Test
    fun `toggleBlocker adds and removes blockers`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.toggleBlocker(Blocker.TooBusy)
        assertTrue(Blocker.TooBusy in vm.uiState.value.selectedBlockers)

        vm.toggleBlocker(Blocker.Sick)
        assertEquals(2, vm.uiState.value.selectedBlockers.size)

        vm.toggleBlocker(Blocker.TooBusy)
        assertFalse(Blocker.TooBusy in vm.uiState.value.selectedBlockers)
        assertEquals(1, vm.uiState.value.selectedBlockers.size)
    }

    // -----------------------------------------------------------------------
    // Action selection & confirmation messages
    // -----------------------------------------------------------------------

    @Test
    fun `chooseAction Resume sets correct message and step`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.chooseAction(RecoveryAction.Resume)
        assertEquals(RecoveryStep.Confirmation, vm.uiState.value.currentStep)
        assertEquals(RecoveryAction.Resume, vm.uiState.value.chosenAction)
        assertEquals("You're back. Let's keep going.", vm.uiState.value.confirmationMessage)
    }

    @Test
    fun `chooseAction Simplify sets correct message`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.chooseAction(RecoveryAction.Simplify)
        assertEquals("Starting smaller is still starting.", vm.uiState.value.confirmationMessage)
    }

    @Test
    fun `chooseAction Pause sets correct message`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.chooseAction(RecoveryAction.Pause)
        assertEquals("Taking a break is a valid choice.", vm.uiState.value.confirmationMessage)
    }

    @Test
    fun `chooseAction Archive sets correct message`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.chooseAction(RecoveryAction.Archive)
        assertEquals(
            "This chapter is complete. New ones await.",
            vm.uiState.value.confirmationMessage
        )
    }

    @Test
    fun `chooseAction FreshStart sets correct message`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.chooseAction(RecoveryAction.FreshStart)
        assertEquals("Every moment is a new beginning.", vm.uiState.value.confirmationMessage)
    }

    // -----------------------------------------------------------------------
    // Confirm action
    // -----------------------------------------------------------------------

    @Test
    fun `confirmAction calls use case and sets isComplete`() = runTest {
        coEvery { completeRecovery(sessionId, RecoveryAction.Resume) } returns
            Result.Success(testSession.copy(status = SessionStatus.Completed, action = RecoveryAction.Resume))

        val vm = createViewModel()
        advanceUntilIdle()

        vm.chooseAction(RecoveryAction.Resume)
        vm.confirmAction()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isComplete)
        coVerify(exactly = 1) { completeRecovery(sessionId, RecoveryAction.Resume) }
    }

    @Test
    fun `confirmAction sets error on failure`() = runTest {
        coEvery { completeRecovery(sessionId, RecoveryAction.Pause) } returns
            Result.Error("db failure")

        val vm = createViewModel()
        advanceUntilIdle()

        vm.chooseAction(RecoveryAction.Pause)
        vm.confirmAction()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isComplete)
        assertNotNull(vm.uiState.value.error)
    }

    // -----------------------------------------------------------------------
    // Feature availability helpers
    // -----------------------------------------------------------------------

    @Test
    fun `isFreshStartAvailable returns true for Relapse session`() = runTest {
        val relapseSession = testSession.copy(
            status = SessionStatus.Pending,
            type = RecoveryType.Relapse
        )
        val vm = createViewModel(
            pendingResult = Result.Success(listOf(relapseSession to testHabit))
        )
        advanceUntilIdle()

        assertTrue(vm.isFreshStartAvailable())
    }

    @Test
    fun `isFreshStartAvailable returns false for Lapse session`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        assertFalse(vm.isFreshStartAvailable())
    }

    @Test
    fun `isSimplifyEnabled returns true when habit has microVersion`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        assertTrue(vm.isSimplifyEnabled())
    }

    @Test
    fun `isSimplifyEnabled returns false when habit has no microVersion`() = runTest {
        val noMicroHabit = testHabit.copy(microVersion = null)
        val vm = createViewModel(
            pendingResult = Result.Success(listOf(testSession to noMicroHabit))
        )
        advanceUntilIdle()

        assertFalse(vm.isSimplifyEnabled())
    }

    // -----------------------------------------------------------------------
    // Confirmation messages do not contain forbidden words
    // -----------------------------------------------------------------------

    @Test
    fun `confirmation messages contain no forbidden words`() {
        val forbidden = listOf("streak", "broke", "failed", "failure", "try harder", "give up", "should have")
        RecoveryAction::class.sealedSubclasses.forEach { subclass ->
            val action = subclass.objectInstance ?: return@forEach
            val message = RecoverySessionViewModel.confirmationMessageFor(action)
            forbidden.forEach { word ->
                assertFalse(
                    "Message for ${action.displayName} contains forbidden word '$word': $message",
                    message.lowercase().contains(word)
                )
            }
        }
    }
}
