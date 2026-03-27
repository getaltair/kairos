package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Completion
import com.getaltair.kairos.domain.enums.CompletionType
import com.getaltair.kairos.domain.repository.CompletionRepository
import com.getaltair.kairos.domain.repository.HabitRepository
import java.time.LocalDate
import kotlinx.coroutines.CancellationException

/**
 * Creates MISSED completions for active habits that had no completion on a given date.
 *
 * Typically invoked by the system for yesterday's date so that lapse detection
 * can operate on a complete completion history. Only the system may create
 * MISSED-type completions (C-1).
 */
class CreateMissedCompletionsUseCase(
    private val habitRepository: HabitRepository,
    private val completionRepository: CompletionRepository
) {

    /**
     * @param date The date to back-fill missed completions for (defaults to yesterday).
     * @return The count of newly created MISSED completions.
     */
    suspend operator fun invoke(date: LocalDate = LocalDate.now().minusDays(1)): Result<Int> {
        return try {
            val habitsResult = habitRepository.getActiveHabits()
            if (habitsResult is Result.Error) return Result.Error(habitsResult.message)
            val activeHabits = (habitsResult as Result.Success).value

            var createdCount = 0

            for (habit in activeHabits) {
                if (!habit.isDueToday(date)) continue

                val existingResult = completionRepository.getForHabitOnDate(habit.id, date)
                if (existingResult is Result.Error) continue

                val existing = (existingResult as Result.Success).value
                if (existing != null) continue

                val missed = Completion(
                    habitId = habit.id,
                    date = date,
                    type = CompletionType.Missed
                )
                val insertResult = completionRepository.insert(missed)
                if (insertResult is Result.Success) createdCount++
            }

            Result.Success(createdCount)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error("Failed to create missed completions: ${e.message}", cause = e)
        }
    }
}
