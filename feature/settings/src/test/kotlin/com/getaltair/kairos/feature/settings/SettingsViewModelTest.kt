package com.getaltair.kairos.feature.settings

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.repository.AuthState
import com.getaltair.kairos.domain.sync.SyncState
import com.getaltair.kairos.domain.sync.SyncStateProvider
import com.getaltair.kairos.domain.usecase.DeleteAccountUseCase
import com.getaltair.kairos.domain.usecase.ObserveAuthStateUseCase
import com.getaltair.kairos.domain.usecase.SignOutUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
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
class SettingsViewModelTest {

    private val syncStateProvider: SyncStateProvider = mockk()
    private val observeAuthStateUseCase: ObserveAuthStateUseCase = mockk()
    private val signOutUseCase: SignOutUseCase = mockk()
    private val deleteAccountUseCase: DeleteAccountUseCase = mockk()

    private val testDispatcher = UnconfinedTestDispatcher()

    private val syncStateFlow = MutableStateFlow<SyncState>(SyncState.NotSignedIn)
    private val authStateFlow = MutableStateFlow<AuthState>(AuthState.SignedOut)

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { syncStateProvider.syncState } returns syncStateFlow
        every { observeAuthStateUseCase() } returns authStateFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): SettingsViewModel = SettingsViewModel(
        syncStateProvider = syncStateProvider,
        observeAuthStateUseCase = observeAuthStateUseCase,
        signOutUseCase = signOutUseCase,
        deleteAccountUseCase = deleteAccountUseCase,
    )

    // -------------------------------------------------------------------------
    // 1. Initial state
    // -------------------------------------------------------------------------

    @Test
    fun `initial state is default SettingsUiState`() {
        viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertEquals(SyncState.NotSignedIn, state.syncState)
        assertNull(state.userEmail)
        assertFalse(state.isSignedIn)
        assertNull(state.lastSyncTime)
        assertFalse(state.showSignOutDialog)
        assertEquals(DeletionState.Idle, state.deletionState)
        assertNull(state.errorMessage)
    }

    // -------------------------------------------------------------------------
    // 2. observeAuthState updates isSignedIn and userEmail when SignedIn
    // -------------------------------------------------------------------------

    @Test
    fun `observeAuthState updates isSignedIn and userEmail when SignedIn`() = runTest {
        viewModel = createViewModel()

        authStateFlow.value = AuthState.SignedIn(
            userId = "user-123",
            email = "test@example.com",
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isSignedIn)
        assertEquals("test@example.com", state.userEmail)
    }

    // -------------------------------------------------------------------------
    // 3. observeAuthState clears isSignedIn and userEmail when SignedOut
    // -------------------------------------------------------------------------

    @Test
    fun `observeAuthState clears isSignedIn and userEmail when SignedOut`() = runTest {
        viewModel = createViewModel()

        // First sign in
        authStateFlow.value = AuthState.SignedIn(
            userId = "user-123",
            email = "test@example.com",
        )
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isSignedIn)

        // Then sign out
        authStateFlow.value = AuthState.SignedOut
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSignedIn)
        assertNull(state.userEmail)
    }

    // -------------------------------------------------------------------------
    // 4. observeSyncState updates syncState
    // -------------------------------------------------------------------------

    @Test
    fun `observeSyncState updates syncState`() = runTest {
        viewModel = createViewModel()

        syncStateFlow.value = SyncState.Synced
        advanceUntilIdle()

        assertEquals(SyncState.Synced, viewModel.uiState.value.syncState)
    }

    // -------------------------------------------------------------------------
    // 5. signOut calls signOutUseCase and logs success
    // -------------------------------------------------------------------------

    @Test
    fun `signOut calls signOutUseCase and has no errorMessage on success`() = runTest {
        viewModel = createViewModel()
        coEvery { signOutUseCase() } returns Result.Success(Unit)

        viewModel.signOut()
        advanceUntilIdle()

        coVerify(exactly = 1) { signOutUseCase() }
        assertNull(viewModel.uiState.value.errorMessage)
    }

    // -------------------------------------------------------------------------
    // 6. signOut sets errorMessage on failure
    // -------------------------------------------------------------------------

    @Test
    fun `signOut sets errorMessage on failure`() = runTest {
        viewModel = createViewModel()
        coEvery { signOutUseCase() } returns Result.Error("Sign out failed")

        viewModel.signOut()
        advanceUntilIdle()

        assertEquals("Unable to sign out. Please try again.", viewModel.uiState.value.errorMessage)
    }

    // -------------------------------------------------------------------------
    // 7. onDeleteAccountConfirm shows re-auth dialog
    // -------------------------------------------------------------------------

    @Test
    fun `onDeleteAccountConfirm shows reauth dialog and hides delete dialog`() {
        viewModel = createViewModel()

        // First show the delete dialog
        viewModel.onDeleteAccountRequest()
        assertEquals(DeletionState.ConfirmDialog, viewModel.uiState.value.deletionState)

        // Confirm deletion, which should transition to reauth dialog
        viewModel.onDeleteAccountConfirm()

        assertEquals(DeletionState.ReauthDialog, viewModel.uiState.value.deletionState)
    }

    // -------------------------------------------------------------------------
    // 8. clearError resets errorMessage to null
    // -------------------------------------------------------------------------

    @Test
    fun `clearError resets errorMessage to null`() = runTest {
        viewModel = createViewModel()
        coEvery { signOutUseCase() } returns Result.Error("Sign out failed")

        // Trigger an error via signOut failure
        viewModel.signOut()
        advanceUntilIdle()
        assertEquals("Unable to sign out. Please try again.", viewModel.uiState.value.errorMessage)

        // Clear the error
        viewModel.clearError()

        assertNull(viewModel.uiState.value.errorMessage)
    }

    // -------------------------------------------------------------------------
    // 9. onSignOutRequest sets showSignOutDialog to true
    // -------------------------------------------------------------------------

    @Test
    fun `onSignOutRequest sets showSignOutDialog to true`() {
        viewModel = createViewModel()

        viewModel.onSignOutRequest()

        assertTrue(viewModel.uiState.value.showSignOutDialog)
    }

    // -------------------------------------------------------------------------
    // 10. onSignOutDismiss sets showSignOutDialog to false
    // -------------------------------------------------------------------------

    @Test
    fun `onSignOutDismiss sets showSignOutDialog to false`() {
        viewModel = createViewModel()

        viewModel.onSignOutRequest()
        assertTrue(viewModel.uiState.value.showSignOutDialog)

        viewModel.onSignOutDismiss()

        assertFalse(viewModel.uiState.value.showSignOutDialog)
    }

    // -------------------------------------------------------------------------
    // 11. onReauthDismiss sets showReauthDialog to false
    // -------------------------------------------------------------------------

    @Test
    fun `onReauthDismiss resets deletionState to Idle`() {
        viewModel = createViewModel()

        // Open the reauth dialog via the normal flow
        viewModel.onDeleteAccountRequest()
        viewModel.onDeleteAccountConfirm()
        assertEquals(DeletionState.ReauthDialog, viewModel.uiState.value.deletionState)

        // Dismiss it
        viewModel.onReauthDismiss()

        assertEquals(DeletionState.Idle, viewModel.uiState.value.deletionState)
    }

    // -------------------------------------------------------------------------
    // 12. deleteAccount success sets accountDeleted to true
    // -------------------------------------------------------------------------

    @Test
    fun `deleteAccount success sets deletionState to Deleted`() = runTest {
        viewModel = createViewModel()
        coEvery { deleteAccountUseCase(any()) } returns Result.Success(Unit)

        viewModel.deleteAccount("password123")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(DeletionState.Deleted, state.deletionState)
        assertNull(state.errorMessage)
    }

    // -------------------------------------------------------------------------
    // 13. deleteAccount failure sets errorMessage and isDeletingAccount false
    // -------------------------------------------------------------------------

    @Test
    fun `deleteAccount failure sets deletionState to Failed with error message`() = runTest {
        viewModel = createViewModel()
        coEvery { deleteAccountUseCase(any()) } returns Result.Error("Incorrect password")

        viewModel.deleteAccount("wrong-password")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(DeletionState.Failed("Incorrect password"), state.deletionState)
        assertEquals("Incorrect password", state.errorMessage)
    }

    // -------------------------------------------------------------------------
    // 14. deleteAccount sets isDeletingAccount true while in progress
    // -------------------------------------------------------------------------

    @Test
    fun `deleteAccount sets deletionState to Deleting while in progress`() = runTest {
        // Use StandardTestDispatcher so coroutine does not complete eagerly
        val standardDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(standardDispatcher)

        val deferred = CompletableDeferred<Result<Unit>>()
        coEvery { deleteAccountUseCase(any()) } coAnswers { deferred.await() }

        viewModel = SettingsViewModel(
            syncStateProvider = syncStateProvider,
            observeAuthStateUseCase = observeAuthStateUseCase,
            signOutUseCase = signOutUseCase,
            deleteAccountUseCase = deleteAccountUseCase,
        )

        // Advance past init coroutines
        standardDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteAccount("password123")

        // Advance enough for the launch to start but the deferred is still pending
        standardDispatcher.scheduler.runCurrent()

        // While in progress, deletionState should be Deleting
        assertEquals(DeletionState.Deleting, viewModel.uiState.value.deletionState)

        // Complete the use case
        deferred.complete(Result.Success(Unit))
        standardDispatcher.scheduler.advanceUntilIdle()

        // After completion, deletionState should be Deleted
        assertEquals(DeletionState.Deleted, viewModel.uiState.value.deletionState)
    }

    // -------------------------------------------------------------------------
    // 15. onAccountDeletedConsumed resets deletionState to Idle
    // -------------------------------------------------------------------------

    @Test
    fun `onAccountDeletedConsumed resets deletionState to Idle`() = runTest {
        viewModel = createViewModel()
        coEvery { deleteAccountUseCase(any()) } returns Result.Success(Unit)

        viewModel.deleteAccount("password123")
        advanceUntilIdle()
        assertEquals(DeletionState.Deleted, viewModel.uiState.value.deletionState)

        viewModel.onAccountDeletedConsumed()
        assertEquals(DeletionState.Idle, viewModel.uiState.value.deletionState)
    }
}
