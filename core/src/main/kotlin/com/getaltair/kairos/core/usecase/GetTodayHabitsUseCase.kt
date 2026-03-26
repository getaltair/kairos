package com.getaltair.kairos.core.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.model.HabitWithStatus
import com.getaltair.kairos.domain.repository.CompletionRepository
import com.getaltair.kairos.domain.repository.HabitRepository
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Retrieves today's habits with their completion status.
 * Filters out DEPARTURE category habits (shown on Pi dashboard, not phone).
 * Filters by frequency for the current day of week.
 * Computes a 7-day completion rate per habit.
 */
class GetTodayHabitsUseCase(
    private val habitRepository: HabitRepository,
    private val completionRepository: CompletionRepository
) {
    operator fun invoke(): Flow<Result<List<HabitWithStatus>>> = flow {
        try {
            val today = LocalDate.now()
            val weekAgo = today.minusDays(6)

            val habitsResult = habitRepository.getActiveHabits()
            if (habitsResult is Result.Error) {
                emit(Result.Error(habitsResult.message, habitsResult.cause))
                return@flow
            }

            val allActiveHabits = (habitsResult as Result.Success).value

            val todayHabits = allActiveHabits
                .filter { it.category !is HabitCategory.Departure }
                .filter { it.isDueToday() }

            val completionsResult = completionRepository.getForDate(today)
            if (completionsResult is Result.Error) {
                emit(Result.Error(completionsResult.message, completionsResult.cause))
                return@flow
            }

            val todayCompletions = (completionsResult as Result.Success).value
            val completionsByHabitId = todayCompletions.associateBy { it.habitId }

            val habitsWithStatus = todayHabits.map { habit ->
                val todayCompletion = completionsByHabitId[habit.id]

                val weekRate = computeWeeklyRate(habit.id, weekAgo, today)

                HabitWithStatus(
                    habit = habit,
                    todayCompletion = todayCompletion,
                    weekCompletionRate = weekRate
                )
            }

            emit(Result.Success(habitsWithStatus))
        } catch (e: Exception) {
            emit(Result.Error("Failed to load today's habits", e))
        }
    }

    private suspend fun computeWeeklyRate(habitId: UUID, weekAgo: LocalDate, today: LocalDate): Float {
        val weekResult = completionRepository.getForHabitInDateRange(habitId, weekAgo, today)
        return when (weekResult) {
            is Result.Success -> weekResult.value.size.toFloat() / 7f
            is Result.Error -> 0f
        }
    }
}
