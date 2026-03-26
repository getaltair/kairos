package com.getaltair.kairos.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getaltair.kairos.core.usecase.CompleteHabitUseCase
import com.getaltair.kairos.core.usecase.GetTodayHabitsUseCase
import com.getaltair.kairos.core.usecase.SkipHabitUseCase
import com.getaltair.kairos.core.usecase.UndoCompletionUseCase
import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.enums.CompletionType
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.SkipReason
import com.getaltair.kairos.domain.model.HabitWithStatus
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TodayViewModel(
    private val getTodayHabitsUseCase: GetTodayHabitsUseCase,
    private val completeHabitUseCase: CompleteHabitUseCase,
    private val skipHabitUseCase: SkipHabitUseCase,
    private val undoCompletionUseCase: UndoCompletionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    private var undoTimerJob: Job? = null

    private val categoryDisplayOrder = listOf(
        HabitCategory.Morning,
        HabitCategory.Afternoon,
        HabitCategory.Evening,
        HabitCategory.Anytime
    )

    init {
        loadTodayHabits()
    }

    private fun loadTodayHabits() {
        viewModelScope.launch {
            getTodayHabitsUseCase().collect { result ->
                when (result) {
                    is Result.Success -> {
                        val habits = result.value
                        val grouped = groupByCategory(habits)
                        val totalCount = habits.size
                        val completedCount = habits.count { it.todayCompletion != null }
                        val progress = if (totalCount > 0) {
                            completedCount.toFloat() / totalCount.toFloat()
                        } else {
                            0f
                        }

                        _uiState.update {
                            it.copy(
                                habitsByCategory = grouped,
                                progress = progress,
                                isLoading = false,
                                isEmpty = totalCount == 0,
                                isAllDone = totalCount > 0 && completedCount == totalCount,
                                error = null
                            )
                        }
                    }

                    is Result.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.message
                            )
                        }
                    }
                }
            }
        }
    }

    private fun groupByCategory(habits: List<HabitWithStatus>): Map<HabitCategory, List<HabitWithStatus>> = habits
        .filter { it.habit.category !is HabitCategory.Departure }
        .groupBy { it.habit.category }
        .entries
        .sortedBy { categoryDisplayOrder.indexOf(it.key) }
        .associate { it.key to it.value }

    fun onHabitComplete(habitId: UUID, completionType: CompletionType, partialPercent: Int? = null) {
        viewModelScope.launch {
            val result = completeHabitUseCase(habitId, completionType, partialPercent)
            if (result is Result.Success) {
                val completion = result.value
                val habitName = _uiState.value.habitsByCategory.values
                    .flatten()
                    .find { it.habit.id == habitId }
                    ?.habit?.name ?: "Habit"
                startUndoTimer(completion.id, habitName)
            }
        }
    }

    fun onHabitSkip(habitId: UUID, skipReason: SkipReason? = null) {
        viewModelScope.launch {
            val result = skipHabitUseCase(habitId, skipReason)
            if (result is Result.Success) {
                val completion = result.value
                val habitName = _uiState.value.habitsByCategory.values
                    .flatten()
                    .find { it.habit.id == habitId }
                    ?.habit?.name ?: "Habit"
                startUndoTimer(completion.id, habitName)
            }
        }
    }

    fun onUndoCompletion() {
        val currentUndo = _uiState.value.undoState ?: return
        undoTimerJob?.cancel()
        viewModelScope.launch {
            undoCompletionUseCase(currentUndo.completionId)
            _uiState.update { it.copy(undoState = null) }
        }
    }

    fun onDismissUndo() {
        undoTimerJob?.cancel()
        _uiState.update { it.copy(undoState = null) }
    }

    private fun startUndoTimer(completionId: UUID, habitName: String) {
        undoTimerJob?.cancel()
        undoTimerJob = viewModelScope.launch {
            for (remaining in 30 downTo 0) {
                _uiState.update {
                    it.copy(
                        undoState = UndoState(completionId, habitName, remaining)
                    )
                }
                if (remaining > 0) delay(1000)
            }
            _uiState.update { it.copy(undoState = null) }
        }
    }
}
