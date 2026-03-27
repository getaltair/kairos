package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.entity.RecoverySession
import com.getaltair.kairos.domain.repository.HabitRepository
import com.getaltair.kairos.domain.repository.RecoveryRepository
import java.util.logging.Level
import java.util.logging.Logger
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

    private val logger = Logger.getLogger(GetPendingRecoveriesUseCase::class.java.name)

    suspend operator fun invoke(): Result<List<Pair<RecoverySession, Habit>>> {
        return try {
            val sessionsResult = recoveryRepository.getAllPending()
            if (sessionsResult is Result.Error) return Result.Error(sessionsResult.message)
            val sessions = (sessionsResult as Result.Success).value

            val pairs = mutableListOf<Pair<RecoverySession, Habit>>()

            for (session in sessions) {
                when (val habitResult = habitRepository.getById(session.habitId)) {
                    is Result.Success -> {
                        val habit = habitResult.value
                        if (habit != null) {
                            pairs.add(session to habit)
                        }
                    }

                    is Result.Error -> {
                        logger.log(
                            Level.WARNING,
                            "Failed to load habit ${session.habitId} for recovery session ${session.id}: ${habitResult.message}"
                        )
                    }
                }
            }

            Result.Success(pairs)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error("Failed to get pending recoveries: ${e.message}", cause = e)
        }
    }
}
