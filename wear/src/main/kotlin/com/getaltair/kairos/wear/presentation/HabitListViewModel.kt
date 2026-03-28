package com.getaltair.kairos.wear.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.getaltair.kairos.domain.wear.WearHabitData
import com.getaltair.kairos.wear.data.WearDataRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * UI state for the habit list screen on the watch.
 * Habits are grouped by category in display order, with DEPARTURE habits filtered out.
 */
data class HabitListUiState(
    val isLoading: Boolean = true,
    val habitsByCategory: Map<String, List<WearHabitData>> = emptyMap(),
    val completedIds: Set<String> = emptySet(),
    val completedCount: Int = 0,
    val totalCount: Int = 0,
)

class HabitListViewModel(private val repository: WearDataRepository) : ViewModel() {

    val uiState: StateFlow<HabitListUiState> = combine(
        repository.todayHabits,
        repository.todayCompletions,
    ) { habits, completions ->
        // DEPARTURE habits are device-specific triggers, not meant for manual tracking on the watch
        val filtered = habits.filter { it.category != "DEPARTURE" }
        val completedIds = completions.map { it.habitId }.toSet()
        val grouped = filtered.groupBy { it.category }
        val categoryOrder = listOf("MORNING", "AFTERNOON", "EVENING", "ANYTIME")
        val ordered = LinkedHashMap<String, List<WearHabitData>>()
        categoryOrder.forEach { cat -> grouped[cat]?.let { ordered[cat] = it } }
        grouped.filterKeys { it !in categoryOrder }.forEach { (k, v) -> ordered[k] = v }
        HabitListUiState(
            isLoading = false,
            habitsByCategory = ordered,
            completedIds = completedIds,
            completedCount = filtered.count { it.id in completedIds },
            totalCount = filtered.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HabitListUiState())
}
