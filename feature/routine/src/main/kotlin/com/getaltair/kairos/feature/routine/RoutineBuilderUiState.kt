package com.getaltair.kairos.feature.routine

import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.enums.HabitCategory

/**
 * A habit selected for inclusion in a routine, with an optional duration override.
 *
 * @property habit The habit entity
 * @property overrideDurationSeconds Custom duration in seconds, or null to use the habit's default
 */
data class SelectedHabit(val habit: Habit, val overrideDurationSeconds: Int?,)

/**
 * UI state for the routine builder screen.
 *
 * @property name Routine name entered by the user
 * @property category Selected time-of-day category
 * @property availableHabits All active habits the user can add
 * @property selectedHabits Ordered list of habits added to the routine with optional duration overrides
 * @property isLoading Whether data is being loaded or saved
 * @property error User-facing error message, if any
 * @property isSaved Whether the routine was saved successfully
 * @property isEditMode Whether editing an existing routine (vs creating new)
 * @property savedRoutineId The ID of the saved routine for navigation
 */
data class RoutineBuilderUiState(
    val name: String = "",
    val category: HabitCategory = HabitCategory.Morning,
    val availableHabits: List<Habit> = emptyList(),
    val selectedHabits: List<SelectedHabit> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false,
    val isEditMode: Boolean = false,
    val savedRoutineId: String? = null,
)
