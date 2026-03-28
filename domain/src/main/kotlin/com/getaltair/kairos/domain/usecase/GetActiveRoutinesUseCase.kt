package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Routine
import com.getaltair.kairos.domain.repository.RoutineRepository
import kotlinx.coroutines.CancellationException

/**
 * Retrieves all active routines for display in the routine list.
 * Active routines are those with status = Active.
 */
class GetActiveRoutinesUseCase(private val routineRepository: RoutineRepository) {

    suspend operator fun invoke(): Result<List<Routine>> = try {
        routineRepository.getActiveRoutines()
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Result.Error("Failed to load routines: ${e.message}", cause = e)
    }
}
