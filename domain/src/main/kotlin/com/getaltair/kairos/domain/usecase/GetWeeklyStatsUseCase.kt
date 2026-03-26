package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Completion
import com.getaltair.kairos.domain.enums.CompletionType
import com.getaltair.kairos.domain.model.WeeklyStats
import com.getaltair.kairos.domain.repository.CompletionRepository
import com.getaltair.kairos.domain.repository.HabitRepository
import com.getaltair.kairos.domain.util.HabitScheduleUtil
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CancellationException

/**
 * Computes weekly statistics for a single habit or all habits.
 *
 * Covers a 7-day window (today minus 6 days to today, inclusive).
 * If habitId is provided, returns stats for that habit only and computes
 * totalDays from the habit's actual schedule. If null, aggregates stats
 * across all habits with totalDays fixed at 7.
 */
class GetWeeklyStatsUseCase(
    private val habitRepository: HabitRepository,
    private val completionRepository: CompletionRepository
) {

    suspend operator fun invoke(habitId: UUID? = null): Result<WeeklyStats> {
        return try {
            val today = LocalDate.now()
            val weekStart = today.minusDays(6)

            val totalDays: Int
            val completions: List<Completion>

            if (habitId != null) {
                val habitResult = habitRepository.getById(habitId)
                if (habitResult is Result.Error) {
                    return Result.Error("Habit not found: ${(habitResult as Result.Error).message}")
                }
                val habit = (habitResult as Result.Success).value
                totalDays = HabitScheduleUtil.countDueDays(habit, weekStart, today)
                    .coerceAtLeast(1)

                val result = completionRepository.getForHabitInDateRange(
                    habitId,
                    weekStart,
                    today
                )
                if (result is Result.Error) return result
                completions = (result as Result.Success).value
            } else {
                // Aggregate mode: totalDays = 7 is an approximation because
                // different habits may have different schedules.
                totalDays = 7
                val result = completionRepository.getForDateRange(weekStart, today)
                if (result is Result.Error) return result
                completions = (result as Result.Success).value
            }

            val completedCount = completions.count { it.type is CompletionType.Full }
            val partialCount = completions.count { it.type is CompletionType.Partial }
            val skippedCount = completions.count { it.type is CompletionType.Skipped }
            val missedCount = completions.count { it.type is CompletionType.Missed }

            Result.Success(
                WeeklyStats(
                    habitId = habitId,
                    totalDays = totalDays,
                    completedCount = completedCount,
                    partialCount = partialCount,
                    skippedCount = skippedCount,
                    missedCount = missedCount
                )
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error("Failed to get weekly stats: ${e.message}", cause = e)
        }
    }
}
