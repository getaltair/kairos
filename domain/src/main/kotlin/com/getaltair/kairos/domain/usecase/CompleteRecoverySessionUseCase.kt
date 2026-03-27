package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.RecoverySession
import com.getaltair.kairos.domain.enums.HabitPhase
import com.getaltair.kairos.domain.enums.HabitStatus
import com.getaltair.kairos.domain.enums.RecoveryAction
import com.getaltair.kairos.domain.enums.SessionStatus
import com.getaltair.kairos.domain.repository.HabitRepository
import com.getaltair.kairos.domain.repository.RecoveryRepository
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CancellationException

/**
 * Completes a pending recovery session by applying the user's chosen action.
 *
 * REC-3: Only Pending sessions may be completed. Attempting to complete a
 * session that is already Completed or Abandoned results in an error.
 *
 * Each [RecoveryAction] produces a different effect on the associated habit:
 * - Resume: phase -> FORMING (resume tracking)
 * - Simplify: activate microVersion if present, phase -> FORMING
 * - Pause: status -> PAUSED
 * - Archive: status -> ARCHIVED
 * - FreshStart: phase -> FORMING (reset after relapse)
 */
class CompleteRecoverySessionUseCase(
    private val recoveryRepository: RecoveryRepository,
    private val habitRepository: HabitRepository
) {

    suspend operator fun invoke(sessionId: UUID, action: RecoveryAction): Result<RecoverySession> {
        return try {
            // Load session
            val sessionResult = recoveryRepository.getById(sessionId)
            if (sessionResult is Result.Error) {
                return Result.Error("Recovery session not found: ${sessionResult.message}")
            }
            val session = (sessionResult as Result.Success).value
                ?: return Result.Error("Recovery session not found")

            // REC-3: Only Pending sessions can be completed
            if (session.status !is SessionStatus.Pending) {
                return Result.Error(
                    "Cannot complete session: status is ${session.status.displayName}, expected Pending"
                )
            }

            // Load associated habit
            val habitResult = habitRepository.getById(session.habitId)
            if (habitResult is Result.Error) {
                return Result.Error("Associated habit not found: ${habitResult.message}")
            }
            val habit = (habitResult as Result.Success).value

            // Apply the action's effect on the habit
            val updatedHabit = when (action) {
                is RecoveryAction.Resume -> habit.copy(phase = HabitPhase.FORMING)

                is RecoveryAction.Simplify -> {
                    if (habit.microVersion != null) {
                        habit.copy(
                            name = habit.microVersion,
                            phase = HabitPhase.FORMING
                        )
                    } else {
                        habit.copy(phase = HabitPhase.FORMING)
                    }
                }

                is RecoveryAction.Pause -> habit.copy(
                    status = HabitStatus.Paused,
                    pausedAt = Instant.now()
                )

                is RecoveryAction.Archive -> habit.copy(
                    status = HabitStatus.Archived,
                    archivedAt = Instant.now()
                )

                is RecoveryAction.FreshStart -> habit.copy(phase = HabitPhase.FORMING)
            }

            habitRepository.update(updatedHabit)

            // Update session to Completed
            val updatedSession = session.copy(
                status = SessionStatus.Completed,
                action = action,
                completedAt = Instant.now()
            )
            val updateResult = recoveryRepository.update(updatedSession)
            if (updateResult is Result.Error) return updateResult

            Result.Success((updateResult as Result.Success).value)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error("Failed to complete recovery session: ${e.message}", cause = e)
        }
    }
}
