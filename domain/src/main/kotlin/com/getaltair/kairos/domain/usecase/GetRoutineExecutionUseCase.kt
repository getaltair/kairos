package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.RoutineExecution
import com.getaltair.kairos.domain.repository.RoutineExecutionRepository
import java.util.UUID
import kotlinx.coroutines.CancellationException

/**
 * Retrieves a routine execution by its ID.
 * Used by the summary screen to display completion results.
 */
class GetRoutineExecutionUseCase(private val routineExecutionRepository: RoutineExecutionRepository,) {

    suspend operator fun invoke(executionId: UUID): Result<RoutineExecution> {
        return try {
            val result = routineExecutionRepository.getById(executionId)
            when (result) {
                is Result.Error -> result

                is Result.Success -> {
                    val execution = result.value
                        ?: return Result.Error("Execution not found")
                    Result.Success(execution)
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error("Failed to load execution: ${e.message}", cause = e)
        }
    }
}
