package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.enums.HabitStatus
import com.getaltair.kairos.domain.repository.HabitRepository
import java.time.Instant
import kotlinx.coroutines.CancellationException

/**
 * Pauses an active habit.
 *
 * State machine: ACTIVE -> PAUSED. Sets [Habit.pausedAt] to now.
 * Rejects transitions from any status other than Active.
 */
class PauseHabitUseCase(private val habitRepository: HabitRepository) {

    suspend operator fun invoke(habitId: java.util.UUID): Result<Habit> {
        return try {
            val result = habitRepository.getById(habitId)
            if (result is Result.Error) {
                return Result.Error("Habit not found: ${result.message}")
            }
            val habit = (result as Result.Success).value

            if (habit.status !is HabitStatus.Active) {
                return Result.Error(
                    "Cannot pause habit: current status is ${habit.status.displayName}, expected Active"
                )
            }

            val updated = habit.copy(
                status = HabitStatus.Paused,
                pausedAt = Instant.now()
            )
            habitRepository.update(updated)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error("Failed to pause habit: ${e.message}", cause = e)
        }
    }
}
