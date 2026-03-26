package com.getaltair.kairos.feature.today

import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.model.HabitWithStatus
import java.time.LocalDate
import java.util.UUID

enum class UndoActionType { COMPLETE, PARTIAL, SKIP }

data class UndoState(
    val completionId: UUID,
    val habitName: String,
    val remainingSeconds: Int,
    val actionType: UndoActionType
) {
    init {
        require(remainingSeconds in 0..UNDO_WINDOW_SECONDS)
        require(habitName.isNotBlank())
    }
    companion object {
        const val UNDO_WINDOW_SECONDS = 30
    }
}

data class TodayUiState(
    val habitsByCategory: Map<HabitCategory, List<HabitWithStatus>> = emptyMap(),
    val date: LocalDate = LocalDate.now(),
    val isLoading: Boolean = true,
    val undoState: UndoState? = null,
    val error: String? = null
) {
    private val allHabits: List<HabitWithStatus>
        get() = habitsByCategory.values.flatten()
    val isEmpty: Boolean get() = allHabits.isEmpty()
    val isAllDone: Boolean get() = allHabits.isNotEmpty() && allHabits.all { it.todayCompletion != null }
    val progress: Float
        get() = if (allHabits.isEmpty()) {
            0f
        } else {
            allHabits.count { it.todayCompletion != null }.toFloat() / allHabits.size
        }
}
