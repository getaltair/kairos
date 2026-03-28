package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.RoutineExecution
import com.getaltair.kairos.domain.enums.ExecutionStatus
import com.getaltair.kairos.domain.repository.RoutineExecutionRepository
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CancellationException

/**
 * Abandons a routine execution mid-progress.
 *
 * Marks the execution as [ExecutionStatus.Abandoned]. Individual habit completions
 * created during the run are preserved (partial progress is not lost).
 *
 * Returns [Result.Error] if the execution is not found, not in an active state,
 * or the update fails.
 */
class AbandonRoutineUseCase(private val routineExecutionRepository: RoutineExecutionRepository,) {

    suspend operator fun invoke(executionId: UUID): Result<RoutineExecution> {
        return try {
            val executionResult = routineExecutionRepository.getById(executionId)
            if (executionResult is Result.Error) {
                return Result.Error("Execution not found: ${executionResult.message}")
            }
            val execution = (executionResult as Result.Success).value
                ?: return Result.Error("Execution not found")

            if (execution.status !is ExecutionStatus.InProgress &&
                execution.status !is ExecutionStatus.Paused
            ) {
                return Result.Error(
                    "Cannot abandon execution: status is ${execution.status.displayName}, " +
                        "expected InProgress or Paused"
                )
            }

            val updatedExecution = execution.copy(
                status = ExecutionStatus.Abandoned,
                completedAt = Instant.now(),
            )

            routineExecutionRepository.update(updatedExecution)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error("Failed to abandon routine: ${e.message}", cause = e)
        }
    }
}
