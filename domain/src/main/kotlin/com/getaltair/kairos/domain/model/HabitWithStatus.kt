package com.getaltair.kairos.domain.model

import com.getaltair.kairos.domain.entity.Completion
import com.getaltair.kairos.domain.entity.Habit

/**
 * A habit paired with its current-day completion state and rolling weekly rate.
 *
 * @property habit the habit entity
 * @property todayCompletion today's completion record, or null if none exists
 * @property weekCompletionRate fraction of due days completed in the last 7 days (0.0..1.0)
 */
data class HabitWithStatus(val habit: Habit, val todayCompletion: Completion?, val weekCompletionRate: Float) {
    init {
        require(todayCompletion == null || todayCompletion.habitId == habit.id) {
            "todayCompletion.habitId must match habit.id"
        }
        require(weekCompletionRate in 0f..1f) {
            "weekCompletionRate must be in 0.0..1.0"
        }
    }
}
