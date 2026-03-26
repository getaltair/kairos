package com.getaltair.kairos.feature.habit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.enums.AnchorType
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.HabitFrequency
import com.getaltair.kairos.domain.usecase.EditHabitUseCase
import com.getaltair.kairos.domain.usecase.GetHabitUseCase
import java.time.DayOfWeek
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class EditHabitViewModel(private val editHabitUseCase: EditHabitUseCase, private val getHabitUseCase: GetHabitUseCase) :
    ViewModel() {

    private val _uiState = MutableStateFlow(EditHabitUiState())
    val uiState: StateFlow<EditHabitUiState> = _uiState.asStateFlow()

    fun loadHabit(habitId: UUID) {
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            try {
                when (val result = getHabitUseCase(habitId)) {
                    is Result.Success -> {
                        val habit = result.value
                        _uiState.update {
                            it.copy(
                                habitId = habit.id,
                                name = habit.name,
                                anchorType = habit.anchorType,
                                anchorBehavior = habit.anchorBehavior,
                                anchorTime = if (habit.anchorType is AnchorType.AtTime) {
                                    habit.timeWindowStart
                                } else {
                                    null
                                },
                                category = habit.category,
                                estimatedSeconds = habit.estimatedSeconds,
                                microVersion = habit.microVersion ?: "",
                                icon = habit.icon,
                                color = habit.color,
                                frequency = habit.frequency,
                                activeDays = habit.activeDays ?: emptySet(),
                                isLoading = false
                            )
                        }
                    }

                    is Result.Error -> {
                        Timber.e(result.cause, "loadHabit failed: %s", result.message)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                loadError = ErrorMapper.toUserMessage(result.message)
                            )
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error loading habit for edit")
                _uiState.update {
                    it.copy(isLoading = false, loadError = "Something went wrong. Please try again.")
                }
            }
        }
    }

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

    fun saveHabit() {
        if (_uiState.value.isSaving) return

        val state = _uiState.value
        val habitId = state.habitId ?: return

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

        val category = state.category
        if (category == null) {
            _uiState.update { it.copy(categoryError = "Please select a category") }
            return
        }

        _uiState.update { it.copy(isSaving = true, saveError = null) }

        viewModelScope.launch {
            try {
                // Fetch current habit to preserve non-editable fields
                val currentResult = getHabitUseCase(habitId)
                if (currentResult is Result.Error) {
                    _uiState.update {
                        it.copy(isSaving = false, saveError = "Could not load habit. Please try again.")
                    }
                    return@launch
                }
                val current = (currentResult as Result.Success).value

                val activeDays = if (state.frequency is HabitFrequency.Custom) {
                    state.activeDays
                } else {
                    null
                }

                val updatedHabit = current.copy(
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
                    color = state.color
                )

                when (val result = editHabitUseCase(updatedHabit)) {
                    is Result.Success -> {
                        _uiState.update { it.copy(isSaving = false, isSaved = true) }
                    }

                    is Result.Error -> {
                        Timber.e(result.cause, "saveHabit failed: %s", result.message)
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                saveError = ErrorMapper.toUserMessage(result.message)
                            )
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error saving habit")
                _uiState.update {
                    it.copy(isSaving = false, saveError = "Something went wrong. Please try again.")
                }
            }
        }
    }

    fun clearSaveError() {
        _uiState.update {
            if (it.saveError != null) {
                it.copy(saveError = null)
            } else {
                it
            }
        }
    }
}
