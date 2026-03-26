package com.getaltair.kairos.core.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.enums.HabitCategory
import com.getaltair.kairos.domain.model.HabitWithStatus
import com.getaltair.kairos.domain.repository.CompletionRepository
import com.getaltair.kairos.domain.repository.HabitRepository
import com.getaltair.kairos.domain.util.HabitScheduleUtil
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import timber.log.Timber

/**
 * Retrieves today's habits with their completion status.
 * Filters out DEPARTURE category habits (shown on Pi dashboard, not phone).
 * Filters by frequency for the current day of week.
 * Computes a 7-day completion rate per habit using schedule-aware due-day counting.
 */
class GetTodayHabitsUseCase(
    private val habitRepository: HabitRepository,
    private val completionRepository: CompletionRepository
) {
    suspend operator fun invoke(): Result<List<HabitWithStatus>> {
        return try {
            val today = LocalDate.now()
            val weekAgo = today.minusDays(6)

            val habitsResult = habitRepository.getActiveHabits()
            if (habitsResult is Result.Error) {
                return Result.Error(habitsResult.message, habitsResult.cause)
            }

            val allActiveHabits = (habitsResult as Result.Success).value

            val todayHabits = allActiveHabits
                .filter { it.category !is HabitCategory.Departure }
                .filter { it.isDueToday() }

            val completionsResult = completionRepository.getForDate(today)
            if (completionsResult is Result.Error) {
                return Result.Error(completionsResult.message, completionsResult.cause)
            }

            val todayCompletions = (completionsResult as Result.Success).value
            val completionsByHabitId = todayCompletions.associateBy { it.habitId }

            val habitsWithStatus = todayHabits.map { habit ->
                val todayCompletion = completionsByHabitId[habit.id]

                val weekRate = computeWeeklyRate(habit, weekAgo, today)

                HabitWithStatus(
                    habit = habit,
                    todayCompletion = todayCompletion,
                    weekCompletionRate = weekRate
                )
            }

            Result.Success(habitsWithStatus)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error("Failed to load today's habits", e)
        }
    }

    private suspend fun computeWeeklyRate(habit: Habit, weekAgo: LocalDate, today: LocalDate): Float {
        val dueCount = HabitScheduleUtil.countDueDays(habit, weekAgo, today)
        if (dueCount == 0) return 0f

        val weekResult = completionRepository.getForHabitInDateRange(habit.id, weekAgo, today)
        return when (weekResult) {
            is Result.Success -> (weekResult.value.size.toFloat() / dueCount.toFloat()).coerceIn(0f, 1f)

            is Result.Error -> {
                Timber.w(weekResult.cause, "Failed to compute weekly rate for habit=%s", habit.id)
                0f
            }
        }
    }
}
