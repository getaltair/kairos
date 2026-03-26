package com.getaltair.kairos.core.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Completion
import com.getaltair.kairos.domain.enums.CompletionType
import com.getaltair.kairos.domain.enums.SkipReason
import com.getaltair.kairos.domain.repository.CompletionRepository
import com.getaltair.kairos.domain.repository.HabitRepository
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CancellationException
import timber.log.Timber

/**
 * Marks a habit as skipped for today with an optional reason.
 *
 * Enforces:
 * - Habit must exist
 * - C-3: One completion per habit per day (duplicate detection)
 *
 * Skipping is always allowed per invariant D-4 (flexibility over rigidity).
 */
class SkipHabitUseCase(
    private val habitRepository: HabitRepository,
    private val completionRepository: CompletionRepository
) {
    suspend operator fun invoke(habitId: UUID, skipReason: SkipReason? = null): Result<Completion> {
        // Validate habit exists
        val habitResult = habitRepository.getById(habitId)
        if (habitResult is Result.Error) {
            return Result.Error("Habit not found: ${habitResult.message}")
        }

        val today = LocalDate.now()

        // C-3: One completion per habit per day
        val existingResult = completionRepository.getForHabitOnDate(habitId, today)
        if (existingResult is Result.Error) return existingResult
        val existing = (existingResult as Result.Success).value
        if (existing != null) {
            return Result.Error("A completion already exists for this habit today")
        }

        return try {
            val completion = Completion(
                id = UUID.randomUUID(),
                habitId = habitId,
                date = today,
                type = CompletionType.Skipped,
                skipReason = skipReason
            )
            completionRepository.insert(completion)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.e(e, "Failed to skip habit id=%s", habitId)
            Result.Error("Failed to skip habit", e)
        }
    }
}
