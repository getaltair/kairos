package com.getaltair.kairos.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.repository.AuthState
import com.getaltair.kairos.domain.sync.SyncStateProvider
import com.getaltair.kairos.domain.usecase.DeleteAccountUseCase
import com.getaltair.kairos.domain.usecase.ObserveAuthStateUseCase
import com.getaltair.kairos.domain.usecase.SignOutUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class SettingsViewModel(
    private val syncStateProvider: SyncStateProvider,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observeSyncState()
        observeAuthState()
    }

    private fun observeSyncState() {
        viewModelScope.launch {
            syncStateProvider.syncState.collect { syncState ->
                _uiState.update { it.copy(syncState = syncState) }
            }
        }
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            observeAuthStateUseCase().collect { authState ->
                _uiState.update {
                    when (authState) {
                        is AuthState.SignedIn -> it.copy(
                            isSignedIn = true,
                            userEmail = authState.email,
                        )

                        is AuthState.SignedOut -> it.copy(
                            isSignedIn = false,
                            userEmail = null,
                        )
                    }
                }
            }
        }
    }

    fun onSignOutRequest() {
        _uiState.update { it.copy(showSignOutDialog = true) }
    }

    fun onSignOutDismiss() {
        _uiState.update { it.copy(showSignOutDialog = false) }
    }

    fun signOut() {
        _uiState.update { it.copy(showSignOutDialog = false) }
        viewModelScope.launch {
            when (val result = signOutUseCase()) {
                is Result.Success -> {
                    Timber.d("Successfully signed out")
                }

                is Result.Error -> {
                    Timber.e(result.cause, "Failed to sign out: %s", result.message)
                    _uiState.update {
                        it.copy(errorMessage = "Unable to sign out. Please try again.")
                    }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onDeleteAccountRequest() {
        _uiState.update { it.copy(showDeleteAccountDialog = true) }
    }

    fun onDeleteAccountConfirm() {
        _uiState.update {
            it.copy(
                showDeleteAccountDialog = false,
                showReauthDialog = true,
            )
        }
    }

    fun onReauthDismiss() {
        _uiState.update { it.copy(showReauthDialog = false) }
    }

    fun deleteAccount(password: String) {
        _uiState.update { it.copy(isDeletingAccount = true, showReauthDialog = false) }
        viewModelScope.launch {
            when (val result = deleteAccountUseCase(password)) {
                is Result.Success -> {
                    Timber.d("Account deleted successfully")
                    _uiState.update {
                        it.copy(isDeletingAccount = false, accountDeleted = true)
                    }
                }

                is Result.Error -> {
                    Timber.e(result.cause, "Failed to delete account: %s", result.message)
                    _uiState.update {
                        it.copy(
                            isDeletingAccount = false,
                            errorMessage = result.message,
                        )
                    }
                }
            }
        }
    }

    fun onDeleteAccountDismiss() {
        _uiState.update { it.copy(showDeleteAccountDialog = false) }
    }
}
