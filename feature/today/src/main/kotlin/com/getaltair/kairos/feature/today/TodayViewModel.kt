package com.getaltair.kairos.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getaltair.kairos.core.usecase.CompleteHabitUseCase
import com.getaltair.kairos.core.usecase.GetTodayHabitsUseCase
import com.getaltair.kairos.core.usecase.SkipHabitUseCase
import com.getaltair.kairos.core.usecase.UndoCompletionUseCase
import com.getaltair.kairos.core.widget.WidgetRefreshNotifier
import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.enums.CompletionType
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.enums.SkipReason
import com.getaltair.kairos.domain.model.HabitWithStatus
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class TodayViewModel(
    private val widgetRefreshNotifier: WidgetRefreshNotifier,
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
            try {
                val result = getTodayHabitsUseCase()
                when (result) {
                    is Result.Success -> {
                        _uiState.update {
                            it.copy(
                                habitsByCategory = groupByCategory(result.value),
                                isLoading = false,
                                error = null
                            )
                        }
                    }

                    is Result.Error -> {
                        Timber.e(result.cause, "Failed to load habits: %s", result.message)
                        _uiState.update {
                            it.copy(isLoading = false, error = "Something went wrong. Please try again.")
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Unexpected error loading habits")
                _uiState.update { it.copy(isLoading = false, error = "An unexpected error occurred") }
            }
        }
    }

    private fun groupByCategory(habits: List<HabitWithStatus>): Map<HabitCategory, List<HabitWithStatus>> = habits
        .filter { it.habit.category !is HabitCategory.Departure }
        .groupBy { it.habit.category }
        .entries
        .sortedBy { entry ->
            val index = categoryDisplayOrder.indexOf(entry.key)
            if (index == -1) categoryDisplayOrder.size else index
        }
        .associate { it.key to it.value }

    fun onHabitComplete(habitId: UUID, completionType: CompletionType, partialPercent: Int? = null) {
        viewModelScope.launch {
            try {
                val result = completeHabitUseCase(habitId, completionType, partialPercent)
                when (result) {
                    is Result.Success -> {
                        val habitName = findHabitName(habitId)
                        val actionType = if (completionType == CompletionType.Partial) {
                            UndoActionType.PARTIAL
                        } else {
                            UndoActionType.COMPLETE
                        }
                        startUndoTimer(result.value.id, habitName, actionType)
                        loadTodayHabits()
                        refreshWidget()
                    }

                    is Result.Error -> {
                        Timber.e(result.cause, "Failed to complete habit: %s", result.message)
                        _uiState.update {
                            it.copy(error = "Something went wrong. Please try again.")
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Unexpected error completing habit")
                _uiState.update {
                    it.copy(error = "Something went wrong. Please try again.")
                }
            }
        }
    }

    fun onHabitSkip(habitId: UUID, skipReason: SkipReason? = null) {
        viewModelScope.launch {
            try {
                val result = skipHabitUseCase(habitId, skipReason)
                when (result) {
                    is Result.Success -> {
                        val habitName = findHabitName(habitId)
                        startUndoTimer(result.value.id, habitName, UndoActionType.SKIP)
                        loadTodayHabits()
                        refreshWidget()
                    }

                    is Result.Error -> {
                        Timber.e(result.cause, "Failed to skip habit: %s", result.message)
                        _uiState.update {
                            it.copy(error = "Something went wrong. Please try again.")
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Unexpected error skipping habit")
                _uiState.update {
                    it.copy(error = "Something went wrong. Please try again.")
                }
            }
        }
    }

    fun onUndoCompletion() {
        val currentUndo = _uiState.value.undoState ?: return
        undoTimerJob?.cancel()
        viewModelScope.launch {
            try {
                val result = undoCompletionUseCase(currentUndo.completionId)
                when (result) {
                    is Result.Success -> {
                        _uiState.update { it.copy(undoState = null) }
                        loadTodayHabits()
                        refreshWidget()
                    }

                    is Result.Error -> {
                        Timber.e(result.cause, "Failed to undo completion: %s", result.message)
                        _uiState.update {
                            it.copy(undoState = null, error = "Something went wrong. Please try again.")
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Error undoing completion")
                _uiState.update {
                    it.copy(undoState = null, error = "Something went wrong. Please try again.")
                }
            }
        }
    }

    fun onDismissUndo() {
        undoTimerJob?.cancel()
        _uiState.update { it.copy(undoState = null) }
    }

    fun refresh() {
        loadTodayHabits()
    }

    fun retryLoad() {
        _uiState.update { it.copy(isLoading = true) }
        loadTodayHabits()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun findHabitName(habitId: UUID): String = _uiState.value.habitsByCategory.values
        .flatten()
        .find { it.habit.id == habitId }
        ?.habit?.name ?: "Habit"

    private fun refreshWidget() {
        viewModelScope.launch {
            try {
                widgetRefreshNotifier.refreshAll()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "Failed to refresh widget")
            }
        }
    }

    private fun startUndoTimer(completionId: UUID, habitName: String, actionType: UndoActionType) {
        undoTimerJob?.cancel()
        undoTimerJob = viewModelScope.launch {
            for (remaining in UndoState.UNDO_WINDOW_SECONDS downTo 0) {
                _uiState.update {
                    it.copy(
                        undoState = UndoState(completionId, habitName, remaining, actionType)
                    )
                }
                if (remaining > 0) delay(1000)
            }
            _uiState.update { it.copy(undoState = null) }
        }
    }
}
