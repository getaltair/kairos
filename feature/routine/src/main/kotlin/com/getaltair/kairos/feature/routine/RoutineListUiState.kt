package com.getaltair.kairos.feature.routine

import com.getaltair.kairos.domain.entity.Routine

/**
 * UI state for the routine list screen.
 *
 * @property routines List of active routines to display
 * @property isLoading Whether the initial load is in progress
 * @property error User-facing error message, if any
 */
data class RoutineListUiState(
    val routines: List<Routine> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)
