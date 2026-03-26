package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.enums.HabitStatus
import com.getaltair.kairos.domain.repository.HabitRepository
import kotlinx.coroutines.CancellationException

/**
 * Resumes a paused habit.
 *
 * State machine: PAUSED -> ACTIVE. Clears [Habit.pausedAt].
 * Rejects transitions from any status other than Paused.
 */
class ResumeHabitUseCase(private val habitRepository: HabitRepository) {

    suspend operator fun invoke(habitId: java.util.UUID): Result<Habit> {
        return try {
            val result = habitRepository.getById(habitId)
            if (result is Result.Error) {
                return Result.Error("Habit not found: ${result.message}")
            }
            val habit = (result as Result.Success).value

            if (habit.status !is HabitStatus.Paused) {
                return Result.Error(
                    "Cannot resume habit: current status is ${habit.status.displayName}, expected Paused"
                )
            }

            val updated = habit.copy(
                status = HabitStatus.Active,
                pausedAt = null
            )
            habitRepository.update(updated)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error("Failed to resume habit: ${e.message}", cause = e)
        }
    }
}
