package com.getaltair.kairos.wear.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getaltair.kairos.domain.wear.WearHabitData
import com.getaltair.kairos.wear.data.WearDataRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * UI state for the habit detail screen on the watch.
 * Shows the selected habit with actions to complete, partially complete, or skip.
 */
data class HabitDetailUiState(val habit: WearHabitData? = null, val isLoading: Boolean = true,)

class HabitDetailViewModel(private val habitId: String, private val repository: WearDataRepository,) : ViewModel() {

    val uiState: StateFlow<HabitDetailUiState> = repository.todayHabits
        .map { habits ->
            HabitDetailUiState(
                habit = habits.find { it.id == habitId },
                isLoading = false,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HabitDetailUiState())

    fun completeHabit(type: String = "FULL", partialPercent: Int? = null) {
        viewModelScope.launch {
            try {
                repository.completeHabit(habitId, type, partialPercent)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "HabitDetailViewModel: error completing habit %s", habitId)
            }
        }
    }

    fun skipHabit(reason: String? = null) {
        viewModelScope.launch {
            try {
                repository.skipHabit(habitId, reason)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.e(e, "HabitDetailViewModel: error skipping habit %s", habitId)
            }
        }
    }
}
