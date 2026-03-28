package com.getaltair.kairos.wear.tile

import com.getaltair.kairos.domain.wear.WearCompletionData
import com.getaltair.kairos.domain.wear.WearHabitData

sealed class TileState {
    object Loading : TileState()
    object Empty : TileState()
    object AllDone : TileState()
    data class Error(val message: String) : TileState()
    data class HasHabits(val habits: List<WearHabitData>, val completions: List<WearCompletionData>,) : TileState() {
        private val completedIds: Set<String>
            get() = completions.map { it.habitId }.toSet()

        val completedHabits: List<WearHabitData>
            get() = habits.filter { it.id in completedIds }

        val pendingHabits: List<WearHabitData>
            get() = habits.filter { it.id !in completedIds }

        val completedCount: Int get() = completedHabits.size

        val totalCount: Int get() = habits.size

        val progress: Float
            get() = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount
    }
}
