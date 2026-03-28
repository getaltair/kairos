package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.repository.RoutineRepository
import java.util.UUID
import kotlinx.coroutines.CancellationException

/**
 * Deletes a routine by ID.
 *
 * Delegates directly to [RoutineRepository.delete].
 * Cascade deletion of associated RoutineHabits is handled in the data layer.
 * Returns [Result.Error] if the repository delete fails.
 */
class DeleteRoutineUseCase(private val routineRepository: RoutineRepository) {

    suspend operator fun invoke(routineId: UUID): Result<Unit> = try {
        routineRepository.delete(routineId)
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Result.Error("Failed to delete routine: ${e.message}", cause = e)
    }
}
