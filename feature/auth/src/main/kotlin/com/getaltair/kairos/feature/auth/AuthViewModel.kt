package com.getaltair.kairos.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.repository.AuthState
import com.getaltair.kairos.domain.usecase.ObserveAuthStateUseCase
import com.getaltair.kairos.domain.usecase.ResetPasswordUseCase
import com.getaltair.kairos.domain.usecase.SignInUseCase
import com.getaltair.kairos.domain.usecase.SignOutUseCase
import com.getaltair.kairos.domain.usecase.SignUpUseCase
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class AuthViewModel(
    private val signInUseCase: SignInUseCase,
    private val signUpUseCase: SignUpUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeAuthStateUseCase().collect { authState ->
                _uiState.update {
                    it.copy(isSignedIn = authState is AuthState.SignedIn)
                }
            }
        }
    }

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, errorMessage = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun onConfirmPasswordChanged(password: String) {
        _uiState.update { it.copy(confirmPassword = password, errorMessage = null) }
    }

    fun signIn() {
        val state = _uiState.value
        if (state.isLoading) return

        if (state.email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your email address") }
            return
        }
        if (state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your password") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                when (val result = signInUseCase(state.email.trim(), state.password)) {
                    is Result.Success -> {
                        _uiState.update { it.copy(isLoading = false) }
                    }

                    is Result.Error -> {
                        Timber.e(result.cause, "Sign in failed: %s", result.message)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Unable to sign in. Please check your credentials and try again."
                            )
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error during sign in")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Something went wrong. Please try again."
                    )
                }
            }
        }
    }

    fun signUp() {
        val state = _uiState.value
        if (state.isLoading) return

        if (state.email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your email address") }
            return
        }
        if (state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter a password") }
            return
        }
        if (state.password.length < 6) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters") }
            return
        }
        if (state.password != state.confirmPassword) {
            _uiState.update { it.copy(errorMessage = "Passwords do not match") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                when (val result = signUpUseCase(state.email.trim(), state.password)) {
                    is Result.Success -> {
                        _uiState.update { it.copy(isLoading = false) }
                    }

                    is Result.Error -> {
                        Timber.e(result.cause, "Sign up failed: %s", result.message)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Unable to create account. Please try again."
                            )
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error during sign up")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Something went wrong. Please try again."
                    )
                }
            }
        }
    }

    fun resetPassword() {
        val state = _uiState.value
        if (state.isLoading) return

        if (state.email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your email address") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                when (val result = resetPasswordUseCase(state.email.trim())) {
                    is Result.Success -> {
                        _uiState.update {
                            it.copy(isLoading = false, passwordResetSent = true)
                        }
                    }

                    is Result.Error -> {
                        Timber.e(result.cause, "Password reset failed: %s", result.message)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Unable to send reset email. Please try again."
                            )
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error during password reset")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Something went wrong. Please try again."
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
