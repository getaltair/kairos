package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.entity.RecoverySession
import com.getaltair.kairos.domain.repository.HabitRepository
import com.getaltair.kairos.domain.repository.RecoveryRepository
import kotlinx.coroutines.CancellationException

/**
 * Fetches all pending recovery sessions paired with their associated habits.
 *
 * Used by the UI to display a list of recoveries that need user action.
 * Sessions whose associated habit cannot be found are silently excluded.
 */
class GetPendingRecoveriesUseCase(
    private val recoveryRepository: RecoveryRepository,
    private val habitRepository: HabitRepository
) {

    suspend operator fun invoke(): Result<List<Pair<RecoverySession, Habit>>> {
        return try {
            val sessionsResult = recoveryRepository.getAllPending()
            if (sessionsResult is Result.Error) return Result.Error(sessionsResult.message)
            val sessions = (sessionsResult as Result.Success).value

            val pairs = mutableListOf<Pair<RecoverySession, Habit>>()

            for (session in sessions) {
                val habitResult = habitRepository.getById(session.habitId)
                if (habitResult is Result.Success) {
                    pairs.add(session to habitResult.value)
                }
            }

            Result.Success(pairs)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error("Failed to get pending recoveries: ${e.message}", cause = e)
        }
    }
}
