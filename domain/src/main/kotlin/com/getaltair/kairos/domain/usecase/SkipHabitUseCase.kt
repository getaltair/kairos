package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Completion
import com.getaltair.kairos.domain.enums.CompletionType
import com.getaltair.kairos.domain.enums.SkipReason
import com.getaltair.kairos.domain.repository.CompletionRepository
import com.getaltair.kairos.domain.repository.HabitRepository
import com.getaltair.kairos.domain.validator.CompletionValidator
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CancellationException

/**
 * Records a skip for a habit on today's date with an optional reason.
 *
 * Enforces all completion rules (C-1 through C-5) and validates
 * the completion entity before persisting.
 */
class SkipHabitUseCase(
    private val habitRepository: HabitRepository,
    private val completionRepository: CompletionRepository
) {

    suspend operator fun invoke(habitId: UUID, skipReason: SkipReason? = null): Result<Completion> {
        return try {
            val habitResult = habitRepository.getById(habitId)
            if (habitResult is Result.Error) {
                return Result.Error("Habit not found: ${(habitResult as Result.Error).message}")
            }

            val today = LocalDate.now()

            // C-3: One completion per habit per day
            val existingResult = completionRepository.getForHabitOnDate(habitId, today)
            if (existingResult is Result.Error) return existingResult

            val existing = (existingResult as Result.Success).value
            if (existing != null) {
                return Result.Error("A completion already exists for this habit today")
            }

            val completion = Completion(
                habitId = habitId,
                date = today,
                type = CompletionType.Skipped,
                skipReason = skipReason
            )

            val validation = CompletionValidator.validate(completion, today)
            if (validation is Result.Error) return validation

            completionRepository.insert(completion)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error("Failed to skip habit: ${e.message}", cause = e)
        }
    }
}
