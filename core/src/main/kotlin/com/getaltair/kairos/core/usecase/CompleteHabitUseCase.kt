package com.getaltair.kairos.core.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Completion
import com.getaltair.kairos.domain.enums.CompletionType
import com.getaltair.kairos.domain.repository.CompletionRepository
import com.getaltair.kairos.domain.repository.HabitRepository
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CancellationException
import timber.log.Timber

/**
 * Marks a habit as completed (FULL or PARTIAL) for today.
 *
 * Enforces:
 * - C-1: Only system can create MISSED completions; SKIPPED must use [SkipHabitUseCase]
 * - C-2: Partial percentage must be in 1..99; non-Partial types must not set partialPercent
 * - C-3: One completion per habit per day (duplicate detection)
 * - C-4: No future dates (inherently satisfied by using today)
 */
class CompleteHabitUseCase(
    private val habitRepository: HabitRepository,
    private val completionRepository: CompletionRepository
) {
    suspend operator fun invoke(
        habitId: UUID,
        completionType: CompletionType,
        partialPercent: Int? = null
    ): Result<Completion> {
        // Validate habit exists
        val habitResult = habitRepository.getById(habitId)
        if (habitResult is Result.Error) {
            return Result.Error("Habit not found: ${habitResult.message}")
        }

        // C-1: Only system can create MISSED completions
        if (completionType is CompletionType.Missed) {
            return Result.Error("Cannot manually create MISSED completions")
        }

        // C-1: Skipped completions must use SkipHabitUseCase
        if (completionType is CompletionType.Skipped) {
            return Result.Error("Use SkipHabitUseCase for skipped completions")
        }

        // C-2: Validate partial percentage when type is Partial
        if (completionType is CompletionType.Partial) {
            if (partialPercent == null || partialPercent !in 1..99) {
                return Result.Error(
                    "Partial completion requires partialPercent in 1..99"
                )
            }
        }

        // C-2: partialPercent must be null for non-Partial types
        if (completionType !is CompletionType.Partial && partialPercent != null) {
            return Result.Error(
                "partialPercent must only be set for PARTIAL type"
            )
        }

        val today = LocalDate.now()

        // C-3: One completion per habit per day
        val existingResult = completionRepository.getForHabitOnDate(habitId, today)
        if (existingResult is Result.Error) return existingResult
        val existing = (existingResult as Result.Success).value
        if (existing != null) {
            return Result.Error("A completion already exists for this habit today")
        }

        // C-4: No future completions (date is always today for this use case)
        // This is inherently satisfied since we use LocalDate.now()

        return try {
            val completion = Completion(
                id = UUID.randomUUID(),
                habitId = habitId,
                date = today,
                type = completionType,
                partialPercent = partialPercent
            )
            completionRepository.insert(completion)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.e(e, "Failed to complete habit id=%s", habitId)
            Result.Error("Failed to complete habit", e)
        }
    }
}
