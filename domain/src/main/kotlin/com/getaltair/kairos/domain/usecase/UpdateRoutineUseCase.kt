package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Routine
import com.getaltair.kairos.domain.repository.RoutineRepository
import kotlinx.coroutines.CancellationException

/**
 * Updates an existing routine after validating its name.
 *
 * Validates that the routine name is not blank and is 50 characters or fewer,
 * then delegates to [RoutineRepository.update].
 * Returns [Result.Error] if validation or the repository update fails.
 */
class UpdateRoutineUseCase(private val routineRepository: RoutineRepository) {

    private companion object {
        const val MAX_NAME_LENGTH = 50
    }

    suspend operator fun invoke(routine: Routine): Result<Routine> = try {
        if (routine.name.isBlank()) {
            return Result.Error("Routine name must not be blank")
        }
        if (routine.name.length > MAX_NAME_LENGTH) {
            return Result.Error("Routine name must be $MAX_NAME_LENGTH characters or fewer")
        }
        routineRepository.update(routine)
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Result.Error("Failed to update routine: ${e.message}", cause = e)
    }
}
