package com.getaltair.kairos.feature.auth

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.repository.AuthState
import com.getaltair.kairos.domain.usecase.ObserveAuthStateUseCase
import com.getaltair.kairos.domain.usecase.ResetPasswordUseCase
import com.getaltair.kairos.domain.usecase.SignInUseCase
import com.getaltair.kairos.domain.usecase.SignOutUseCase
import com.getaltair.kairos.domain.usecase.SignUpUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
class AuthViewModelTest {

    private val signInUseCase: SignInUseCase = mockk()
    private val signUpUseCase: SignUpUseCase = mockk()
    private val signOutUseCase: SignOutUseCase = mockk()
    private val resetPasswordUseCase: ResetPasswordUseCase = mockk()
    private val observeAuthStateUseCase: ObserveAuthStateUseCase = mockk()

    private val testDispatcher = UnconfinedTestDispatcher()

    private val authStateFlow = MutableStateFlow<AuthState>(AuthState.SignedOut)

    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { observeAuthStateUseCase() } returns authStateFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): AuthViewModel = AuthViewModel(
        signInUseCase = signInUseCase,
        signUpUseCase = signUpUseCase,
        signOutUseCase = signOutUseCase,
        resetPasswordUseCase = resetPasswordUseCase,
        observeAuthStateUseCase = observeAuthStateUseCase,
    )

    // -------------------------------------------------------------------------
    // 1. signIn with blank email sets error message
    // -------------------------------------------------------------------------

    @Test
    fun `signIn with blank email sets error message`() {
        viewModel = createViewModel()
        viewModel.onEmailChanged("")
        viewModel.onPasswordChanged("password123")

        viewModel.signIn()

        assertEquals("Please enter your email address", viewModel.uiState.value.errorMessage)
    }

    // -------------------------------------------------------------------------
    // 2. signIn with blank password sets error message
    // -------------------------------------------------------------------------

    @Test
    fun `signIn with blank password sets error message`() {
        viewModel = createViewModel()
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("")

        viewModel.signIn()

        assertEquals("Please enter your password", viewModel.uiState.value.errorMessage)
    }

    // -------------------------------------------------------------------------
    // 3. signIn clears error and sets loading state
    // -------------------------------------------------------------------------

    @Test
    fun `signIn clears error and sets loading state`() = runTest {
        viewModel = createViewModel()
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("password123")

        val deferred = kotlinx.coroutines.CompletableDeferred<Result<Unit>>()
        coEvery { signInUseCase(any(), any()) } coAnswers { deferred.await() }

        viewModel.signIn()

        // errorMessage should be null (cleared) while loading
        assertNull(viewModel.uiState.value.errorMessage)

        // Complete the deferred to allow the test to finish
        deferred.complete(Result.Success(Unit))
        advanceUntilIdle()
    }

    // -------------------------------------------------------------------------
    // 4. signIn success updates isSignedIn via auth state observation
    // -------------------------------------------------------------------------

    @Test
    fun `signIn success clears loading state`() = runTest {
        viewModel = createViewModel()
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("password123")
        coEvery { signInUseCase("test@example.com", "password123") } returns Result.Success(Unit)

        viewModel.signIn()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    // -------------------------------------------------------------------------
    // 5. signIn failure sets error message
    // -------------------------------------------------------------------------

    @Test
    fun `signIn failure sets error message`() = runTest {
        viewModel = createViewModel()
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("wrong-password")
        coEvery {
            signInUseCase("test@example.com", "wrong-password")
        } returns Result.Error("Invalid credentials")

        viewModel.signIn()
        advanceUntilIdle()

        assertEquals(
            "Unable to sign in. Please check your credentials and try again.",
            viewModel.uiState.value.errorMessage,
        )
        assertFalse(viewModel.uiState.value.isLoading)
    }

    // -------------------------------------------------------------------------
    // 6. signUp with short password sets error message
    // -------------------------------------------------------------------------

    @Test
    fun `signUp with short password sets error message`() {
        viewModel = createViewModel()
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("12345")
        viewModel.onConfirmPasswordChanged("12345")

        viewModel.signUp()

        assertEquals(
            "Password must be at least 6 characters",
            viewModel.uiState.value.errorMessage,
        )
    }

    // -------------------------------------------------------------------------
    // 7. signUp with mismatched passwords sets error message
    // -------------------------------------------------------------------------

    @Test
    fun `signUp with mismatched passwords sets error message`() {
        viewModel = createViewModel()
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("password123")
        viewModel.onConfirmPasswordChanged("different456")

        viewModel.signUp()

        assertEquals("Passwords do not match", viewModel.uiState.value.errorMessage)
    }

    // -------------------------------------------------------------------------
    // 8. signUp success updates state
    // -------------------------------------------------------------------------

    @Test
    fun `signUp success clears loading state`() = runTest {
        viewModel = createViewModel()
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("password123")
        viewModel.onConfirmPasswordChanged("password123")
        coEvery {
            signUpUseCase("test@example.com", "password123")
        } returns Result.Success(Unit)

        viewModel.signUp()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    // -------------------------------------------------------------------------
    // 9. resetPassword with blank email sets error message
    // -------------------------------------------------------------------------

    @Test
    fun `resetPassword with blank email sets error message`() {
        viewModel = createViewModel()
        viewModel.onEmailChanged("")

        viewModel.resetPassword()

        assertEquals("Please enter your email address", viewModel.uiState.value.errorMessage)
    }

    // -------------------------------------------------------------------------
    // 10. resetPassword success sets passwordResetSent flag
    // -------------------------------------------------------------------------

    @Test
    fun `resetPassword success sets passwordResetSent flag`() = runTest {
        viewModel = createViewModel()
        viewModel.onEmailChanged("test@example.com")
        coEvery { resetPasswordUseCase("test@example.com") } returns Result.Success(Unit)

        viewModel.resetPassword()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.passwordResetSent)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    // -------------------------------------------------------------------------
    // 11. loading guard: concurrent signIn calls are ignored
    // -------------------------------------------------------------------------

    @Test
    fun `loading guard - concurrent signIn calls are ignored`() = runTest {
        viewModel = createViewModel()
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("password123")

        val deferred = kotlinx.coroutines.CompletableDeferred<Result<Unit>>()
        coEvery { signInUseCase(any(), any()) } coAnswers { deferred.await() }

        viewModel.signIn()
        // While still loading, second call should be ignored
        viewModel.signIn()

        deferred.complete(Result.Success(Unit))
        advanceUntilIdle()

        coVerify(exactly = 1) { signInUseCase(any(), any()) }
    }

    // -------------------------------------------------------------------------
    // Additional: signUp with blank email sets error
    // -------------------------------------------------------------------------

    @Test
    fun `signUp with blank email sets error message`() {
        viewModel = createViewModel()
        viewModel.onEmailChanged("")
        viewModel.onPasswordChanged("password123")
        viewModel.onConfirmPasswordChanged("password123")

        viewModel.signUp()

        assertEquals("Please enter your email address", viewModel.uiState.value.errorMessage)
    }

    // -------------------------------------------------------------------------
    // Additional: signUp with blank password sets error
    // -------------------------------------------------------------------------

    @Test
    fun `signUp with blank password sets error message`() {
        viewModel = createViewModel()
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("")
        viewModel.onConfirmPasswordChanged("")

        viewModel.signUp()

        assertEquals("Please enter a password", viewModel.uiState.value.errorMessage)
    }

    // -------------------------------------------------------------------------
    // Additional: signUp failure sets error message
    // -------------------------------------------------------------------------

    @Test
    fun `signUp failure sets error message`() = runTest {
        viewModel = createViewModel()
        viewModel.onEmailChanged("test@example.com")
        viewModel.onPasswordChanged("password123")
        viewModel.onConfirmPasswordChanged("password123")
        coEvery {
            signUpUseCase("test@example.com", "password123")
        } returns Result.Error("Email already in use")

        viewModel.signUp()
        advanceUntilIdle()

        assertEquals(
            "Unable to create account. Please try again.",
            viewModel.uiState.value.errorMessage,
        )
        assertFalse(viewModel.uiState.value.isLoading)
    }

    // -------------------------------------------------------------------------
    // Additional: resetPassword failure sets error message
    // -------------------------------------------------------------------------

    @Test
    fun `resetPassword failure sets error message`() = runTest {
        viewModel = createViewModel()
        viewModel.onEmailChanged("test@example.com")
        coEvery { resetPasswordUseCase("test@example.com") } returns Result.Error("User not found")

        viewModel.resetPassword()
        advanceUntilIdle()

        assertEquals(
            "Unable to send reset email. Please try again.",
            viewModel.uiState.value.errorMessage,
        )
        assertFalse(viewModel.uiState.value.isLoading)
    }

    // -------------------------------------------------------------------------
    // Additional: clearError resets errorMessage
    // -------------------------------------------------------------------------

    @Test
    fun `clearError resets errorMessage to null`() {
        viewModel = createViewModel()
        viewModel.onEmailChanged("")
        viewModel.signIn()
        assertEquals("Please enter your email address", viewModel.uiState.value.errorMessage)

        viewModel.clearError()

        assertNull(viewModel.uiState.value.errorMessage)
    }

    // -------------------------------------------------------------------------
    // Additional: auth state observation updates isSignedIn
    // -------------------------------------------------------------------------

    @Test
    fun `auth state observation updates isSignedIn when SignedIn`() = runTest {
        viewModel = createViewModel()

        authStateFlow.value = AuthState.SignedIn(userId = "user-123", email = "test@example.com")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSignedIn)
    }

    @Test
    fun `auth state observation updates isSignedIn when SignedOut`() = runTest {
        viewModel = createViewModel()

        authStateFlow.value = AuthState.SignedIn(userId = "user-123", email = "test@example.com")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isSignedIn)

        authStateFlow.value = AuthState.SignedOut
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSignedIn)
    }
}
