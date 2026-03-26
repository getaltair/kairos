package com.getaltair.kairos.feature.habit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.enums.AnchorType
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.HabitFrequency
import com.getaltair.kairos.domain.enums.HabitPhase
import com.getaltair.kairos.domain.enums.HabitStatus
import com.getaltair.kairos.domain.usecase.CreateHabitUseCase
import com.getaltair.kairos.feature.habit.ErrorMapper
import java.time.DayOfWeek
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class CreateHabitViewModel(private val createHabitUseCase: CreateHabitUseCase) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateHabitUiState())
    val uiState: StateFlow<CreateHabitUiState> = _uiState.asStateFlow()

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(name = name, nameError = null) }
    }

    fun onAnchorTypeSelected(type: AnchorType) {
        _uiState.update {
            it.copy(
                anchorType = type,
                anchorBehavior = "",
                anchorTime = null,
                anchorError = null
            )
        }
    }

    fun onAnchorBehaviorChanged(text: String) {
        _uiState.update { it.copy(anchorBehavior = text, anchorError = null) }
    }

    fun onAnchorTimeChanged(time: String) {
        _uiState.update {
            it.copy(
                anchorTime = time,
                anchorBehavior = time,
                anchorError = null
            )
        }
    }

    fun onCategorySelected(category: HabitCategory) {
        _uiState.update { it.copy(category = category, categoryError = null) }
    }

    fun onEstimatedSecondsChanged(seconds: Int) {
        _uiState.update { it.copy(estimatedSeconds = seconds) }
    }

    fun onMicroVersionChanged(text: String) {
        _uiState.update { it.copy(microVersion = text) }
    }

    fun onIconSelected(icon: String?) {
        _uiState.update { it.copy(icon = icon) }
    }

    fun onColorSelected(color: String?) {
        _uiState.update { it.copy(color = color) }
    }

    fun onFrequencySelected(freq: HabitFrequency) {
        _uiState.update {
            val activeDays = if (freq is HabitFrequency.Custom) it.activeDays else emptySet()
            it.copy(frequency = freq, activeDays = activeDays)
        }
    }

    fun onActiveDaysChanged(days: Set<DayOfWeek>) {
        _uiState.update { it.copy(activeDays = days) }
    }

    fun goToNextStep() {
        val state = _uiState.value
        when (state.currentStep) {
            WizardStep.NAME -> {
                when {
                    state.name.isBlank() -> {
                        _uiState.update { it.copy(nameError = "Name is required") }
                        return
                    }

                    state.name.length > 100 -> {
                        _uiState.update { it.copy(nameError = "Name must be 100 characters or fewer") }
                        return
                    }
                }
                _uiState.update { it.copy(currentStep = WizardStep.ANCHOR) }
            }

            WizardStep.ANCHOR -> {
                when {
                    state.anchorType !is AnchorType.AtTime && state.anchorBehavior.isBlank() -> {
                        _uiState.update { it.copy(anchorError = "Please describe the anchor behavior") }
                        return
                    }

                    state.anchorType is AnchorType.AtTime && state.anchorTime == null -> {
                        _uiState.update { it.copy(anchorError = "Please set a time") }
                        return
                    }
                }
                _uiState.update { it.copy(currentStep = WizardStep.CATEGORY) }
            }

            WizardStep.CATEGORY -> {
                if (state.category == null) {
                    _uiState.update { it.copy(categoryError = "Please select a category") }
                    return
                }
                _uiState.update { it.copy(currentStep = WizardStep.OPTIONS) }
            }

            WizardStep.OPTIONS -> {
                // No-op: Create button handles submission
            }
        }
    }

    fun goToPreviousStep() {
        val state = _uiState.value
        when (state.currentStep) {
            WizardStep.NAME -> {
                // No-op: already on first step
            }

            WizardStep.ANCHOR -> _uiState.update { it.copy(currentStep = WizardStep.NAME) }

            WizardStep.CATEGORY -> _uiState.update { it.copy(currentStep = WizardStep.ANCHOR) }

            WizardStep.OPTIONS -> _uiState.update { it.copy(currentStep = WizardStep.CATEGORY) }
        }
    }

    fun createHabit() {
        if (_uiState.value.creationStatus is CreationStatus.Creating) return

        val state = _uiState.value

        val category = state.category
        if (category == null) {
            Timber.e("createHabit called with null category")
            _uiState.update {
                it.copy(creationStatus = CreationStatus.Failed("Please select a category before creating your habit."))
            }
            return
        }

        val activeDays = if (state.frequency is HabitFrequency.Custom) state.activeDays else null

        if (state.frequency is HabitFrequency.Custom && activeDays.isNullOrEmpty()) {
            Timber.w("createHabit called with Custom frequency but no active days selected")
            _uiState.update {
                it.copy(creationStatus = CreationStatus.Failed("Please select at least one day for custom frequency"))
            }
            return
        }

        val habit = Habit(
            name = state.name,
            anchorBehavior = state.anchorBehavior,
            anchorType = state.anchorType,
            timeWindowStart = state.anchorTime,
            category = category,
            frequency = state.frequency,
            activeDays = activeDays,
            estimatedSeconds = state.estimatedSeconds,
            microVersion = state.microVersion.ifBlank { null },
            icon = state.icon,
            color = state.color,
            allowPartialCompletion = true,
            phase = HabitPhase.ONBOARD,
            status = HabitStatus.Active,
            lapseThresholdDays = 3,
            relapseThresholdDays = 7
        )

        _uiState.update { it.copy(creationStatus = CreationStatus.Creating) }

        viewModelScope.launch {
            try {
                when (val result = createHabitUseCase(habit)) {
                    is Result.Success -> {
                        _uiState.update { it.copy(creationStatus = CreationStatus.Created) }
                    }

                    is Result.Error -> {
                        Timber.e(result.cause, "createHabit failed: %s", result.message)
                        _uiState.update {
                            it.copy(
                                creationStatus = CreationStatus.Failed(
                                    ErrorMapper.toUserMessage(result.message)
                                )
                            )
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error creating habit")
                _uiState.update {
                    it.copy(creationStatus = CreationStatus.Failed("Something went wrong. Please try again."))
                }
            }
        }
    }

    fun clearCreationError() {
        _uiState.update {
            if (it.creationStatus is CreationStatus.Failed) {
                it.copy(creationStatus = CreationStatus.Idle)
            } else {
                it
            }
        }
    }
}
