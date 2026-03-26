package com.getaltair.kairos.core.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.repository.CompletionRepository
import java.util.UUID
import kotlinx.coroutines.CancellationException
import timber.log.Timber

/**
 * Undoes a completion by deleting it.
 * The 30-second undo window is enforced at the ViewModel layer, not here.
 */
class UndoCompletionUseCase(private val completionRepository: CompletionRepository) {
    suspend operator fun invoke(completionId: UUID): Result<Unit> = try {
        completionRepository.delete(completionId)
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Timber.e(e, "Failed to undo completion id=%s", completionId)
        Result.Error("Failed to undo completion", e)
    }
}
