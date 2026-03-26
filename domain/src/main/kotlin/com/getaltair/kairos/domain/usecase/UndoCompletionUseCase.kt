package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.repository.CompletionRepository
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CancellationException

/**
 * Undoes a completion within a 30-second window.
 *
 * The undo window is calculated from the time the original completion was made
 * ([completedAt]) to the current time. If more than 30 seconds have elapsed,
 * the undo is rejected.
 *
 * @param completionId ID of the completion to undo
 * @param completedAt the original completion's createdAt timestamp; the caller
 *   must supply the exact value from the [Completion] being undone
 */
class UndoCompletionUseCase(private val completionRepository: CompletionRepository) {

    suspend operator fun invoke(completionId: UUID, completedAt: Instant): Result<Unit> {
        return try {
            val elapsed = Duration.between(completedAt, Instant.now()).seconds
            if (elapsed > UNDO_WINDOW_SECONDS) {
                return Result.Error(
                    "Undo window has expired (${elapsed}s elapsed, max ${UNDO_WINDOW_SECONDS}s)"
                )
            }

            completionRepository.delete(completionId)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error("Failed to undo completion: ${e.message}", cause = e)
        }
    }

    companion object {
        const val UNDO_WINDOW_SECONDS = 30L
    }
}
