package com.getaltair.kairos.feature.recovery

import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.entity.RecoverySession
import com.getaltair.kairos.domain.enums.Blocker
import com.getaltair.kairos.domain.enums.RecoveryAction

/**
 * UI state for the recovery session wizard.
 *
 * @property currentStep The active wizard step
 * @property habit The habit being recovered (loaded from pending sessions)
 * @property session The recovery session being completed
 * @property selectedBlockers User-selected blockers (optional)
 * @property chosenAction The recovery action chosen by the user
 * @property isLoading Whether a background operation is in progress
 * @property error User-facing error message
 * @property confirmationMessage Supportive message shown on the confirmation step
 * @property isComplete Whether the session has been finalized
 */
data class RecoverySessionUiState(
    val currentStep: RecoveryStep = RecoveryStep.Welcome,
    val habit: Habit? = null,
    val session: RecoverySession? = null,
    val selectedBlockers: Set<Blocker> = emptySet(),
    val chosenAction: RecoveryAction? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val confirmationMessage: String? = null,
    val isComplete: Boolean = false
)

/**
 * Steps in the recovery session wizard.
 */
enum class RecoveryStep {
    Welcome,
    BlockerSelection,
    ActionSelection,
    Confirmation
}
