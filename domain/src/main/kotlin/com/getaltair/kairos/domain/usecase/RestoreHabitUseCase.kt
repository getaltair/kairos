package com.getaltair.kairos.domain.usecase

import com.getaltair.kairos.domain.common.Result
import com.getaltair.kairos.domain.entity.Habit
import com.getaltair.kairos.domain.enums.HabitStatus
import com.getaltair.kairos.domain.repository.HabitRepository
import java.util.UUID
import kotlinx.coroutines.CancellationException

/**
 * Restores an archived habit back to active.
 *
 * State machine: ARCHIVED -> ACTIVE. Clears [Habit.archivedAt].
 * Rejects transitions from any status other than Archived.
 */
class RestoreHabitUseCase(private val habitRepository: HabitRepository) {

    suspend operator fun invoke(habitId: UUID): Result<Habit> {
        return try {
            val result = habitRepository.getById(habitId)
            if (result is Result.Error) {
                return Result.Error("Habit not found: ${result.message}")
            }
            val habit = (result as Result.Success).value

            if (habit.status !is HabitStatus.Archived) {
                return Result.Error(
                    "Cannot restore habit: current status is ${habit.status.displayName}, expected Archived"
                )
            }

            val updated = habit.copy(
                status = HabitStatus.Active,
                archivedAt = null
            )
            habitRepository.update(updated)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error("Failed to restore habit: ${e.message}", cause = e)
        }
    }
}
