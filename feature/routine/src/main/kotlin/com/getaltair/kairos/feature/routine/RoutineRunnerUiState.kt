package com.getaltair.kairos.feature.routine

/**
 * Presentation-layer step result type for the routine runner.
 *
 * This intentionally differs from the domain [com.getaltair.kairos.domain.enums.StepResult]
 * which only has Completed and Skipped. The UI needs a PENDING state for steps that have
 * not yet been reached, which the domain does not model.
 */
enum class StepResultType {
    PENDING,
    DONE,
    SKIPPED,
}

/**
 * UI state for the routine runner screen.
 *
 * @property routineName Display name of the running routine
 * @property currentStepIndex Zero-based index of the current step
 * @property totalSteps Total number of steps in the routine
 * @property currentHabitName Name of the habit at the current step
 * @property timeRemainingSeconds Countdown seconds remaining for the current step
 * @property totalTimeSeconds Total allocated seconds for the current step
 * @property isPaused Whether the timer is currently paused
 * @property upNextHabitName Name of the next habit, or null if on last step
 * @property stepResults Results for each step (PENDING, DONE, SKIPPED)
 * @property isComplete Whether the routine has been fully completed
 * @property executionId The UUID of the RoutineExecution record
 * @property isLoading Whether the routine is loading or starting
 * @property error User-facing error message, if any
 */
data class RoutineRunnerUiState(
    val routineName: String = "",
    val currentStepIndex: Int = 0,
    val totalSteps: Int = 0,
    val currentHabitName: String = "",
    val timeRemainingSeconds: Int = 0,
    val totalTimeSeconds: Int = 0,
    val isPaused: Boolean = false,
    val upNextHabitName: String? = null,
    val stepResults: List<StepResultType> = emptyList(),
    val isComplete: Boolean = false,
    val executionId: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)
