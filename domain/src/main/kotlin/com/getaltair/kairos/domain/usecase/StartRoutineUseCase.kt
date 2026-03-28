package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.RoutineExecution
import com.getaltair.kairos.domain.enums.ExecutionStatus
import com.getaltair.kairos.domain.repository.RoutineExecutionRepository
import com.getaltair.kairos.domain.repository.RoutineRepository
import java.util.UUID
import kotlinx.coroutines.CancellationException

/**
 * Starts a new routine execution.
 *
 * E-1: Enforces active execution uniqueness. Checks that no execution with
 * status InProgress or Paused currently exists for the given routine. If one
 * exists, returns [Result.Error] with a descriptive message.
 *
 * Creates a new [RoutineExecution] with status InProgress and currentStepIndex = 0.
 */
class StartRoutineUseCase(
    private val routineRepository: RoutineRepository,
    private val routineExecutionRepository: RoutineExecutionRepository,
) {

    suspend operator fun invoke(routineId: UUID): Result<RoutineExecution> {
        return try {
            // Verify routine exists
            val routineResult = routineRepository.getById(routineId)
            if (routineResult is Result.Error) {
                return Result.Error("Routine not found: ${routineResult.message}")
            }
            val routine = (routineResult as Result.Success).value
                ?: return Result.Error("Routine not found")

            // E-1: Check no active execution exists for this routine
            val activeResult = routineExecutionRepository.getActiveForRoutine(routineId)
            if (activeResult is Result.Error) {
                return Result.Error("Failed to check active executions: ${activeResult.message}")
            }
            val activeExecution = (activeResult as Result.Success).value
            if (activeExecution != null) {
                return Result.Error(
                    "Routine '${routine.name}' already has an active execution " +
                        "(status: ${activeExecution.status.displayName})"
                )
            }

            val execution = RoutineExecution(
                routineId = routineId,
                status = ExecutionStatus.InProgress,
                currentStepIndex = 0,
            )

            routineExecutionRepository.insert(execution)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error("Failed to start routine: ${e.message}", cause = e)
        }
    }
}
