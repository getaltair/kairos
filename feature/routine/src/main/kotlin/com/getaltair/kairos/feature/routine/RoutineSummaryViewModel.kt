package com.getaltair.kairos.feature.routine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.entity.RoutineExecution
import com.getaltair.kairos.domain.entity.RoutineHabit
import com.getaltair.kairos.domain.usecase.GetRoutineDetailUseCase
import com.getaltair.kairos.domain.usecase.GetRoutineExecutionUseCase
import java.time.Duration
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Data for a single habit result in the summary.
 */
data class HabitSummaryItem(val habitName: String, val wasCompleted: Boolean, val durationSeconds: Int,)

/**
 * UI state for the routine summary screen.
 */
data class RoutineSummaryUiState(
    val routineName: String = "",
    val totalTimeSeconds: Long = 0,
    val habitsCompleted: Int = 0,
    val habitsSkipped: Int = 0,
    val habitResults: List<HabitSummaryItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

/**
 * ViewModel for the routine summary screen.
 *
 * Loads the RoutineExecution and its associated routine detail to display
 * the completion summary. Since individual step results are not persisted
 * on the execution entity, we infer step count from the routine detail
 * and the execution's currentStepIndex.
 */
class RoutineSummaryViewModel(
    private val executionId: String,
    private val getRoutineExecutionUseCase: GetRoutineExecutionUseCase,
    private val getRoutineDetailUseCase: GetRoutineDetailUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutineSummaryUiState())
    val uiState: StateFlow<RoutineSummaryUiState> = _uiState.asStateFlow()

    init {
        loadSummary()
    }

    private fun loadSummary() {
        viewModelScope.launch {
            try {
                val parsedId = UUID.fromString(executionId)

                // Load the execution
                when (val execResult = getRoutineExecutionUseCase(parsedId)) {
                    is Result.Error -> {
                        Timber.e("Failed to load execution: %s", execResult.message)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Something went wrong loading the summary.",
                            )
                        }
                        return@launch
                    }

                    is Result.Success -> {
                        val execution = execResult.value

                        // Load routine detail
                        when (val detailResult = getRoutineDetailUseCase(execution.routineId)) {
                            is Result.Error -> {
                                Timber.e("Failed to load routine detail: %s", detailResult.message)
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        error = "Something went wrong loading routine details.",
                                    )
                                }
                                return@launch
                            }

                            is Result.Success -> {
                                val (routine, habitsWithDetails) = detailResult.value
                                val totalSteps = habitsWithDetails.size
                                val completedSteps = execution.currentStepIndex

                                // Determine how many were completed vs skipped
                                // Since we don't have per-step results stored on execution,
                                // we know the total that were processed = currentStepIndex.
                                // We can't distinguish done vs skipped at summary level
                                // without additional data. For now, count all processed as done.
                                val totalTime = if (execution.completedAt != null) {
                                    Duration.between(
                                        execution.startedAt,
                                        execution.completedAt,
                                    ).seconds - execution.totalPausedSeconds
                                } else {
                                    0L
                                }

                                val habitResults = habitsWithDetails.mapIndexed { index, (rh, habit) ->
                                    val wasCompleted = index < completedSteps
                                    val duration = rh.overrideDurationSeconds
                                        ?: habit.estimatedSeconds
                                    HabitSummaryItem(
                                        habitName = habit.name,
                                        wasCompleted = wasCompleted,
                                        durationSeconds = duration,
                                    )
                                }

                                _uiState.update {
                                    it.copy(
                                        routineName = routine.name,
                                        totalTimeSeconds = totalTime.coerceAtLeast(0),
                                        habitsCompleted = habitResults.count { it.wasCompleted },
                                        habitsSkipped = totalSteps - habitResults.count { it.wasCompleted },
                                        habitResults = habitResults,
                                        isLoading = false,
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Unexpected error loading summary")
                _uiState.update {
                    it.copy(isLoading = false, error = "An unexpected error occurred")
                }
            }
        }
    }
}
