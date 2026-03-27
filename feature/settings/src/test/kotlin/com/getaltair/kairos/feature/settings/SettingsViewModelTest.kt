package com.getaltair.kairos.feature.settings

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.repository.AuthState
import com.getaltair.kairos.domain.sync.SyncState
import com.getaltair.kairos.domain.sync.SyncStateProvider
import com.getaltair.kairos.domain.usecase.ObserveAuthStateUseCase
import com.getaltair.kairos.domain.usecase.SignOutUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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
        assertFalse(state.showDeleteAccountDialog)
        assertFalse(state.showSignOutDialog)
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
    // 7. onDeleteAccountConfirm sets not-available errorMessage
    // -------------------------------------------------------------------------

    @Test
    fun `onDeleteAccountConfirm sets not-available errorMessage`() {
        viewModel = createViewModel()

        viewModel.onDeleteAccountConfirm()

        assertEquals(
            "Account deletion is not yet available.",
            viewModel.uiState.value.errorMessage,
        )
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
}
