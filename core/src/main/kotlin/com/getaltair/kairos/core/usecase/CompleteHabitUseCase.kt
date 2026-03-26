package com.getaltair.kairos.core.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Completion
import com.getaltair.kairos.domain.enums.CompletionType
import com.getaltair.kairos.domain.repository.CompletionRepository
import java.time.LocalDate
import java.util.UUID

/**
 * Marks a habit as completed (FULL or PARTIAL) for today.
 * Validates invariant C-2 (partial percentage range) and C-4 (no future dates).
 */
class CompleteHabitUseCase(private val completionRepository: CompletionRepository) {
    suspend operator fun invoke(
        habitId: UUID,
        completionType: CompletionType,
        partialPercent: Int? = null
    ): Result<Completion> {
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
            Result.Error("Failed to complete habit", e)
        }
    }
}
