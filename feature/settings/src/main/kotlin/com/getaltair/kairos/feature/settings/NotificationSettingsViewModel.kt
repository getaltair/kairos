package com.getaltair.kairos.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.UserPreferences
import com.getaltair.kairos.domain.repository.PreferencesRepository
import java.time.LocalTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class NotificationSettingsViewModel(private val preferencesRepository: PreferencesRepository,) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationSettingsUiState())
    val uiState: StateFlow<NotificationSettingsUiState> = _uiState.asStateFlow()

    /** Cached preferences for applying partial updates. */
    private var currentPreferences: UserPreferences? = null

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            try {
                when (val result = preferencesRepository.get()) {
                    is Result.Success -> {
                        currentPreferences = result.value
                        _uiState.update {
                            it.copy(
                                notificationsEnabled = result.value.notificationEnabled,
                                quietHoursEnabled = result.value.quietHoursEnabled,
                                quietHoursStart = result.value.quietHoursStart,
                                quietHoursEnd = result.value.quietHoursEnd,
                                isLoading = false,
                                error = null,
                            )
                        }
                    }

                    is Result.Error -> {
                        Timber.e(result.cause, "Failed to load preferences: %s", result.message)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Unable to load notification settings.",
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Unexpected error loading preferences")
                _uiState.update {
                    it.copy(isLoading = false, error = "An unexpected error occurred.")
                }
            }
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        val prefs = currentPreferences ?: return
        updatePreferences(prefs.copy(notificationEnabled = enabled))
    }

    fun toggleQuietHours(enabled: Boolean) {
        val prefs = currentPreferences ?: return
        updatePreferences(prefs.copy(quietHoursEnabled = enabled))
    }

    fun setQuietHoursStart(time: LocalTime) {
        val prefs = currentPreferences ?: return
        updatePreferences(prefs.copy(quietHoursStart = time))
    }

    fun setQuietHoursEnd(time: LocalTime) {
        val prefs = currentPreferences ?: return
        updatePreferences(prefs.copy(quietHoursEnd = time))
    }

    private fun updatePreferences(updated: UserPreferences) {
        viewModelScope.launch {
            try {
                when (val result = preferencesRepository.update(updated)) {
                    is Result.Success -> {
                        currentPreferences = result.value
                        _uiState.update {
                            it.copy(
                                notificationsEnabled = result.value.notificationEnabled,
                                quietHoursEnabled = result.value.quietHoursEnabled,
                                quietHoursStart = result.value.quietHoursStart,
                                quietHoursEnd = result.value.quietHoursEnd,
                                error = null,
                            )
                        }
                    }

                    is Result.Error -> {
                        Timber.e(result.cause, "Failed to update preferences: %s", result.message)
                        _uiState.update {
                            it.copy(error = "Unable to save settings. Please try again.")
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Unexpected error updating preferences")
                _uiState.update {
                    it.copy(error = "An unexpected error occurred.")
                }
            }
        }
    }
}
