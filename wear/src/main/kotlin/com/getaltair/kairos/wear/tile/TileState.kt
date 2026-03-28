package com.getaltair.kairos.wear.tile

import com.getaltair.kairos.domain.wear.WearCompletionData
import com.getaltair.kairos.domain.wear.WearHabitData

sealed class TileState {
    object Loading : TileState()
    object Empty : TileState()
    object AllDone : TileState()
    data class HasHabits(
        val habits: List<WearHabitData>,
        val completions: List<WearCompletionData>,
        val completedCount: Int,
        val totalCount: Int,
    ) : TileState() {
        val progress: Float
            get() = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount

        val pendingHabits: List<WearHabitData>
            get() {
                val completedIds = completions.map { it.habitId }.toSet()
                return habits.filter { it.id !in completedIds }
            }
    }
}
