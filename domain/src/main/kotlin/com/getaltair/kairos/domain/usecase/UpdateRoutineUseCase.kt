package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Routine
import com.getaltair.kairos.domain.repository.RoutineRepository
import kotlinx.coroutines.CancellationException

/**
 * Updates an existing routine.
 *
 * Delegates directly to [RoutineRepository.update].
 * Returns [Result.Error] if the repository update fails.
 */
class UpdateRoutineUseCase(private val routineRepository: RoutineRepository) {

    suspend operator fun invoke(routine: Routine): Result<Routine> = try {
        routineRepository.update(routine)
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Result.Error("Failed to update routine: ${e.message}", cause = e)
    }
}
