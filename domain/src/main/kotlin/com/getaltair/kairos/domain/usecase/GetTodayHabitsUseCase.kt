package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Completion
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.enums.CompletionType
import com.getaltair.kairos.domain.model.HabitWithStatus
import com.getaltair.kairos.domain.repository.CompletionRepository
import com.getaltair.kairos.domain.repository.HabitRepository
import com.getaltair.kairos.domain.util.HabitScheduleUtil
import java.time.LocalDate
import kotlinx.coroutines.CancellationException

/**
 * Gets all habits due today with their completion status and weekly rate.
 *
 * For each habit, calculates:
 * - Whether it was completed today
 * - The weekly completion rate (completions / days due in last 7 days)
 */
class GetTodayHabitsUseCase(
    private val habitRepository: HabitRepository,
    private val completionRepository: CompletionRepository
) {

    suspend operator fun invoke(): Result<List<HabitWithStatus>> {
        return try {
            val today = LocalDate.now()
            val weekStart = today.minusDays(6)

            val habitsResult = habitRepository.getHabitsForDate(today)
            if (habitsResult is Result.Error) return habitsResult

            val habits = (habitsResult as Result.Success).value

            val todayCompletionsResult = completionRepository.getForDate(today)
            if (todayCompletionsResult is Result.Error) return todayCompletionsResult

            val todayCompletions = (todayCompletionsResult as Result.Success).value

            val weekCompletionsResult = completionRepository.getForDateRange(weekStart, today)
            if (weekCompletionsResult is Result.Error) return weekCompletionsResult

            val weekCompletions = (weekCompletionsResult as Result.Success).value

            val todayByHabit = todayCompletions.associateBy { it.habitId }
            val weekByHabit = weekCompletions.groupBy { it.habitId }

            val result = habits.map { habit ->
                val todayCompletion = todayByHabit[habit.id]
                val habitWeekCompletions = weekByHabit[habit.id] ?: emptyList()
                val weekRate = calculateWeekRate(habit, habitWeekCompletions, weekStart, today)

                HabitWithStatus(
                    habit = habit,
                    todayCompletion = todayCompletion,
                    weekCompletionRate = weekRate
                )
            }

            Result.Success(result)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error("Failed to get today's habits: ${e.message}", cause = e)
        }
    }

    /**
     * Calculates the weekly completion rate for a habit.
     *
     * Rate = (completed + partial completions in last 7 days) /
     *        (days this habit was due in last 7 days)
     */
    private fun calculateWeekRate(
        habit: Habit,
        completions: List<Completion>,
        weekStart: LocalDate,
        today: LocalDate
    ): Float {
        val dueCount = HabitScheduleUtil.countDueDays(habit, weekStart, today)
        if (dueCount == 0) return 0f

        val completedCount = completions.count { completion ->
            completion.type is CompletionType.Full || completion.type is CompletionType.Partial
        }

        return (completedCount.toFloat() / dueCount.toFloat()).coerceIn(0f, 1f)
    }
}
