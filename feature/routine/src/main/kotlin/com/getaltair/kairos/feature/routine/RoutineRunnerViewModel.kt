package com.getaltair.kairos.feature.routine

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.enums.StepResult
import com.getaltair.kairos.domain.model.RoutineStep
import com.getaltair.kairos.domain.usecase.AbandonRoutineUseCase
import com.getaltair.kairos.domain.usecase.AdvanceRoutineStepUseCase
import com.getaltair.kairos.domain.usecase.CompleteRoutineUseCase
import com.getaltair.kairos.domain.usecase.GetRoutineDetailUseCase
import com.getaltair.kairos.domain.usecase.StartRoutineUseCase
import com.getaltair.kairos.feature.routine.service.RoutineTimerService
import com.getaltair.kairos.feature.routine.service.RoutineTimerState
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Provides the current time in nanoseconds.
 * Defaults to [System.nanoTime]; tests can substitute a virtual-time source.
 */
fun interface NanoClock {
    fun nanoTime(): Long
}

/**
 * ViewModel for the routine runner screen.
 *
 * Manages the full lifecycle of a routine execution:
 * load routine detail, start execution, run countdown timer,
 * handle done/skip/pause/resume/abandon actions.
 *
 * Timer uses [System.nanoTime] for precise elapsed time tracking
 * rather than relying solely on delay tick counting.
 */
class RoutineRunnerViewModel(
    application: Application,
    private val routineId: String,
    private val getRoutineDetailUseCase: GetRoutineDetailUseCase,
    private val startRoutineUseCase: StartRoutineUseCase,
    private val advanceRoutineStepUseCase: AdvanceRoutineStepUseCase,
    private val completeRoutineUseCase: CompleteRoutineUseCase,
    private val abandonRoutineUseCase: AbandonRoutineUseCase,
    private val nanoClock: NanoClock = NanoClock { System.nanoTime() },
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(RoutineRunnerUiState())
    val uiState: StateFlow<RoutineRunnerUiState> = _uiState.asStateFlow()

    /** Ordered list of routine steps (RoutineHabit + Habit). */
    private var steps: List<RoutineStep> = emptyList()

    /** The execution UUID after starting. */
    private var executionId: UUID? = null

    /** The active timer coroutine job. Internal for test cancellation. */
    internal var timerJob: Job? = null

    // C5 FIX: Pause time accumulation
    private var pauseStartNanos: Long = 0L
    private var accumulatedPausedMs: Long = 0L

    // Action 7: Service call protection
    private fun startForegroundTimerService(habitName: String, timeRemaining: Int, stepInfo: String) {
        try {
            val intent = Intent(getApplication(), RoutineTimerService::class.java).apply {
                action = RoutineTimerService.ACTION_START
                putExtra(RoutineTimerService.EXTRA_ROUTINE_NAME, _uiState.value.routineName)
                putExtra(RoutineTimerService.EXTRA_HABIT_NAME, habitName)
                putExtra(RoutineTimerService.EXTRA_TIME_REMAINING, timeRemaining)
                putExtra(RoutineTimerService.EXTRA_STEP_INFO, stepInfo)
                putExtra(RoutineTimerService.EXTRA_IS_PAUSED, false)
            }
            getApplication<Application>().startForegroundService(intent)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.w(e, "Failed to start timer service -- continuing without it")
        }
    }

    private fun updateTimerService(habitName: String, timeRemaining: Int, stepInfo: String, isPaused: Boolean) {
        try {
            val intent = Intent(getApplication(), RoutineTimerService::class.java).apply {
                action = RoutineTimerService.ACTION_UPDATE
                putExtra(RoutineTimerService.EXTRA_HABIT_NAME, habitName)
                putExtra(RoutineTimerService.EXTRA_TIME_REMAINING, timeRemaining)
                putExtra(RoutineTimerService.EXTRA_STEP_INFO, stepInfo)
                putExtra(RoutineTimerService.EXTRA_IS_PAUSED, isPaused)
            }
            getApplication<Application>().startService(intent)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.w(e, "Failed to update timer service -- continuing without it")
        }
    }

    private fun stopTimerService() {
        try {
            val intent = Intent(getApplication(), RoutineTimerService::class.java).apply {
                action = RoutineTimerService.ACTION_STOP
            }
            getApplication<Application>().startService(intent)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.w(e, "Failed to stop timer service -- continuing without it")
        }
    }

    init {
        loadAndStart()
        // Collect RoutineTimerState actions from the Channel-backed flow.
        viewModelScope.launch {
            RoutineTimerState.action.collect { action ->
                when (action) {
                    RoutineTimerState.TimerAction.DONE -> onDone()
                    RoutineTimerState.TimerAction.SKIP -> onSkip()
                    RoutineTimerState.TimerAction.PAUSE -> onPause()
                    RoutineTimerState.TimerAction.RESUME -> onResume()
                }
            }
        }
    }

    private fun loadAndStart() {
        viewModelScope.launch {
            try {
                val parsedId = UUID.fromString(routineId)

                // Load routine detail
                when (val detailResult = getRoutineDetailUseCase(parsedId)) {
                    is Result.Error -> {
                        Timber.e("Failed to load routine: %s", detailResult.message)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Something went wrong loading the routine.",
                            )
                        }
                        return@launch
                    }

                    is Result.Success -> {
                        val (routine, habitsWithDetails) = detailResult.value
                        steps = habitsWithDetails

                        if (steps.isEmpty()) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = "This routine has no habits.",
                                )
                            }
                            return@launch
                        }

                        // Start execution
                        when (val startResult = startRoutineUseCase(parsedId)) {
                            is Result.Error -> {
                                Timber.e("Failed to start routine: %s", startResult.message)
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        error = "Something went wrong starting the routine.",
                                    )
                                }
                                return@launch
                            }

                            is Result.Success -> {
                                executionId = startResult.value.id
                                val firstStep = steps[0]
                                val duration = effectiveDuration(firstStep)

                                _uiState.update {
                                    it.copy(
                                        routineName = routine.name,
                                        currentStepIndex = 0,
                                        totalSteps = steps.size,
                                        currentHabitName = firstStep.habit.name,
                                        timeRemainingSeconds = duration,
                                        totalTimeSeconds = duration,
                                        isPaused = false,
                                        upNextHabitName = steps.getOrNull(1)?.habit?.name,
                                        stepResults = List(steps.size) { StepResultType.PENDING },
                                        isComplete = false,
                                        executionId = startResult.value.id.toString(),
                                        isLoading = false,
                                        error = null,
                                    )
                                }

                                startTimer()

                                val stepInfo = "Step 1 of ${steps.size}"
                                startForegroundTimerService(
                                    habitName = firstStep.habit.name,
                                    timeRemaining = duration,
                                    stepInfo = stepInfo,
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Unexpected error in loadAndStart")
                _uiState.update {
                    it.copy(isLoading = false, error = "An unexpected error occurred")
                }
            }
        }
    }

    /**
     * Starts or restarts the countdown timer for the current step.
     * Uses [System.nanoTime] for precise elapsed time measurement.
     */
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var lastTickNanos = nanoClock.nanoTime()

            while (_uiState.value.timeRemainingSeconds > 0) {
                if (_uiState.value.isPaused) {
                    delay(100)
                    lastTickNanos = nanoClock.nanoTime()
                    continue
                }

                delay(100) // Check frequently for responsiveness
                val now = nanoClock.nanoTime()
                val elapsedMs = (now - lastTickNanos) / 1_000_000

                if (elapsedMs >= 1000) {
                    lastTickNanos = now
                    _uiState.update {
                        it.copy(timeRemainingSeconds = (it.timeRemainingSeconds - 1).coerceAtLeast(0))
                    }
                    val state = _uiState.value
                    updateTimerService(
                        habitName = state.currentHabitName,
                        timeRemaining = state.timeRemainingSeconds,
                        stepInfo = "Step ${state.currentStepIndex + 1} of ${state.totalSteps}",
                        isPaused = false,
                    )
                }
            }

            // Timer expired -- auto-advance behavior
            // The user can still tap Done or Skip; timer reaching 0
            // does not auto-complete. The UI should prompt the user.
        }
    }

    /**
     * Marks the current step as DONE, advances to the next step or completes the routine.
     */
    fun onDone() {
        advanceStep(StepResultType.DONE, StepResult.Completed)
    }

    /**
     * Marks the current step as SKIPPED, advances to the next step or completes the routine.
     */
    fun onSkip() {
        advanceStep(StepResultType.SKIPPED, StepResult.Skipped)
    }

    private fun advanceStep(uiResult: StepResultType, domainResult: StepResult) {
        // Action 8: Null-guard logging
        val execId = executionId ?: run {
            Timber.w("advanceStep called with null executionId")
            return
        }
        val currentIndex = _uiState.value.currentStepIndex
        val currentStep = steps.getOrNull(currentIndex) ?: run {
            Timber.w("advanceStep called with null currentStep at index %d", currentIndex)
            return
        }

        viewModelScope.launch {
            try {
                // Record step result in domain
                val result = advanceRoutineStepUseCase(
                    executionId = execId,
                    stepResult = domainResult,
                    habitId = currentStep.routineHabit.habitId,
                )

                if (result is Result.Error) {
                    Timber.e("Failed to advance step: %s", result.message)
                    _uiState.update {
                        it.copy(error = "Something went wrong. Please try again.")
                    }
                    return@launch
                }

                // Update step results in UI
                val updatedResults = _uiState.value.stepResults.toMutableList()
                updatedResults[currentIndex] = uiResult

                val nextIndex = currentIndex + 1

                if (nextIndex >= steps.size) {
                    // Last step -- complete the routine
                    timerJob?.cancel()

                    // C2 FIX: Only set isComplete on success
                    when (val completeResult = completeRoutineUseCase(execId)) {
                        is Result.Error -> {
                            Timber.e("Failed to complete routine: %s", completeResult.message)
                            _uiState.update {
                                it.copy(error = "Failed to complete routine. Please try again.")
                            }
                            return@launch
                        }

                        is Result.Success -> {
                            // Success -- now safe to mark complete
                        }
                    }
                    stopTimerService()
                    _uiState.update {
                        it.copy(
                            stepResults = updatedResults,
                            isComplete = true,
                        )
                    }
                } else {
                    // Move to next step
                    val nextStep = steps[nextIndex]
                    val nextDuration = effectiveDuration(nextStep)
                    val upNext = steps.getOrNull(nextIndex + 1)?.habit?.name

                    _uiState.update {
                        it.copy(
                            currentStepIndex = nextIndex,
                            currentHabitName = nextStep.habit.name,
                            timeRemainingSeconds = nextDuration,
                            totalTimeSeconds = nextDuration,
                            upNextHabitName = upNext,
                            stepResults = updatedResults,
                            isPaused = false,
                        )
                    }

                    updateTimerService(
                        habitName = nextStep.habit.name,
                        timeRemaining = nextDuration,
                        stepInfo = "Step ${nextIndex + 1} of ${steps.size}",
                        isPaused = false,
                    )

                    startTimer()
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Unexpected error advancing step")
                _uiState.update {
                    it.copy(error = "Something went wrong. Please try again.")
                }
            }
        }
    }

    /**
     * Pauses the timer and records the pause start time for accumulation.
     */
    fun onPause() {
        // C5 FIX: Record pause start time
        pauseStartNanos = nanoClock.nanoTime()
        _uiState.update { it.copy(isPaused = true) }
        val state = _uiState.value
        updateTimerService(
            habitName = state.currentHabitName,
            timeRemaining = state.timeRemainingSeconds,
            stepInfo = "Step ${state.currentStepIndex + 1} of ${state.totalSteps}",
            isPaused = true,
        )
    }

    /**
     * Resumes the timer from paused state and accumulates paused duration.
     */
    fun onResume() {
        // C5 FIX: Accumulate paused time
        if (pauseStartNanos > 0L) {
            accumulatedPausedMs += (nanoClock.nanoTime() - pauseStartNanos) / 1_000_000
            pauseStartNanos = 0L
        }
        _uiState.update { it.copy(isPaused = false) }
        val state = _uiState.value
        updateTimerService(
            habitName = state.currentHabitName,
            timeRemaining = state.timeRemainingSeconds,
            stepInfo = "Step ${state.currentStepIndex + 1} of ${state.totalSteps}",
            isPaused = false,
        )
    }

    /**
     * Abandons the routine execution.
     * Marks the execution as Abandoned; partial completions are preserved.
     *
     * C3 FIX: Accepts a callback to run after the abandon completes,
     * preventing a race condition where navigation occurs before the
     * abandon use case finishes.
     */
    fun onAbandon(onAbandoned: () -> Unit) {
        val execId = executionId ?: run {
            Timber.w("onAbandon called with null executionId")
            return
        }
        timerJob?.cancel()
        stopTimerService()

        viewModelScope.launch {
            try {
                abandonRoutineUseCase(execId)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Unexpected error abandoning routine")
            }
            onAbandoned()
        }
    }

    /**
     * Gets the effective duration in seconds for a step,
     * using the override if present, or the habit's estimated duration.
     */
    private fun effectiveDuration(step: RoutineStep): Int =
        step.routineHabit.overrideDurationSeconds ?: step.habit.estimatedSeconds

    override fun onCleared() {
        timerJob?.cancel()
        stopTimerService()
        super.onCleared()
    }
}
