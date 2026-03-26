package com.getaltair.kairos.domain.model

import com.getaltair.kairos.domain.entity.Completion
import com.getaltair.kairos.domain.entity.Habit

/**
 * Aggregated view of a habit with its recent completions and weekly rate.
 * Used by the Habit Detail screen.
 *
 * @property habit The habit entity
 * @property recentCompletions Completions for the last 30 days
 * @property weeklyCompletionRate Fraction of days completed in the last 7 days (0.0 to 1.0)
 */
data class HabitDetail(val habit: Habit, val recentCompletions: List<Completion>, val weeklyCompletionRate: Float)
