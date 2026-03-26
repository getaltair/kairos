package com.getaltair.kairos.feature.habit

import com.getaltair.kairos.domain.entity.Completion
import com.getaltair.kairos.domain.entity.Habit

data class HabitDetailUiState(
    val habit: Habit? = null,
    val recentCompletions: List<Completion> = emptyList(),
    val weeklyCompletionRate: Float = 0f,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showDeleteConfirmation: Boolean = false,
    val actionResult: String? = null,
    val isDeleted: Boolean = false
)
