package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.RoutineExecution
import com.getaltair.kairos.domain.enums.ExecutionStatus
import com.getaltair.kairos.domain.repository.RoutineExecutionRepository
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CancellationException

/**
 * Completes a routine execution.
 *
 * E-3: Marks the execution as Completed. Individual habit completions are
 * expected to have been created by [AdvanceRoutineStepUseCase] for each step
 * as the user progressed through the routine. This use case finalizes the
 * execution record.
 *
 * Returns [Result.Error] if the execution is not found, not in an
 * active state, or the update fails.
 */
class CompleteRoutineUseCase(private val routineExecutionRepository: RoutineExecutionRepository,) {

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
                    "Cannot complete execution: status is ${execution.status.displayName}, " +
                        "expected InProgress or Paused"
                )
            }

            val updatedExecution = execution.copy(
                status = ExecutionStatus.Completed,
                completedAt = Instant.now(),
            )

            routineExecutionRepository.update(updatedExecution)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error("Failed to complete routine: ${e.message}", cause = e)
        }
    }
}
