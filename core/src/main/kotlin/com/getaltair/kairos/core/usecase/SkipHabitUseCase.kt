package com.getaltair.kairos.core.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Completion
import com.getaltair.kairos.domain.enums.CompletionType
import com.getaltair.kairos.domain.enums.SkipReason
import com.getaltair.kairos.domain.repository.CompletionRepository
import java.time.LocalDate
import java.util.UUID

/**
 * Marks a habit as skipped for today with an optional reason.
 * Skipping is always allowed per invariant D-4 (flexibility over rigidity).
 */
class SkipHabitUseCase(private val completionRepository: CompletionRepository) {
    suspend operator fun invoke(habitId: UUID, skipReason: SkipReason? = null): Result<Completion> = try {
        val completion = Completion(
            id = UUID.randomUUID(),
            habitId = habitId,
            date = LocalDate.now(),
            type = CompletionType.Skipped,
            skipReason = skipReason
        )
        completionRepository.insert(completion)
    } catch (e: Exception) {
        Result.Error("Failed to skip habit", e)
    }
}
