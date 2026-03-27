package com.getaltair.kairos.feature.recovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.enums.Blocker
import com.getaltair.kairos.domain.enums.RecoveryAction
import com.getaltair.kairos.domain.enums.RecoveryType
import com.getaltair.kairos.domain.usecase.CompleteRecoverySessionUseCase
import com.getaltair.kairos.domain.usecase.GetPendingRecoveriesUseCase
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel driving the recovery session wizard.
 *
 * Loads the pending recovery session for the given [habitId], then walks the
 * user through the Welcome, BlockerSelection, ActionSelection, and Confirmation
 * steps. All user-facing copy follows FR-6 shame-free language rules.
 */
class RecoverySessionViewModel(
    private val habitId: String,
    private val getPendingRecoveriesUseCase: GetPendingRecoveriesUseCase,
    private val completeRecoverySessionUseCase: CompleteRecoverySessionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecoverySessionUiState())
    val uiState: StateFlow<RecoverySessionUiState> = _uiState.asStateFlow()

    init {
        loadRecoverySession()
    }

    private fun loadRecoverySession() {
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val parsedId = UUID.fromString(habitId)

                when (val result = getPendingRecoveriesUseCase()) {
                    is Result.Success -> {
                        val match = result.value.firstOrNull { (_, habit) ->
                            habit.id == parsedId
                        }
                        if (match != null) {
                            _uiState.update {
                                it.copy(
                                    session = match.first,
                                    habit = match.second,
                                    isLoading = false
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = "No pending recovery found for this habit."
                                )
                            }
                        }
                    }

                    is Result.Error -> {
                        Timber.e("Failed to load pending recoveries: %s", result.message)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Something went wrong. Please try again."
                            )
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error loading recovery session")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Something went wrong. Please try again."
                    )
                }
            }
        }
    }

    /** Adds or removes a blocker from the selection set. */
    fun toggleBlocker(blocker: Blocker) {
        _uiState.update {
            val updated = it.selectedBlockers.toMutableSet()
            if (blocker in updated) updated.remove(blocker) else updated.add(blocker)
            it.copy(selectedBlockers = updated)
        }
    }

    /** Skips blocker selection and advances to action selection. */
    fun skipBlockers() {
        _uiState.update { it.copy(currentStep = RecoveryStep.ActionSelection) }
    }

    /** Saves selected blockers and advances to action selection. */
    fun proceedFromBlockers() {
        _uiState.update { it.copy(currentStep = RecoveryStep.ActionSelection) }
    }

    /** Advances from Welcome to BlockerSelection. */
    fun proceedFromWelcome() {
        _uiState.update { it.copy(currentStep = RecoveryStep.BlockerSelection) }
    }

    /**
     * Sets the chosen recovery action, derives a shame-free confirmation
     * message, and advances to the Confirmation step.
     */
    fun chooseAction(action: RecoveryAction) {
        val message = confirmationMessageFor(action)
        _uiState.update {
            it.copy(
                chosenAction = action,
                confirmationMessage = message,
                currentStep = RecoveryStep.Confirmation
            )
        }
    }

    /**
     * Sends the chosen action to [CompleteRecoverySessionUseCase] and marks
     * the UI as complete on success.
     */
    fun confirmAction() {
        val state = _uiState.value
        val session = state.session ?: return
        val action = state.chosenAction ?: return

        if (state.isLoading) return

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                when (val result = completeRecoverySessionUseCase(session.id, action)) {
                    is Result.Success -> {
                        _uiState.update { it.copy(isLoading = false, isComplete = true) }
                    }

                    is Result.Error -> {
                        Timber.e("Failed to complete recovery: %s", result.message)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Something went wrong. Please try again."
                            )
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error completing recovery session")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Something went wrong. Please try again."
                    )
                }
            }
        }
    }

    /** Retreats to the previous wizard step. */
    fun goBack() {
        _uiState.update {
            when (it.currentStep) {
                RecoveryStep.Welcome -> it

                // Already on first step
                RecoveryStep.BlockerSelection -> it.copy(currentStep = RecoveryStep.Welcome)

                RecoveryStep.ActionSelection -> it.copy(currentStep = RecoveryStep.BlockerSelection)

                RecoveryStep.Confirmation -> it.copy(
                    currentStep = RecoveryStep.ActionSelection,
                    chosenAction = null,
                    confirmationMessage = null
                )
            }
        }
    }

    /**
     * Whether the FreshStart action should be available.
     * Only shown when the session type is Relapse.
     */
    fun isFreshStartAvailable(): Boolean = _uiState.value.session?.type is RecoveryType.Relapse

    /**
     * Whether the Simplify action should be enabled.
     * Disabled when the habit has no micro-version configured.
     */
    fun isSimplifyEnabled(): Boolean = _uiState.value.habit?.microVersion != null

    companion object {
        /**
         * Returns a supportive, shame-free confirmation message for the given action.
         * FR-6: No forbidden words (streak, broke, failed, failure, try harder, give up, should have).
         */
        internal fun confirmationMessageFor(action: RecoveryAction): String = when (action) {
            is RecoveryAction.Resume -> "You're back. Let's keep going."
            is RecoveryAction.Simplify -> "Starting smaller is still starting."
            is RecoveryAction.Pause -> "Taking a break is a valid choice."
            is RecoveryAction.Archive -> "This chapter is complete. New ones await."
            is RecoveryAction.FreshStart -> "Every moment is a new beginning."
        }
    }
}
