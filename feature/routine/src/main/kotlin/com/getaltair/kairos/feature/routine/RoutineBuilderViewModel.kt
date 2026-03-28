package com.getaltair.kairos.feature.routine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.usecase.CreateRoutineUseCase
import com.getaltair.kairos.domain.usecase.GetActiveHabitsUseCase
import com.getaltair.kairos.domain.usecase.GetRoutineDetailUseCase
import com.getaltair.kairos.domain.usecase.UpdateRoutineUseCase
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel for the routine builder screen.
 *
 * Supports both create and edit modes. In create mode, [routineId] is null.
 * In edit mode, [routineId] is provided and the existing routine data is loaded.
 *
 * Manages the list of available habits, selected habits with ordering,
 * and duration overrides. Validates R-1 (minimum 2 habits) before saving.
 */
class RoutineBuilderViewModel(
    private val routineId: String?,
    private val getRoutineDetailUseCase: GetRoutineDetailUseCase,
    private val getActiveHabitsUseCase: GetActiveHabitsUseCase,
    private val createRoutineUseCase: CreateRoutineUseCase,
    private val updateRoutineUseCase: UpdateRoutineUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        RoutineBuilderUiState(
            isLoading = true,
            isEditMode = routineId != null,
        )
    )
    val uiState: StateFlow<RoutineBuilderUiState> = _uiState.asStateFlow()

    /** Cached routine entity for edit mode, used when calling updateRoutineUseCase. */
    private var existingRoutine: com.getaltair.kairos.domain.entity.Routine? = null

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                // Load available habits
                when (val habitsResult = getActiveHabitsUseCase()) {
                    is Result.Success -> {
                        _uiState.update {
                            it.copy(availableHabits = habitsResult.value)
                        }
                    }

                    is Result.Error -> {
                        Timber.e(
                            habitsResult.cause,
                            "Failed to load habits: %s",
                            habitsResult.message,
                        )
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Something went wrong loading habits.",
                            )
                        }
                        return@launch
                    }
                }

                // If edit mode, load existing routine data
                if (routineId != null) {
                    val parsedId = UUID.fromString(routineId)
                    when (val detailResult = getRoutineDetailUseCase(parsedId)) {
                        is Result.Success -> {
                            val (routine, habitsWithDetails) = detailResult.value
                            existingRoutine = routine
                            val selected = habitsWithDetails.map { (routineHabit, habit) ->
                                SelectedHabit(habit, routineHabit.overrideDurationSeconds)
                            }
                            _uiState.update {
                                it.copy(
                                    name = routine.name,
                                    category = routine.category,
                                    selectedHabits = selected,
                                    isLoading = false,
                                )
                            }
                        }

                        is Result.Error -> {
                            Timber.e(
                                detailResult.cause,
                                "Failed to load routine: %s",
                                detailResult.message,
                            )
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = "Something went wrong loading the routine.",
                                )
                            }
                        }
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Unexpected error loading builder data")
                _uiState.update {
                    it.copy(isLoading = false, error = "An unexpected error occurred")
                }
            }
        }
    }

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name, error = null) }
    }

    fun updateCategory(category: HabitCategory) {
        _uiState.update { it.copy(category = category) }
    }

    fun addHabit(habit: Habit) {
        _uiState.update { state ->
            if (state.selectedHabits.any { it.habit.id == habit.id }) {
                state // Already selected
            } else {
                state.copy(
                    selectedHabits = state.selectedHabits + SelectedHabit(habit, null),
                    error = null,
                )
            }
        }
    }

    fun removeHabit(habitId: UUID) {
        _uiState.update { state ->
            state.copy(
                selectedHabits = state.selectedHabits.filter { it.habit.id != habitId },
            )
        }
    }

    fun reorderHabits(fromIndex: Int, toIndex: Int) {
        _uiState.update { state ->
            val list = state.selectedHabits.toMutableList()
            if (fromIndex in list.indices && toIndex in list.indices) {
                val item = list.removeAt(fromIndex)
                list.add(toIndex, item)
            }
            state.copy(selectedHabits = list)
        }
    }

    fun setDurationOverride(habitId: UUID, seconds: Int?) {
        _uiState.update { state ->
            state.copy(
                selectedHabits = state.selectedHabits.map { selected ->
                    if (selected.habit.id == habitId) {
                        selected.copy(overrideDurationSeconds = seconds)
                    } else {
                        selected
                    }
                },
            )
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.isLoading) return

        // Client-side validation
        if (state.name.isBlank()) {
            _uiState.update { it.copy(error = "Please give your routine a name.") }
            return
        }
        if (state.selectedHabits.size < 2) {
            _uiState.update { it.copy(error = "Add at least 2 habits to your routine.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                if (state.isEditMode) {
                    // C1 FIX: Edit mode calls updateRoutineUseCase
                    val routine = existingRoutine
                    if (routine == null) {
                        Timber.e("Edit mode but existingRoutine is null")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Something went wrong. Please try again.",
                            )
                        }
                        return@launch
                    }

                    val updatedRoutine = routine.copy(
                        name = state.name,
                        category = state.category,
                    )

                    when (val result = updateRoutineUseCase(updatedRoutine)) {
                        is Result.Success -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isSaved = true,
                                    savedRoutineId = result.value.id.toString(),
                                )
                            }
                        }

                        is Result.Error -> {
                            Timber.e(result.cause, "Failed to update routine: %s", result.message)
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = "Something went wrong. Please try again.",
                                )
                            }
                        }
                    }
                } else {
                    // Create mode
                    val habitIds = state.selectedHabits.map { it.habit.id }
                    val durations = state.selectedHabits
                        .filter { it.overrideDurationSeconds != null }
                        .associate { it.habit.id to it.overrideDurationSeconds }

                    when (
                        val result = createRoutineUseCase(
                            name = state.name,
                            category = state.category,
                            habitIds = habitIds,
                            durations = durations,
                        )
                    ) {
                        is Result.Success -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isSaved = true,
                                    savedRoutineId = result.value.id.toString(),
                                )
                            }
                        }

                        is Result.Error -> {
                            Timber.e(result.cause, "Failed to save routine: %s", result.message)
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = "Something went wrong. Please try again.",
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Unexpected error saving routine")
                _uiState.update {
                    it.copy(isLoading = false, error = "Something went wrong. Please try again.")
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
