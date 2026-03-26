package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.enums.CompletionType
import com.getaltair.kairos.domain.model.HabitDetail
import com.getaltair.kairos.domain.repository.CompletionRepository
import com.getaltair.kairos.domain.repository.HabitRepository
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CancellationException

/**
 * Fetches a habit with its recent completions and weekly completion rate.
 *
 * Returns a [HabitDetail] containing:
 * - The habit entity
 * - Completions for the last 30 days
 * - Weekly completion rate (Full + Partial completions / 7 days)
 */
class GetHabitDetailUseCase(
    private val habitRepository: HabitRepository,
    private val completionRepository: CompletionRepository
) {

    suspend operator fun invoke(habitId: UUID): Result<HabitDetail> {
        return try {
            val habitResult = habitRepository.getById(habitId)
            if (habitResult is Result.Error) {
                return Result.Error("Habit not found: ${habitResult.message}")
            }
            val habit = (habitResult as Result.Success).value

            val today = LocalDate.now()
            val thirtyDaysAgo = today.minusDays(29)

            val completionsResult = completionRepository.getForHabitInDateRange(
                habitId,
                thirtyDaysAgo,
                today
            )
            if (completionsResult is Result.Error) return completionsResult
            val completions = (completionsResult as Result.Success).value

            val sevenDaysAgo = today.minusDays(6)
            val weeklyCompletions = completions.filter { it.date >= sevenDaysAgo }
            val successCount = weeklyCompletions.count {
                it.type is CompletionType.Full || it.type is CompletionType.Partial
            }
            val weeklyRate = successCount.toFloat() / 7f

            Result.Success(
                HabitDetail(
                    habit = habit,
                    recentCompletions = completions,
                    weeklyCompletionRate = weeklyRate
                )
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error("Failed to get habit detail: ${e.message}", cause = e)
        }
    }
}
