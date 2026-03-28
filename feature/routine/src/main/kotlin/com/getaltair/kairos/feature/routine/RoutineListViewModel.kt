package com.getaltair.kairos.feature.routine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.usecase.GetActiveRoutinesUseCase
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel for the routine list screen.
 *
 * Loads all active routines on initialization and exposes them
 * via [uiState]. Supports refresh and retry on error.
 */
class RoutineListViewModel(private val getActiveRoutinesUseCase: GetActiveRoutinesUseCase,) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutineListUiState(isLoading = true))
    val uiState: StateFlow<RoutineListUiState> = _uiState.asStateFlow()

    init {
        loadRoutines()
    }

    private fun loadRoutines() {
        viewModelScope.launch {
            try {
                when (val result = getActiveRoutinesUseCase()) {
                    is Result.Success -> {
                        _uiState.update {
                            it.copy(
                                routines = result.value,
                                isLoading = false,
                                error = null,
                            )
                        }
                    }

                    is Result.Error -> {
                        Timber.e(result.cause, "Failed to load routines: %s", result.message)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Something went wrong. Please try again.",
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Unexpected error loading routines")
                _uiState.update {
                    it.copy(isLoading = false, error = "An unexpected error occurred")
                }
            }
        }
    }

    fun refresh() {
        loadRoutines()
    }

    fun retryLoad() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        loadRoutines()
    }
}
