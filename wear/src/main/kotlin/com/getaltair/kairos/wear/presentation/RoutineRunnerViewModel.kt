package com.getaltair.kairos.wear.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getaltair.kairos.wear.data.WearDataRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * UI state for the routine runner screen on the watch.
 * Tracks the current step, countdown timer, and navigation state.
 */
data class RoutineRunnerUiState(
    val currentStep: String = "",
    val nextStep: String? = null,
    val stepIndex: Int = 0,
    val totalSteps: Int = 0,
    val remainingSeconds: Int = 0,
    val isFinished: Boolean = false,
)

class RoutineRunnerViewModel(private val routineId: String, private val repository: WearDataRepository,) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutineRunnerUiState())
    val uiState: StateFlow<RoutineRunnerUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var currentExecutionId: String? = null

    init {
        viewModelScope.launch {
            repository.activeRoutine.collect { routine ->
                if (routine != null && routine.routineId == routineId) {
                    currentExecutionId = routine.executionId
                    val stepIndex = routine.currentStepIndex
                    _uiState.value = RoutineRunnerUiState(
                        currentStep = routine.steps.getOrElse(stepIndex) { "" },
                        nextStep = routine.steps.getOrNull(stepIndex + 1),
                        stepIndex = stepIndex,
                        totalSteps = routine.steps.size,
                        remainingSeconds = routine.remainingSeconds,
                    )
                    startTimer(routine.remainingSeconds)
                } else if (routine == null) {
                    _uiState.value = _uiState.value.copy(isFinished = true)
                }
            }
        }
    }

    private fun startTimer(seconds: Int) {
        timerJob?.cancel()
        if (seconds <= 0) return
        timerJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1000)
                remaining--
                _uiState.value = _uiState.value.copy(remainingSeconds = remaining)
            }
        }
    }

    fun onDone() {
        viewModelScope.launch {
            val execId = currentExecutionId ?: run {
                Timber.w("RoutineRunnerViewModel: executionId is null, ignoring action")
                return@launch
            }
            try {
                repository.advanceRoutineStep(execId)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "RoutineRunnerViewModel: error advancing routine step")
            }
        }
    }

    fun onSkip() {
        viewModelScope.launch {
            val execId = currentExecutionId ?: run {
                Timber.w("RoutineRunnerViewModel: executionId is null, ignoring action")
                return@launch
            }
            try {
                repository.skipRoutineStep(execId)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "RoutineRunnerViewModel: error skipping routine step")
            }
        }
    }

    fun onPause() {
        viewModelScope.launch {
            val execId = currentExecutionId ?: run {
                Timber.w("RoutineRunnerViewModel: executionId is null, ignoring action")
                return@launch
            }
            try {
                repository.pauseRoutine(execId)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "RoutineRunnerViewModel: error pausing routine")
            }
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}
