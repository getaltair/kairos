package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Completion
import com.getaltair.kairos.domain.entity.RoutineExecution
import com.getaltair.kairos.domain.enums.CompletionType
import com.getaltair.kairos.domain.enums.ExecutionStatus
import com.getaltair.kairos.domain.enums.StepResult
import com.getaltair.kairos.domain.repository.CompletionRepository
import com.getaltair.kairos.domain.repository.RoutineExecutionRepository
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CancellationException

/**
 * Advances a routine execution to the next step.
 *
 * Marks the current step as done or skipped based on [StepResult], creates an
 * individual [Completion] for the habit via [CompletionRepository], and advances
 * the currentStepIndex.
 *
 * E-2: Step index must remain within routine bounds (enforced by caller
 * providing valid habitId for the current step).
 */
class AdvanceRoutineStepUseCase(
    private val routineExecutionRepository: RoutineExecutionRepository,
    private val completionRepository: CompletionRepository,
) {

    suspend operator fun invoke(executionId: UUID, stepResult: StepResult, habitId: UUID,): Result<RoutineExecution> {
        return try {
            val executionResult = routineExecutionRepository.getById(executionId)
            if (executionResult is Result.Error) {
                return Result.Error("Execution not found: ${executionResult.message}")
            }
            val execution = (executionResult as Result.Success).value
                ?: return Result.Error("Execution not found")

            if (execution.status !is ExecutionStatus.InProgress) {
                return Result.Error(
                    "Cannot advance step: execution status is ${execution.status.displayName}, " +
                        "expected InProgress"
                )
            }

            // E-3: Create individual habit completion for the current step
            val completionType = when (stepResult) {
                is StepResult.Completed -> CompletionType.Full
                is StepResult.Skipped -> CompletionType.Skipped
            }

            val completion = Completion(
                habitId = habitId,
                date = LocalDate.now(),
                type = completionType,
            )
            val completionResult = completionRepository.insert(completion)
            if (completionResult is Result.Error) {
                return Result.Error("Failed to record habit completion: ${completionResult.message}")
            }

            // Advance to the next step
            val updatedExecution = execution.copy(
                currentStepIndex = execution.currentStepIndex + 1,
            )

            routineExecutionRepository.update(updatedExecution)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error("Failed to advance routine step: ${e.message}", cause = e)
        }
    }
}
