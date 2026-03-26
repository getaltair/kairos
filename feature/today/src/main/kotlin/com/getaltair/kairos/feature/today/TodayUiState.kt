package com.getaltair.kairos.feature.today

import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.model.HabitWithStatus
import java.time.LocalDate
import java.util.UUID

data class UndoState(val completionId: UUID, val habitName: String, val remainingSeconds: Int)

data class TodayUiState(
    val habitsByCategory: Map<HabitCategory, List<HabitWithStatus>> = emptyMap(),
    val progress: Float = 0f,
    val date: LocalDate = LocalDate.now(),
    val isLoading: Boolean = true,
    val undoState: UndoState? = null,
    val isEmpty: Boolean = false,
    val isAllDone: Boolean = false,
    val error: String? = null
)
