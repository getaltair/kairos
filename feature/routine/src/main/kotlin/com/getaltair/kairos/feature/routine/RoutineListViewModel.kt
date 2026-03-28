package com.getaltair.kairos.feature.routine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.usecase.GetActiveRoutinesUseCase
import com.getaltair.kairos.domain.usecase.GetRoutineDetailUseCase
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
class RoutineListViewModel(
    private val getActiveRoutinesUseCase: GetActiveRoutinesUseCase,
    private val getRoutineDetailUseCase: GetRoutineDetailUseCase,
) : ViewModel() {

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
                        val enrichedRoutines = result.value.map { routine ->
                            when (val detailResult = getRoutineDetailUseCase(routine.id)) {
                                is Result.Success -> {
                                    val (_, habitsWithDetails) = detailResult.value
                                    RoutineListItem(
                                        routine = routine,
                                        habitCount = habitsWithDetails.size,
                                        estimatedSeconds = habitsWithDetails.sumOf { (rh, habit) ->
                                            rh.overrideDurationSeconds ?: habit.estimatedSeconds
                                        },
                                    )
                                }

                                is Result.Error -> {
                                    Timber.w(
                                        "Failed to load detail for routine %s: %s",
                                        routine.id,
                                        detailResult.message
                                    )
                                    RoutineListItem(
                                        routine = routine,
                                        habitCount = 0,
                                        estimatedSeconds = 0,
                                    )
                                }
                            }
                        }
                        _uiState.update {
                            it.copy(
                                routines = enrichedRoutines,
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

    /** Refreshes the routine list, showing a loading indicator while fetching. */
    fun refresh() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        loadRoutines()
    }

    fun retryLoad() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        loadRoutines()
    }
}
