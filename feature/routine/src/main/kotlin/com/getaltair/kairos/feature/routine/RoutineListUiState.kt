package com.getaltair.kairos.feature.routine

import com.getaltair.kairos.domain.entity.Routine

/**
 * Enriched routine data for display in the list, including habit count and estimated duration.
 *
 * @property routine The routine entity
 * @property habitCount Number of habits in the routine
 * @property estimatedSeconds Total estimated duration in seconds across all habits
 */
data class RoutineListItem(val routine: Routine, val habitCount: Int, val estimatedSeconds: Int,)

/**
 * UI state for the routine list screen.
 *
 * @property routines List of active routines with enriched display data
 * @property isLoading Whether the initial load is in progress
 * @property error User-facing error message, if any
 */
data class RoutineListUiState(
    val routines: List<RoutineListItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)
