package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Completion
import com.getaltair.kairos.domain.enums.CompletionType
import com.getaltair.kairos.domain.enums.HabitStatus
import com.getaltair.kairos.domain.repository.CompletionRepository
import com.getaltair.kairos.domain.repository.HabitRepository
import com.getaltair.kairos.domain.validator.CompletionValidator
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CancellationException

/**
 * Creates a completion for a past date (backdating).
 *
 * Enforces:
 * - Habit must exist and be Active
 * - C-1: Cannot manually create MISSED completions
 * - C-3: No duplicate completion for the same habit+date
 * - C-4: No future dates (via [CompletionValidator])
 * - C-5: Within 7-day window (via [CompletionValidator])
 */
class BackdateCompletionUseCase(
    private val habitRepository: HabitRepository,
    private val completionRepository: CompletionRepository
) {

    suspend operator fun invoke(
        habitId: UUID,
        date: LocalDate,
        type: CompletionType,
        partialPercent: Int? = null
    ): Result<Completion> {
        return try {
            val habitResult = habitRepository.getById(habitId)
            if (habitResult is Result.Error) {
                return Result.Error("Habit not found: ${habitResult.message}")
            }
            val habit = (habitResult as Result.Success).value

            if (habit.status !is HabitStatus.Active) {
                return Result.Error(
                    "Cannot backdate completion: habit status is ${habit.status.displayName}, expected Active"
                )
            }

            // C-1: Cannot manually create MISSED completions
            if (type is CompletionType.Missed) {
                return Result.Error("Cannot manually create MISSED completions")
            }

            val completion = Completion(
                id = UUID.randomUUID(),
                habitId = habitId,
                date = date,
                type = type,
                partialPercent = partialPercent
            )

            // C-4 and C-5: Validate date constraints
            val validation = CompletionValidator.validate(completion)
            if (validation is Result.Error) return validation

            // C-3: One completion per habit per day
            val existingResult = completionRepository.getForHabitOnDate(habitId, date)
            if (existingResult is Result.Error) return existingResult
            val existing = (existingResult as Result.Success).value
            if (existing != null) {
                return Result.Error("A completion already exists for this habit on $date")
            }

            completionRepository.insert(completion)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error("Failed to backdate completion: ${e.message}", cause = e)
        }
    }
}
